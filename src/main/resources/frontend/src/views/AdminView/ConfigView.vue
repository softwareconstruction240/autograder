<script setup lang="ts">
import { defineAsyncComponent, onMounted, ref } from 'vue'
import { listOfPhases } from '@/types/types'
import { useAppConfigStore } from '@/stores/appConfig'
import { generateClickableLink, readableTimestamp } from '@/utils/utils'
import ConfigSection from '@/components/config/ConfigSection.vue'

// Lazy Load Editor Components
const BannerConfigEditor = defineAsyncComponent(() => import('@/components/config/BannerConfigEditor.vue'))
const LivePhaseConfigEditor = defineAsyncComponent(() => import('@/components/config/LivePhaseConfigEditor.vue'))
const CourseIdConfigEditor = defineAsyncComponent(() => import('@/components/config/CourseIdConfigEditor.vue'))

const appConfigStore = useAppConfigStore();

onMounted( async () => {
  await useAppConfigStore().updateConfig();
})

</script>

<template>
  <div id="configContainer">
    <ConfigSection title="Banner Message" description="A dynamic message displayed across the top of the Autograder">
      <template #editor="{ closeEditor }">
        <BannerConfigEditor :closeEditor="closeEditor"/>
      </template>
      <template #current>
      <div v-if="appConfigStore.bannerMessage">
        <p><span class="infoLabel">Current Message: </span><span v-text="appConfigStore.bannerMessage"/></p>
        <p><span class="infoLabel">Current Link: </span><span v-html="generateClickableLink(appConfigStore.bannerLink)"/></p>
        <p><span class="infoLabel">Expires: </span><span v-text="readableTimestamp(appConfigStore.bannerExpiration)"/></p>
      </div>
        <p v-else>There is currently no banner message</p>
      </template>
    </ConfigSection>

    <ConfigSection title="Live Phases" description="These phases are live and open for students to submit to">
      <template #editor="{ closeEditor }">
        <LivePhaseConfigEditor :closeEditor="closeEditor"/>
      </template>
      <template #current>
        <div v-for="phase in listOfPhases()">
          <p>
            <i v-if="appConfigStore.phaseActivationList[phase]" class="fa-solid fa-circle-check" style="color: green"/>
            <i v-else class="fa-solid fa-x" style="color: red"/>
            {{phase}}</p>
        </div>
      </template>
    </ConfigSection>

    <ConfigSection title="Course IDs" description="Phase assignment ID numbers, rubric IDs, and rubric points">
      <template #editor="{ closeEditor }">
        <CourseIdConfigEditor :closeEditor="closeEditor"/>
      </template>
      <template #current>
        <p><span class="infoLabel">Course ID:</span> {{appConfigStore.courseNumber}}</p>
        <p><em>Open editor to see the rest of the values</em></p>
      </template>
    </ConfigSection>
  </div>
</template>

<style scoped>
:deep(input) {
  border: 1px solid #ccc;
  padding: 8px;
  border-radius: 4px;
}
:deep(input[type="text"]) {
  width: 100%;
}

.infoLabel {
  font-weight: bold;
}

#configContainer {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr; /* Three columns of 100px each */
  grid-gap: 10px; /* Gap between grid items */
  margin: 10px;
}

input[type="text"]{
  padding: 5px;
  width: 100%;
}
</style>

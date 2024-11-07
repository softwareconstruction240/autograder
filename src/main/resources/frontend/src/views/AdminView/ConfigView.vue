<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PopUp from '@/components/PopUp.vue'
import { listOfPhases } from '@/types/types'
import { useAppConfigStore } from '@/stores/appConfig'
import { generateClickableLink, readableTimestamp } from '@/utils/utils'
import ConfigSection from '@/components/config/ConfigSection.vue'
import BannerConfigEditor from '@/components/config/BannerConfigEditor.vue'
import LivePhaseConfigEditor from '@/components/config/LivePhaseConfigEditor.vue'
import CourseIdConfigEditor from '@/components/config/CourseIdConfigEditor.vue'

const appConfigStore = useAppConfigStore();

// PopUp Control
const openLivePhases = ref<boolean>(false);
const openCanvasCourseIds = ref<boolean>(false);
const openManuelCourseIds = ref<boolean>(false);

// =========================
onMounted( async () => {
  await useAppConfigStore().updateConfig();
})

</script>

<template>
  <div id="configContainer">
    <ConfigSection title="Banner Message" description="A dynamic message displayed across the top of the Autograder">
      <template v-slot:editor>
        <BannerConfigEditor/>
      </template>
      <template v-slot:current>
      <span v-if="appConfigStore.bannerMessage">
        <p><span class="infoDescription">Current Message: </span><span v-text="appConfigStore.bannerMessage"/></p>
        <p><span class="infoDescription">Current Link: </span><span v-html="generateClickableLink(appConfigStore.bannerLink)"/></p>
        <p><span class="infoDescription">Expires: </span><span v-text="readableTimestamp(appConfigStore.bannerExpiration)"/></p>
      </span>
        <p v-else>There is currently no banner message</p>
      </template>
    </ConfigSection>

    <ConfigSection title="Live Phases" description="These are the phases are live and open for students to submit to">
      <template v-slot:editor>
        <LivePhaseConfigEditor/>
      </template>
      <template v-slot:current>
        <div v-for="phase in listOfPhases()">
          <p>
            <i v-if="appConfigStore.phaseActivationList[phase]" class="fa-solid fa-circle-check" style="color: green"/>
            <i v-else class="fa-solid fa-x" style="color: red"/>
            {{phase}}</p>
        </div>
      </template>
    </ConfigSection>

    <ConfigSection title="Course IDs" description="Phase assignment ID numbers, rubric IDs, rubric points, ">
      <template v-slot:editor>
        <CourseIdConfigEditor/>
      </template>
      <template v-slot:current>
        <p><b>Course ID:</b> {{appConfigStore.courseNumber}}</p>
      </template>
    </ConfigSection>
  </div>

  <PopUp
    v-if="openLivePhases"
    @closePopUp="openLivePhases = false; appConfigStore.updateConfig()">

  </PopUp>

  <PopUp
    v-if="openCanvasCourseIds"
    @closePopUp="openCanvasCourseIds = false">

    <h3 style="text-align: center">Course Related IDs</h3>
    <p style="text-align: center">
      Are you sure you want to use Canvas for retrieving assignment IDs, rubric IDs, and rubric points?<br />
      This will overwrite the preexisting values and cannot be restored.
    </p>

    <span class="center-buttons">
      <button @click="submitCanvasCourseIds">Yes</button>
      <button @click="openCanvasCourseIds = false">Go Back</button>
    </span>
  </PopUp>

  <PopUp
    v-if="openManuelCourseIds"
    @closePopUp="openManuelCourseIds = false">
    <h3>Course Related IDs</h3>
  </PopUp>

</template>

<style scoped>
.submitChanges {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.submitChanges >* {
  margin: 5px;
}
.infoDescription {
  font-weight: bold;
}

#configContainer {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr; /* Three columns of 100px each */
  grid-gap: 10px; /* Gap between grid items */
  margin: 10px;
}

button {
  margin-right: 5px;
  margin-top: 5px;
}

input[type="text"]{
  padding: 5px;
  width: 100%;
}

.center-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
}



.inline-container {
  display: flex;
  align-items: center;
  margin-right: 10px; /* Optional: Adjust spacing between elements */
  margin-left: 10px;
}

.inline-container label {
  margin-right: 5px; /* Optional: Adjust spacing between label and input */
}


</style>

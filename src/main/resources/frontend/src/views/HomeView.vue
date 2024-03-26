<script setup lang="ts">

import { onMounted, ref } from 'vue'
import {useSubmissionStore} from "@/stores/submissions";
import OptionSelector from '@/components/OptionSelector.vue'
import type { Phase } from '@/types/types'
import { uiConfig } from '@/stores/uiConfig'

// periodically check if grading is happening
onMounted(async () => {
  setInterval(async () => {
    if (!useSubmissionStore().currentlyGrading)
      await useSubmissionStore().checkGrading();
  }, 5000);
})

const selectedPhase = ref<Phase | null>(null);

const selectPhase = (phase: Phase) => {
  selectedPhase.value = phase
}

</script>

<template>
  <OptionSelector
    :options="['0','1','3','4','5','6']"
  @optionSelected="value => selectPhase(value)"/>

  <h3 v-if="!selectedPhase">Please select a phase</h3>
  <h3 v-else v-html="uiConfig.getPhaseName(selectedPhase)"/>
  <a
    target="_blank"
    :href="uiConfig.getSpecLink(selectedPhase)">
    <span v-if="selectedPhase">Review phase specs on Github</span>
    <span v-else>Review project specs on Github</span>
  </a>
</template>

<style scoped>
h3 {
  font-size: xx-large;
  margin-top: 20px;
}

</style>
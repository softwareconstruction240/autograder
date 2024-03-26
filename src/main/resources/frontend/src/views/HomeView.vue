<script setup lang="ts">

import { onMounted, ref } from 'vue'
import {useSubmissionStore} from "@/stores/submissions";
import OptionSelector from '@/components/OptionSelector.vue'
import type { Phase, Submission } from '@/types/types'
import { uiConfig } from '@/stores/uiConfig'
import { submissionPost } from '@/services/submissionService'
import LiveStatus from '@/views/PhaseView/LiveStatus.vue'
import SubmissionHistory from '@/views/PhaseView/SubmissionHistory.vue'
import InfoPanel from '@/components/InfoPanel.vue'
import ResultsPreview from '@/views/PhaseView/ResultsPreview.vue'

// periodically check if grading is happening
onMounted(async () => {
  setInterval(async () => {
    if (!useSubmissionStore().currentlyGrading)
      await useSubmissionStore().checkGrading();
  }, 5000);
})

const selectedPhase = ref<Phase | null>(null);
const openGrader = ref<boolean>(false);
const showResults = ref<boolean>(false);
const lastSubmission = ref<Submission | null>(null);

const selectPhase = async (phase: Phase) => {
  selectedPhase.value = phase
  await useSubmissionStore().checkGrading()
}
const submitPhase = async () => {
  if (selectedPhase.value === null) { // make typescript happy
    console.error("submitPhase() was called without a phase selected")
    return
  }
  try {
    useSubmissionStore().currentlyGrading = true;
    await submissionPost(selectedPhase.value);
    openGrader.value = true;
    showResults.value = false;
  } catch (e) {
    alert(e)
  }
}

const handleGradingDone = async () => {
  useSubmissionStore().currentlyGrading = false;
  lastSubmission.value = await useSubmissionStore().getLastSubmission();
  showResults.value = true;
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

  <button @click="submitPhase" class="submit primary" :disabled="!selectedPhase || useSubmissionStore().currentlyGrading">Submit to grader</button>

  <InfoPanel style="height: 300px;" v-if="openGrader">
    <LiveStatus v-if="useSubmissionStore().currentlyGrading" @show-results="handleGradingDone"/>
    <ResultsPreview v-if="showResults" :submission="lastSubmission"/>
  </InfoPanel>

  <div id="submission-history" style="width: 100%">
    <h3>Submission History</h3>
    <p>Click on a submission to see details</p>
    <SubmissionHistory/>
  </div>

</template>

<style scoped>
h3 {
  font-size: xx-large;
  margin-top: 20px;
}

.submit {
  padding: 20px 40px;
  font-size: 30px;
  border-radius: 20px;
  max-width: 80vw;
  margin: 20px;
}

</style>
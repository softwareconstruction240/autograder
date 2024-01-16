<script setup lang="ts">
import type {Phase, Submission} from "@/types/types";
import PastSubmissions from "@/views/PhaseView/PastSubmissions.vue";
import {ref} from "vue";
import {submissionPost} from "@/services/submissionService";
import ResultsSection from "@/views/PhaseView/ResultsSection.vue";
import LiveStatus from "@/views/PhaseView/LiveStatus.vue";
import {useSubmissionStore} from "@/stores/submissions";

const props = defineProps<{
  phaseTitle: string;
  phaseDescription: string;
  phase: Phase
}>();

const selectedResults = ref<Submission | null>(null);

const showResults = (submission: Submission) => {
  if (selectedResults.value === submission) {
    selectedResults.value = null;
    return;
  }
  selectedResults.value = submission;
  useSubmissionStore().currentlyGrading = false;
}

const submitPhase = async () => {
  try {
    await submissionPost(props.phase);
    useSubmissionStore().currentlyGrading = true;
  } catch (e) {
    alert(e)
  }
}

const handleGradingDone = async () => {
  await useSubmissionStore().getSubmissions(props.phase);
  selectedResults.value = useSubmissionStore().submissionsByPhase[props.phase][0];
  useSubmissionStore().currentlyGrading = false;
}

</script>

<template>
  <div class="container">
    <div id="phase-description">
      <h1>{{ phaseTitle }}</h1>
      <p>{{ phaseDescription }}</p>
    </div>

    <div id="submission-options">
      <div id="choices">
        <div>
          <h3>Submit Phase</h3>
          <p>Click the button below to submit your phase</p>
          <button :disabled="useSubmissionStore().currentlyGrading" @click="submitPhase">Submit Phase</button>
        </div>
        <div>
          <h3>Past Submissions</h3>
          <p>Click on a submission to view its results</p>
          <PastSubmissions
              :phase="phase"
              @show-results="showResults"
          />
        </div>
      </div>
      <div id="results">
        <!-- FIXME: for the life of me I can't force this thing to only take up 50% of the space. this hack should be temporary -->
        <div style="width: 40vw; height: 100%;">
          <ResultsSection v-if="selectedResults && !useSubmissionStore().currentlyGrading"
                          :submission="selectedResults"/>
          <LiveStatus v-else-if="useSubmissionStore().currentlyGrading" @show-results="handleGradingDone"/>
        </div>
      </div>
    </div>


  </div>

</template>

<style scoped>

#phase-description {
  margin-top: 10px;

  text-align: center;
  width: 100%;
}

#submission-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr;
  gap: 0 0;
  grid-auto-flow: row;
  grid-template-areas:
    "choices results";
}

#choices {
  grid-area: choices;

  display: flex;
  flex-direction: column;
  gap: 40px;
}

#results {
  grid-area: results;

  border-left: 1px solid #c6c6c6;

  padding-left: 20px;

}

</style>
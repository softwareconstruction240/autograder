<script setup lang="ts">
import type {Phase, Submission} from "@/types/types";
import PastSubmissions from "@/views/PhaseView/PastSubmissions.vue";
import {ref} from "vue";
import {submissionPost} from "@/services/submissionService";
import {useAuthStore} from "@/stores/auth";
import ResultsSection from "@/views/PhaseView/ResultsSection.vue";
import LiveStatus from "@/views/PhaseView/LiveStatus.vue";
import {useSubmissionStore} from "@/stores/submissions";

const props = defineProps<{
  phaseTitle: string;
  phaseDescription: string;
  phase: Phase
}>();

const selectedResults = ref<Submission | null>(null);

const currentlyGrading = ref<boolean>(false);

const showResults = (submission: Submission) => {
  if (selectedResults.value === submission) {
    selectedResults.value = null;
    return;
  }
  selectedResults.value = submission;
  currentlyGrading.value = false;
}

const submitPhase = async () => {
  await submissionPost(props.phase, useAuthStore().user!.repoUrl);
  currentlyGrading.value = true;
}

const handleGradingDone = async () => {
  await useSubmissionStore().getSubmissions(props.phase);
  selectedResults.value = useSubmissionStore().submissionsByPhase[props.phase][0];
  currentlyGrading.value = false;
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
          <button @click="submitPhase">Submit Phase</button>
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
        <div style="width: 40vw;">
          <ResultsSection v-if="selectedResults && !currentlyGrading" :submission="selectedResults"/>
          <LiveStatus v-else-if="currentlyGrading" @show-results="handleGradingDone" />
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

#submission-options {  display: grid;
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
  justify-content: space-evenly;
  gap: 40px;
}

#results { grid-area: results; }

</style>
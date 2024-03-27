<script setup lang="ts">
import type {Submission, TestResult} from "@/types/types";
import {ref} from "vue";
import type { CellClickedEvent } from 'ag-grid-community'
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-quartz.css";
import ResultsPopup from "@/views/StudentView/ResultsPopup.vue";
import {
  generateClickableCommitLink,
  generateClickableLink,
  nameOnSubmission,
  readableTimestamp,
  scoreToPercentage
} from '@/utils/utils'
import RubricItemView from '@/views/StudentView/RubricItemView.vue'

const { submission } = defineProps<{
  submission: Submission;
}>();

const testResults = ref<TestResult | undefined>(undefined);
const textResults = ref<string | undefined>(undefined);

const openResults = (event: CellClickedEvent) => {
  if (event.data.results.testResults) {
    testResults.value = event.data.results.testResults
  } else {
    textResults.value = event.data.results.textResults
  }
}

</script>

<template>
  <div class="container">
    <h2>Phase {{submission.phase.charAt(5)}}</h2>
    <h3>{{nameOnSubmission(submission)}} ({{submission.netId}})</h3>
    <p>{{readableTimestamp(submission.timestamp)}}</p>
    <p v-html="generateClickableLink(submission.repoUrl)"/>
    <p>commit: <span v-html="generateClickableCommitLink(submission.repoUrl, submission.headHash)"/></p>
    <p>status:
      <span v-if="false"><b>needs approval, go see a TA</b> <i class="fa-solid fa-triangle-exclamation" style="color: var(--failure-color)"/></span>
      <span v-else-if="submission.passed">passed <i class="fa-solid fa-circle-check" style="color: var(--success-color)"/></span>
      <span v-else>failed <i class="fa-solid fa-circle-xmark" style="color: var(--failure-color)"/></span>
    </p>

    <div id="important">
      <div class="info-box">
        <p>Score:</p>
        <h1 v-html="scoreToPercentage(submission.score)"/>
      </div>
      <div class="info-box">
        <p>Notes:</p>
        <p v-html="submission.notes.replace('\n', '<br />')"/>
      </div>
    </div>

    <RubricItemView
      v-if="submission.rubric.passoffTests"
      :rubric-item="submission.rubric.passoffTests"
    />
    <RubricItemView
      v-if="submission.rubric.quality"
      :rubric-item="submission.rubric.quality"
    />
    <RubricItemView
      v-if="submission.rubric.unitTests"
      :rubric-item="submission.rubric.unitTests"
    />
  </div>

  <ResultsPopup
      v-if="testResults || textResults"
      :text-results="textResults"
      :test-results="testResults"
      @closePopUp="() => {
        testResults = undefined;
        textResults = undefined;
      }"
  />

</template>

<style scoped>
.container {
  flex-direction: column;
  align-items: flex-start;
  text-align: left;
  max-width: 550px;
}

.container p {
  padding: 1px 0;
}

#important {
  display: flex;
  flex-direction: row;
}

.info-box {
  background-color: var(--opposite);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--opposite-text-color);
  margin: 10px;
  text-align: center;
  padding: 5px 15px;
  border-radius: 5px;
}
</style>
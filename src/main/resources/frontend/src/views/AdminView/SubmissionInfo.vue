<script setup lang="ts">
import type {Submission} from "@/types/types";
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-quartz.css";
import {
  standardColSettings,
  loadRubricRows,
  wrappingColSettings,
} from "@/utils/tableUtils";
import {
  generateClickableLink,
  nameFromNetId,
  readableTimestamp,
  scoreToPercentage
} from "@/utils/utils";
import { approveSubmissionPost } from '@/services/adminService'

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

const approveSubmission = async (penalize: boolean) => {
  try {
    await approveSubmissionPost(submission.netId, submission.phase, penalize);
  } catch (e) {
    alert(e)
  }
}

const columnDefs = reactive([
  { headerName: "Category", field: 'category', flex:1 },
  { headerName: "Criteria", field: "criteria", ...wrappingColSettings, flex:2 },
  { headerName: "Notes", field: "notes", ...wrappingColSettings, flex:2, sortable: false, onCellClicked: openResults },
  { headerName: "Points", field: "points", flex:1, onCellClicked: openResults }
])
const rowData = reactive({
  value: [] = loadRubricRows(submission)
})
</script>

<template>
  <h3>{{submission.phase}} - {{nameFromNetId(submission.netId)}}</h3>
  <div class="container">
    <div>
      <p><span class="info-label">Submitted:</span> {{readableTimestamp(submission.timestamp)}}</p>
      <p><span class="info-label">NetID: </span> {{submission.netId}}</p>
      <p><span class="info-label">Overall Score: </span>{{scoreToPercentage(submission.score)}}
        <span v-if="submission.passed">Submission passed!</span>
        <span v-else class="failure">Submission failed</span>
      </p>
    </div>
    <Panel>
      <p><span class="info-label">GitHub Repo: </span><span v-html="generateClickableLink(submission.repoUrl)"/></p>
      <p class="info-label">Submission Notes:</p>
      <p id="notes-field">{{submission.notes}}</p>
    </Panel>
  </div>

  <RubricTable :rubric="submission.rubric"/>
  <Panel v-if="submission.verifiedStatus == 'Unapproved'" class="blocked-submission-notice">
    <p>This submission has been blocked!</p>
    <span>Explain the importance of frequent, consistent commits. Only approve without the penalty if they have a good reason for failing to meet the standard.</span>
    <div>
      <button @click="approveSubmission(true)">Approve with penalty (DEFAULT)</button>
      <button @click="approveSubmission(false)">Approve with no penalty</button>
    </div>
  </Panel>
  <ag-grid-vue
      class="ag-theme-quartz"
      style="height: 30vh; width: 65vw"
      :columnDefs="columnDefs"
      :rowData="rowData.value"
      :defaultColDef="standardColSettings"
  ></ag-grid-vue>

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
.info-label {
  font-weight: bold;
}

.container {
  display: grid;
  grid-template-columns: 25vw 40vw;
  column-gap: 10px;
}

.blocked-submission-notice {
  max-width: 90%;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.blocked-submission-notice p {
  font-weight: bold;
  color: red;
}

.blocked-submission-notice button {
  font-size: small;
  margin-right: 10px;
  padding: 5px;
  margin-bottom: 10px;
  margin-top: 10px;
}
</style>
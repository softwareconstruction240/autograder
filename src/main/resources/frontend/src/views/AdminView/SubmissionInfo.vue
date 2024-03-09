<script setup lang="ts">
import type {Rubric, Submission, TestResult, User} from "@/types/types";
import {onMounted, reactive, ref} from "vue";
import {submissionsForUserGet} from "@/services/adminService";
import { AgGridVue } from 'ag-grid-vue3';
import type { CellClickedEvent } from 'ag-grid-community'
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-quartz.css";
import RubricTable from "@/views/PhaseView/RubricTable.vue";
import PopUp from "@/components/PopUp.vue";
import {loadRubricRows, renderPhaseCell, renderScoreCell, renderTimestampCell} from "@/utils/tableUtils";
import ResultsPopup from "@/views/PhaseView/ResultsPopup.vue";
import {
  generateClickableLink,
  nameFromNetId,
  readableTimestamp,
  scoreToPercentage
} from "../../utils/utils";

const { submission } = defineProps<{
  submission: Submission;
}>();

const testResults = ref<TestResult | undefined>(undefined);
const textResults = ref<string | undefined>(undefined);

const openResults = (event: CellClickedEvent) => {
  console.log(event.data)
  console.log(event.data.results.testResults)
  if (event.data.results.testResults) {
    testResults.value = event.data.results.testResults
    console.log("EXAM")
    console.log(event.data.results.testResults)
  } else {
    textResults.value = event.data.results.textResults
    console.log("TEXT")
    console.log(event.data.results.textResults)
  }
}

const columnDefs = reactive([
  { headerName: "Category", field: 'category', sortable: true, filter: true, flex:1 },
  { headerName: "Criteria", field: "criteria", sortable: true, filter: true, flex:2 },
  { headerName: "Notes", field: "notes", sortable: true, filter: true, flex:2, cellStyle: {"wordBreak": "normal"}, wrapText: true, onCellClicked: openResults},
  { headerName: "Points", field: "points", sortable: true, filter: true, flex:1, onCellClicked: openResults }
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
    <div>
      <p><span class="info-label">GitHub Repo: </span><span v-html="generateClickableLink(submission.repoUrl)"/></p>
      <p class="info-label">Submission Notes:</p>
      <p id="notes-field">{{submission.notes}}</p>
    </div>
  </div>

  <ag-grid-vue
      class="ag-theme-quartz"
      style="height: 30vh; width: 65vw"
      :columnDefs="columnDefs"
      :rowData="rowData.value"
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
</style>
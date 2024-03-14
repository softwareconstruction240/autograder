<script setup lang="ts">
import {onMounted, reactive, ref} from "vue";
import type {Submission, User} from "@/types/types";
import {submissionsLatestGet} from "@/services/adminService";
import {useAdminStore} from "@/stores/admin";
import PopUp from "@/components/PopUp.vue";
import { AgGridVue } from 'ag-grid-vue3';
import type { CellClickedEvent } from 'ag-grid-community'
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-quartz.css";
import {
  standardColSettings,
  renderPhaseCell,
  renderScoreCell,
  renderTimestampCell
} from "@/utils/tableUtils";
import StudentInfo from "@/views/AdminView/StudentInfo.vue";
import SubmissionInfo from "@/views/AdminView/SubmissionInfo.vue";
import {nameFromNetId} from "@/utils/utils";

const selectedSubmission = ref<Submission | null>(null);
const selectedStudent = ref<User | null>(null);

onMounted(async () => {
  const submissionsData = await submissionsLatestGet();
  submissionsData.sort((a, b) => b.timestamp.localeCompare(a.timestamp)) // Sort by timestamp descending
  var dataToShow: any = []
  submissionsData.forEach(submission => {
    dataToShow.push( {
      name: nameFromNetId(submission.netId),
      phase: submission.phase,
      timestamp: submission.timestamp,
      score: submission.score,
      notes: submission.notes,
      netId: submission.netId,
      passed: submission.passed,
      approved: submission.score > 0.5, // TODO: make it actually what its called
      submission: submission
      }
    )
  })
  rowData.value = dataToShow
})

const openSubmissionInfo = (event: CellClickedEvent) => {
  selectedSubmission.value = event.data.submission
}

const nameCellClicked = (event: CellClickedEvent) => {
  selectedStudent.value = useAdminStore().usersByNetId[event.data.netId];
}

const columnDefs = reactive([
  { headerName: "Name", field: 'name', flex:2, minWidth: 100, onCellClicked: nameCellClicked },
  { headerName: "Phase", field: 'phase', flex:1, cellRenderer: renderPhaseCell },
  { headerName: "Timestamp", field: 'timestamp', sort: 'desc', sortedAt: 0, filter: 'agDateColumnFilter', flex:1.5, cellRenderer: renderTimestampCell},
  { headerName: "Score", field: 'score', flex:1, minWidth: 85, cellRenderer: renderScoreCell, onCellClicked: openSubmissionInfo },
  { headerName: "Notes", field: 'notes', flex:5, onCellClicked: openSubmissionInfo },
])
const rowData = reactive({
  value: []
})
</script>


<template>
  <ag-grid-vue
      class="ag-theme-quartz"
      style="height: 75vh"
      :columnDefs="columnDefs"
      :rowData="rowData.value"
      :defaultColDef="standardColSettings"
  ></ag-grid-vue>

  <PopUp
      v-if="selectedSubmission"
      @closePopUp="selectedSubmission = null">
    <SubmissionInfo :submission="selectedSubmission"/>
  </PopUp>

  <PopUp
      v-if="selectedStudent"
      @closePopUp="selectedStudent = null">
    <StudentInfo :student="selectedStudent"></StudentInfo>
  </PopUp>

</template>

<style scoped>
</style>
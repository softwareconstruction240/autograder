<script setup lang="ts">
import {onMounted, reactive, ref} from "vue";
import type {Submission, User} from "@/types/types";
import {submissionsLatestGet} from "@/services/adminService";
import {useAdminStore} from "@/stores/admin";
import PopUp from "@/components/PopUp.vue";
import { AgGridVue } from 'ag-grid-vue3';
import type { ValueGetterParams, CellClickedEvent } from 'ag-grid-community'
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-quartz.css";
import {nameCellRender, renderPhaseCell, renderScoreCell, renderTimestampCell} from "@/utils/tableUtils";
import StudentInfo from "@/views/AdminView/StudentInfo.vue";
import SubmissionInfo from "@/views/AdminView/SubmissionInfo.vue";

const selectedSubmission = ref<Submission | null>(null);
const selectedStudent = ref<User | null>(null);

onMounted(async () => {
  const submissionsData = await submissionsLatestGet();
  submissionsData.sort((a, b) => b.timestamp.localeCompare(a.timestamp)) // Sort by timestamp descending
  var dataToShow: any = []
  submissionsData.forEach(submission => {
    dataToShow.push( submission )
  })
  rowData.value = dataToShow
})

const notesCellClicked = (event: CellClickedEvent) => {
  selectedSubmission.value = event.data
  console.log(selectedSubmission.value)
}

const nameCellClicked = (event: CellClickedEvent) => {
  selectedStudent.value = useAdminStore().usersByNetId[event.data.netId];
  console.log(event.data.netId)
  console.log(selectedStudent.value)
}

const columnDefs = reactive([
  { headerName: "Name", field: 'name', flex:2, cellRenderer: nameCellRender, onCellClicked: nameCellClicked },
  { headerName: "Phase", field: 'phase', flex:1, cellRenderer: renderPhaseCell },
  { headerName: "Timestamp", field: 'time', filter: 'agDateColumnFilter', flex:1.5, cellRenderer: renderTimestampCell},
  { headerName: "Score", field: 'score', flex:1, cellRenderer: renderScoreCell },
  { headerName: "Notes", field: 'notes', flex:5, onCellClicked: notesCellClicked },
])
const rowData = reactive({
  value: []
})
const rowClassRules = {
  'failed-row': (params: ValueGetterParams) => !params.data.passed,
}

</script>


<template>
  <ag-grid-vue
      class="ag-theme-quartz"
      style="height: 75vh"
      :columnDefs="columnDefs"
      :rowData="rowData.value"
      :rowClassRules="rowClassRules"
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
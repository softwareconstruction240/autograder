<script setup lang="ts">
import type {Rubric, Submission, User} from "@/types/types";
import {onMounted, reactive, ref} from "vue";
import {submissionsForUserGet} from "@/services/adminService";
import { AgGridVue } from 'ag-grid-vue3';
import type { CellClickedEvent } from 'ag-grid-community'
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-quartz.css";
import RubricTable from "@/views/PhaseView/RubricTable.vue";
import PopUp from "@/components/PopUp.vue";
import {renderPhaseCell, renderScoreCell, renderTimestampCell} from "@/utils/tableUtils";
import SubmissionInfo from "@/views/AdminView/SubmissionInfo.vue";
import {generateClickableLink} from "../../utils/utils";

const { student } = defineProps<{
  student: User;
}>();

const studentSubmissions = ref<Submission[]>([])
const selectedSubmission = ref<Submission | null>(null);

onMounted(async () => {
  studentSubmissions.value = await submissionsForUserGet(student.netId);
  var dataToShow: any = []
  studentSubmissions.value.forEach(submission => {
    dataToShow.push( submission )
  })
  rowData.value = dataToShow
});

const cellClickHandler = (event: CellClickedEvent) => {
  selectedSubmission.value = event.data;
  console.log("meme")
  console.log(event.data)
  console.log(selectedSubmission.value?.score)
}

const columnDefs = reactive([
  { headerName: "Phase", field: 'phase', sortable: true, filter: true, flex:1, cellRenderer: renderPhaseCell },
  { headerName: "Timestamp", field: "time", sortable: true, filter: true, flex:1, cellRenderer: renderTimestampCell},
  { headerName: "Score", field: "score", sortable: true, filter: true, flex:1, cellRenderer: renderScoreCell },
  { headerName: "Notes", field: "notes", sortable: true, filter: true, flex:5, onCellClicked: cellClickHandler }
])
const rowData = reactive({
  value: []
})
</script>

<template>
  <h3>{{student.firstName}} {{student.lastName}}</h3>
  <p>netID: {{student.netId}}</p>
  <p>Github Repo: <span v-html="generateClickableLink(student.repoUrl)"/> </p>

  <ag-grid-vue
      class="ag-theme-quartz"
      style="height: 35vh; width: 75vw"
      :columnDefs="columnDefs"
      :rowData="rowData.value"
  ></ag-grid-vue>

  <PopUp
      v-if="selectedSubmission"
      @closePopUp="selectedSubmission = null">
    <SubmissionInfo :submission="selectedSubmission"/>
  </PopUp>
</template>

<style scoped>
a:visited, a {
  color: darkblue;
  font-style: italic;
}
</style>
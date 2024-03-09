<script setup lang="ts">
import type {Rubric, Submission, User} from "@/types/types";
import {onMounted, reactive, ref} from "vue";
import {submissionsForUserGet, usersGet} from "@/services/adminService";
import { AgGridVue } from 'ag-grid-vue3';
import type { ValueGetterParams, CellClickedEvent } from 'ag-grid-community'
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-alpine.css";
import RubricTable from "@/views/PhaseView/RubricTable.vue";
import PopUp from "@/components/PopUp.vue";

const { student } = defineProps<{
  student: User;
}>();

const studentSubmissions = ref<Submission[]>([])

const selectedRubric = ref<Rubric | null>(null);

onMounted(async () => {
  studentSubmissions.value = await submissionsForUserGet(student.netId);
  var dataToShow: any = []
  studentSubmissions.value.forEach(submission => {
    dataToShow.push(
        {
          phase: submission.phase,
          time: submission.timestamp,
          score: (submission.score * 100) + "%",
          notes: submission.notes,
          rubric: submission.rubric,
          passed: submission.passed
        }
    )
  })
  rowData.value = dataToShow
});

const cellClickHandler = (event: CellClickedEvent) => {
  selectedRubric.value = event.data.rubric;
}

const columnDefs = reactive([
  { headerName: "Phase", field: 'phase', sortable: true, filter: true, flex:1},
  { headerName: "Timestamp", field: "time", sortable: true, filter: true, flex:2 },
  { headerName: "Score", field: "score", sortable: true, filter: true, flex:1 },
  { headerName: "Notes", field: "notes", sortable: true, filter: true, flex:5, onCellClicked: cellClickHandler }
])
const rowData = reactive({
  value: []
})
</script>

<template>
  <h3>{{student.firstName}} {{student.lastName}}</h3>
  <p>netID: {{student.netId}}</p>
  <p>Github Repo: <a href="{{student.repoUrl}}">{{student.repoUrl}}</a> </p>

  <ag-grid-vue
      class="ag-theme-alpine"
      style="height: 35vh; width: 60vw"
      :columnDefs="columnDefs"
      :rowData="rowData.value"
  ></ag-grid-vue>

  <PopUp
      v-if="selectedRubric"
      @closePopUp="selectedRubric = null">
    <RubricTable :rubric="selectedRubric"/>
  </PopUp>
</template>

<style scoped>
a:visited, a {
  color: darkblue;
  font-style: italic;
}
</style>
<script setup lang="ts">
import { AgGridVue } from 'ag-grid-vue3';
import type { ValueGetterParams, CellClickedEvent } from 'ag-grid-community'
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-alpine.css";
import {onMounted, reactive, ref} from "vue";
import {submissionsForUserGet, usersGet} from "@/services/adminService";
import PopUp from "@/components/PopUp.vue";
import type {User} from "@/types/types";
import StudentInfo from "@/views/AdminView/StudentInfo.vue";

let selectedStudent = ref<User | null>(null);

const cellClickHandler = (event: CellClickedEvent) => {
  let findResult = studentData.find(user => user.netId === event.data.netID)
  selectedStudent.value = findResult || null; // Setting selected student opens a popup
  console.log(selectedStudent)
}

let studentData: User[] = [];

onMounted(async () => {
  const userData = await usersGet();
  studentData = userData.filter(user => user.role == "STUDENT") // get rid of users that aren't students
  var dataToShow: any = []
  studentData.forEach(student => {
    dataToShow.push(
        {
          name: student.firstName + " " + student.lastName,
          netID: student.netId,
          github: student.repoUrl
        }
    )
  })
  console.log(dataToShow)
  rowData.value = dataToShow
})

const columnDefs = reactive([
  { headerName: "Student Name", field: 'name', sortable: true, filter: true, onCellClicked: cellClickHandler },
  { headerName: "BYU netID", field: "netID", sortable: true, filter: true, onCellClicked: cellClickHandler },
  { headerName: "Github Repo URL", field: "github", flex:1, sortable: false, filter: true, cellRenderer: function(params: ValueGetterParams) {
      return '<a href="' + params.data.github + '" target="_blank">' + params.data.github + '</a>'
    }}
])
let rowData = reactive({
  value: []
})
</script>

<template>
  <ag-grid-vue
      class="ag-theme-alpine"
      style="height: 75vh"
      :columnDefs="columnDefs"
      :rowData="rowData.value"
  ></ag-grid-vue>

  <PopUp
      v-if="selectedStudent"
      @closePopUp="selectedStudent = null">
    <StudentInfo :student="selectedStudent"></StudentInfo>
  </PopUp>

</template>

<style scoped>
</style>
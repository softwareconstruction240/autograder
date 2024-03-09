<script setup lang="ts">
import { AgGridVue } from 'ag-grid-vue3';
import type { CellClickedEvent } from 'ag-grid-community'
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-quartz.css";
import {onMounted, reactive, ref} from "vue";
import {testStudentModeGet, usersGet} from "@/services/adminService";
import PopUp from "@/components/PopUp.vue";
import type {User} from "@/types/types";
import StudentInfo from "@/views/AdminView/StudentInfo.vue";
import {nameFromNetIdCellRender, renderRepoLinkCell} from "@/utils/tableUtils";

const selectedStudent = ref<User | null>(null);
let studentData: User[] = [];

const cellClickHandler = (event: CellClickedEvent) => {
  let findResult = studentData.find(user => user.netId === event.data.netId)
  selectedStudent.value = findResult || null; // Setting selected student opens a popup
}

onMounted(async () => {
  const userData = await usersGet();
  studentData = userData.filter(user => user.role == "STUDENT") // get rid of users that aren't students
  var dataToShow: any = []
  studentData.forEach(student => {dataToShow.push(student)})
  rowData.value = dataToShow
})

const columnDefs = reactive([
  { headerName: "Student Name", field: 'name', flex: 2, sortable: true, filter: true, cellRenderer: nameFromNetIdCellRender, onCellClicked: cellClickHandler },
  { headerName: "BYU netID", field: "netId", flex: 1, sortable: true, filter: true, onCellClicked: cellClickHandler },
  { headerName: "Github Repo URL", field: "repoUrl", flex: 5, sortable: false, filter: true, cellRenderer: renderRepoLinkCell }
])
const rowData = reactive({
  value: []
})

const activateTestStudentMode = async () => {
  await testStudentModeGet()
  window.location.href = '/';
}
</script>

<template>
  <div class="test-student-mode-container">
    <div>
      <p>Click on the button below to become the test student</p>
      <p>- you will need to log out and back in again to return to admin
        mode</p>
      <p>- you will not be able to enter student mode unless the Test Student has a submission for the GitHub Repository
        assignment on Canvas</p>
    </div>
    <div>
      <button @click="activateTestStudentMode">Go to Test Student Mode</button>
    </div>
  </div>

  <ag-grid-vue
      class="ag-theme-quartz"
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
.test-student-mode-container {
  align-items: center;
  margin: 10px;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  background-color: #f2f2f2;
  cursor: pointer;
  display: grid;
  grid-template-columns: 3fr 1fr;
}
</style>
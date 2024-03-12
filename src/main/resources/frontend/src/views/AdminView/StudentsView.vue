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
import {renderRepoLinkCell, standardColSettings} from "@/utils/tableUtils";

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
  studentData.forEach(student => {
    dataToShow.push( {
          name: student.firstName + " " + student.lastName,
          netId: student.netId,
          repoUrl: student.repoUrl
        }
    )
  })
  rowData.value = dataToShow
})

const columnDefs = reactive([
  { headerName: "Student Name", field: "name", flex: 2, onCellClicked: cellClickHandler },
  { headerName: "BYU netID", field: "netId", flex: 1, onCellClicked: cellClickHandler },
  { headerName: "Github Repo URL", field: "repoUrl", flex: 5, sortable: false, cellRenderer: renderRepoLinkCell }
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
      <p>Use "Test Student Mode" to use the autograder as the course's test student</p>
      <p>- you will need to log out and back in again to return to admin mode</p>
      <p>- the test student must have a Github Repo submitted to the Canvas assignment</p>
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
      :defaultColDef="standardColSettings"
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
<script setup lang="ts">
import { AgGridVue } from 'ag-grid-vue3';
import type { ValueGetterParams } from 'ag-grid-community'
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-alpine.css";
import {onMounted, reactive} from "vue";
import {usersGet} from "@/services/adminService";

const columnDefs = reactive([
        { headerName: "Name", field: 'name', sortable: true, filter: true},
        { headerName: "BYU netID", field: "netID", sortable: true, filter: true},
        { headerName: "Github URL", field: "github", flex:1, sortable: false, filter: true, cellRenderer: function(params: ValueGetterParams) {
          return '<a href="' + params.data.github + '" target="_blank">' + params.data.github + '</a>'
          }}
      ])
let rowData = reactive({
  value: []
})

onMounted(async () => {
  const userData = await usersGet();
  const studentData = userData.filter(user => user.role == "STUDENT") // get rid of users that aren't students
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
</script>

<template>
  <ag-grid-vue
      class="ag-theme-alpine"
      style="height: 75vh"
      :columnDefs="columnDefs"
      :rowData="rowData.value"
  ></ag-grid-vue>

</template>

<style scoped>
</style>
<script setup lang="ts">
import { AgGridVue } from 'ag-grid-vue3';
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-alpine.css";

// eslint-disable-next-line vue/no-export-in-script-setup
const columnDefs = [
        { headerName: "Name", field: 'name'},
        { headerName: "BYU netID", field: "netID"},
        { headerName: "Github URL", field: "github", flex:1}
      ]
const rowData = [
        {name: "Roger McGee", netID: "rogerm", github: "http://girthagk.com"},
        {name: "Steve McGee", netID: "stevem", github: "http://github.com"},
        {name: "Roger McGee", netID: "rogerm", github: "http://github.com"},
        {name: "Steve McGee", netID: "stevem", github: "http://github.com"},
        {name: "Roger McGee", netID: "rogerm", github: "http://github.com"},
        {name: "Steve McGee", netID: "stevem", github: "http://github.com"},
        {name: "Roger McGee", netID: "rogerm", github: "http://github.com"},
        {name: "Steve McGee", netID: "stevem", github: "http://github.com"},
        {name: "Roger McGee", netID: "rogerm", github: "http://github.com"},
        {name: "Steve McGee", netID: "stevem", github: "http://github.comasdfghdfgdhdsasfgdfhfjfrewrtyhgftryhgfd"},
        {name: "Roger McGee", netID: "rogerm", github: "http://github.com"},
        {name: "Steve McGee", netID: "stevem", github: "http://github.com"},
        {name: "Roger McGee", netID: "rogerm", github: "http://github.com"},
        {name: "Steve McGee", netID: "stevem", github: "http://github.com"},
        {name: "Roger McGee", netID: "rogerm", github: "http://github.com"},
        {name: "Steve McGee", netID: "stevem", github: "http://github.com"},
        {name: "Roger McGee", netID: "rogerm", github: "http://github.com"},
        {name: "Steve McGee", netID: "stevem", github: "http://github.com"},
      ]
</script>

<template>
  <ag-grid-vue
      class="ag-theme-alpine"
      style="height: 75vh"
      :columnDefs="columnDefs"
      :rowData="rowData"
  ></ag-grid-vue>

</template>

<style scoped>
</style>
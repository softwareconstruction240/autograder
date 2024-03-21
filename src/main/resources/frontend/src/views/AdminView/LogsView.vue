<script setup lang="ts">


import {onMounted, reactive, ref} from "vue";
import {standardColSettings} from "@/utils/tableUtils";
import {AgGridVue} from "ag-grid-vue3";
import type {CellClickedEvent} from 'ag-grid-community'
import {logGet, logsGet} from "@/services/logsService";
import PopUp from "@/components/PopUp.vue";

const logFiles = ref<string[]>([]);
const logFile = ref<string | undefined>(undefined);

onMounted(async () => {
  await getLogFiles()
})

const getLogFiles = async () => {
  logFiles.value = await logsGet();
  rowData.value = loadRows()
}
const setLogFile = async (event: CellClickedEvent) => {
  logFile.value = await logGet(event.data.name);
}

const columnDefs = reactive([
  {headerName: "Name", field: 'name', flex: 1, onCellClicked: setLogFile}
])

const loadRows = () => {
  const rows: any = []
  for (let i = 0; i < logFiles.value.length; i++) {
    const item = logFiles.value[i]
    if (item) {
      const row = {
        name: item
      }
      rows.push(row)
    }
  }
  return rows
}

const rowData = reactive({
  value: [] = loadRows()
})

</script>

<template>
  <ag-grid-vue
      class="ag-theme-quartz"
      style="height: 65vh"
      :columnDefs="columnDefs"
      :rowData="rowData.value"
      :defaultColDef="standardColSettings"
  ></ag-grid-vue>
  <PopUp
      v-if="logFile"
      @closePopUp="() => {
            logFile = undefined;
        }">
    <div class="log-popup">
      <h2>Log</h2>
      <p style="white-space: pre">{{ logFile }}</p>
    </div>
  </PopUp>

</template>

<style scoped>

</style>
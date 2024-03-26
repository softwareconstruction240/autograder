<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import type { Submission } from '@/types/types'
import { renderPhaseCell, renderScoreCell, renderTimestampCell, standardColSettings } from '@/utils/tableUtils'
import { submissionsGet } from '@/services/submissionService'
import { AgGridVue } from 'ag-grid-vue3'


onMounted(async () => { await loadSubmissions() })

const loadSubmissions = async () => {
  try {
    loadSubmissionsToTable( await submissionsGet(null) )
  } catch (e) {
    console.log(e)
  }
}

const loadSubmissionsToTable = (submissionsData : Submission[]) => {
  var dataToShow: any = []
  submissionsData.forEach(submission => {
    dataToShow.push( {
        phase: submission.phase,
        timestamp: submission.timestamp,
        score: submission.score,
        notes: submission.notes,
        netId: submission.netId,
        headHash: submission.headHash,
        submission: submission
      }
    )
  })
  rowData.value = dataToShow
  console.log(rowData.value)
}

const columnDefs = reactive([
  { headerName: "Phase", field: 'phase', flex:1, cellRenderer: renderPhaseCell },
  { headerName: "Timestamp", field: 'timestamp', sort: 'desc', sortedAt: 0, filter: 'agDateColumnFilter', flex:1.5, cellRenderer: renderTimestampCell},
  { headerName: "Score", field: 'score', flex:1, filter: false, sortable: false, cellRenderer: renderScoreCell },
  { headerName: "Notes", field: 'notes', flex:3, sortable: false, hide: (window.innerWidth < 600) },
])
const rowData = reactive({
  value: []
})
</script>

<template>
  <ag-grid-vue
    class="ag-theme-quartz"
    style="height: 40vh; width: 100%"
    :columnDefs="columnDefs"
    :rowData="rowData.value"
    :defaultColDef="standardColSettings"
  ></ag-grid-vue>
</template>

<style scoped>

</style>
<script setup lang="ts">
import {onMounted, reactive, ref} from "vue";
import type {Phase, Submission, User} from "@/types/types";
import {submissionsLatestGet} from "@/services/adminService";
import {useAdminStore} from "@/stores/admin";
import PopUp from "@/components/PopUp.vue";
import { AgGridVue } from 'ag-grid-vue3';
import type { CellClickedEvent } from 'ag-grid-community'
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-quartz.css";
import {
  standardColSettings,
  renderPhaseCell,
  renderScoreCell,
  renderTimestampCell
} from "@/utils/tableUtils";
import StudentInfo from "@/views/AdminView/StudentInfo.vue";
import SubmissionInfo from "@/views/AdminView/SubmissionInfo.vue";
import {generateClickableLink, nameFromNetId} from "@/utils/utils";
import {adminSubmissionPost} from "@/services/submissionService";
import Dropdown from "@/components/Dropdown.vue";
import LiveStatus from "@/views/PhaseView/LiveStatus.vue";

const selectedSubmission = ref<Submission | null>(null);
const selectedStudent = ref<User | null>(null);
const runningAdminRepo = ref<boolean>(false)
const DEFAULT_SUBMISSIONS_TO_LOAD = 25;
let allSubmissionsLoaded = false;
let adminRepo = reactive( {
  value: ""
})


onMounted(async () => { await resetPage() })

const resetPage = async () => {
  runningAdminRepo.value = false;
  selectedSubmission.value = null;
  allSubmissionsLoaded = false;
  const submissionsData = await submissionsLatestGet(DEFAULT_SUBMISSIONS_TO_LOAD);
  loadSubmissionsToTable(submissionsData);
}

const loadAllSubmissions = async () => {
  loadSubmissionsToTable( await submissionsLatestGet() )
  allSubmissionsLoaded = true;
}

const loadSubmissionsToTable = (submissionsData : Submission[]) => {
  var dataToShow: any = []
  submissionsData.forEach(submission => {
    dataToShow.push( {
          name: nameFromNetId(submission.netId),
          phase: submission.phase,
          timestamp: submission.timestamp,
          score: submission.score,
          notes: submission.notes,
          netId: submission.netId,
          admin: submission.admin,
          submission: submission
        }
    )
  })
  rowData.value = dataToShow
}

const notesCellClicked = (event: CellClickedEvent) => {
  selectedSubmission.value = event.data.submission
}

const adminDoneGrading = async () => {
  let data = await submissionsLatestGet(1)
  selectedSubmission.value = data[0]
}

const nameCellClicked = (event: CellClickedEvent) => {
  selectedStudent.value = useAdminStore().usersByNetId[event.data.netId];
}

const columnDefs = reactive([
  { headerName: "Name", field: 'name', flex:2, onCellClicked: nameCellClicked },
  { headerName: "Phase", field: 'phase', flex:1, cellRenderer: renderPhaseCell },
  { headerName: "Timestamp", field: 'timestamp', sort: 'desc', sortedAt: 0, filter: 'agDateColumnFilter', flex:1.5, cellRenderer: renderTimestampCell},
  { headerName: "Score", field: 'score', flex:1, cellRenderer: renderScoreCell },
  { headerName: "Notes", field: 'notes', flex:5, onCellClicked: notesCellClicked },
])
const rowData = reactive({
  value: []
})

const adminSubmit = async (phase: Phase) => {
  try {
    await adminSubmissionPost(phase, adminRepo.value)
    runningAdminRepo.value = true;
  } catch (error) {
    if (error instanceof Error) { alert("Error running grader: " + error.message) }
    else { alert("Unknown error running grader") }
  }
}
</script>


<template>

  <div class="adminSubmission">
    <input v-model="adminRepo.value" type="text" id="repoUrlInput" placeholder="Github Repo URL"/>
    <Dropdown :disabled="!adminRepo.value.includes('github.com/')">
      <template v-slot:dropdown-parent>
        <button>Grade Repo</button>
      </template>
      <template v-slot:dropdown-items>
        <a @click="adminSubmit('0')">Phase 0</a>
        <a @click="adminSubmit('1')">Phase 1</a>
        <a @click="adminSubmit('3')">Phase 3</a>
        <a @click="adminSubmit('4')">Phase 4</a>
        <a @click="adminSubmit('5')">Phase 5</a>
        <a @click="adminSubmit('6')">Phase 6</a>
        <a @click="adminSubmit('42')">Quality</a>
      </template>
    </Dropdown>
  </div>

  <ag-grid-vue
      class="ag-theme-quartz"
      style="height: 65vh"
      :columnDefs="columnDefs"
      :rowData="rowData.value"
      :defaultColDef="standardColSettings"
  ></ag-grid-vue>

  <div class="container">
    <p v-if="allSubmissionsLoaded">All latest submissions are loaded</p>
    <p v-else>Currently only the {{DEFAULT_SUBMISSIONS_TO_LOAD}} most recent latest submssions are loaded</p>
    <button id="loadMore" @click="loadAllSubmissions">
      <span v-if="allSubmissionsLoaded">Reload submissions list</span>
      <span v-else>Load all latest submissions</span>
    </button>
  </div>

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

  <PopUp
    v-if="runningAdminRepo"
    @closePopUp="resetPage">
    <div v-if="!selectedSubmission">
      <h3 style="width: 70vw">Running Grader As Admin</h3>
      <p>Github Repo: <span v-html="generateClickableLink(adminRepo.value)"/></p>
      <LiveStatus @show-results="adminDoneGrading"/>
    </div>
    <SubmissionInfo
        v-if="selectedSubmission"
        :submission="selectedSubmission"/>
  </PopUp>

</template>

<style scoped>
#repoUrlInput {
  flex-grow: 1;
  padding: 10px;
  margin-right: 10px;
}
.adminSubmission {
  display: flex;
  padding: 10px;
}
.container {
  padding: 10px;
  display: grid;
  grid-template-columns: 2fr 1fr;
  margin: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  background-color: #f2f2f2;
  align-items: center;
}

#loadMore {
  font-size: medium;
}
</style>
<script setup lang="ts">
import type {Submission, TestResult} from "@/types/types";
import {reactive, ref} from "vue";
import { AgGridVue } from 'ag-grid-vue3';
import type { CellClickedEvent } from 'ag-grid-community'
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-quartz.css";
import {standardColSettings, loadRubricRows, wrappingColSettings} from "@/utils/tableUtils";
import ResultsPopup from "@/views/PhaseView/ResultsPopup.vue";
import {
  generateClickableLink,
  nameFromNetId,
  readableTimestamp,
  scoreToPercentage
} from "@/utils/utils";
import RubricTable from "@/views/PhaseView/RubricTable.vue";

const { submission } = defineProps<{
  submission: Submission;
}>();

</script>

<template>
  <h3>{{submission.phase}} - {{nameFromNetId(submission.netId)}}</h3>
  <div class="container">
    <div>
      <p><span class="info-label">Submitted:</span> {{readableTimestamp(submission.timestamp)}}</p>
      <p><span class="info-label">NetID: </span> {{submission.netId}}</p>
      <p><span class="info-label">Overall Score: </span>{{scoreToPercentage(submission.score)}}
        <span v-if="submission.passed">Submission passed!</span>
        <span v-else class="failure">Submission failed</span>
      </p>
      <p v-if="submission.admin" class="info-label">This is an admin submission</p>
    </div>
    <div>
      <p><span class="info-label">GitHub Repo: </span><span v-html="generateClickableLink(submission.repoUrl)"/></p>
      <p class="info-label">Submission Notes:</p>
      <p id="notes-field">{{submission.notes}}</p>
    </div>
  </div>

  <RubricTable :rubric="submission.rubric"/>

</template>

<style scoped>
.info-label {
  font-weight: bold;
}

.container {
  display: grid;
  grid-template-columns: 25vw 40vw;
  column-gap: 10px;
}
</style>
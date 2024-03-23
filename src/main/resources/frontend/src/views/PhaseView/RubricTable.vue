<script setup lang="ts">
import type {Rubric, TestResult} from "@/types/types";
import ResultsPopup from "@/views/PhaseView/ResultsPopup.vue";
import {ref} from "vue";

defineProps<{
  rubric: Rubric;
}>();

const selectedTestResults = ref<TestResult | undefined>(undefined);
const selectedTextResults = ref<string | undefined>(undefined);

const failedTests = (testResults: TestResult) => {
  return testResults.root.numTestsFailed > 0 || testResults.root.numTestsPassed === 0;
}

</script>

<template>
  <div class="rubric-container">
    <table class="rubric-table">
      <tr>
        <th>Category</th>
        <th>Criteria</th>
        <th>Notes</th>
        <th>Points</th>
        <th>Results</th>
      </tr>
      <tr
          v-if="rubric.passoffTests"
          :class="failedTests(rubric.passoffTests.results.testResults) && 'failed'"
      >
        <td>{{rubric.passoffTests.category}}</td>
        <td>{{rubric.passoffTests.criteria}}</td>
        <td>{{rubric.passoffTests.results.notes}}</td>
        <td>{{Math.round(rubric.passoffTests.results.score)}} / {{rubric.passoffTests.results.possiblePoints}}</td>
        <td
            class="selectable"
            @click="() => {
              selectedTestResults = rubric.passoffTests.results.testResults;
              selectedTextResults = undefined;
            }"
        >Click here</td>
      </tr>
      <tr
          v-if="rubric.unitTests"
          :class="failedTests(rubric.unitTests.results.testResults) && 'failed'"
      >
        <td>{{rubric.unitTests.category}}</td>
        <td>{{rubric.unitTests.criteria}}</td>
        <td>{{rubric.unitTests.results.notes}}</td>
        <td>{{Math.round(rubric.unitTests.results.score)}} / {{rubric.unitTests.results.possiblePoints}}</td>
        <td
            class="selectable"
            @click="() => {
              selectedTestResults = rubric.unitTests.results.testResults;
              selectedTextResults = undefined;
            }"
        >Click here</td>
      </tr>
      <tr v-if="rubric.quality">
        <td>{{ rubric.quality.category }}</td>
        <td>{{ rubric.quality.criteria }}</td>
        <td>{{ rubric.quality.results.notes }}</td>
        <td>{{ Math.round(rubric.quality.results.score) }} / {{ rubric.quality.results.possiblePoints }}</td>
        <td
            class="selectable"
            @click="() => {
              selectedTestResults = undefined
              selectedTextResults = rubric.quality.results.textResults;
            }"
        >Click here</td>
      </tr>
    </table>
  </div>

  <ResultsPopup
      v-if="selectedTestResults || selectedTextResults"
      :text-results="selectedTextResults"
      :test-results="selectedTestResults"
      @closePopUp="() => {
        selectedTestResults = undefined;
        selectedTextResults = undefined;
      }"
  />

</template>

<style scoped>

.rubric-container {
  overflow-x: auto;
}

table, th, td {
  border: 2px solid #ddd;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  background-color: #e3e2e2;
  padding: 8px;
  text-align: left;
}

td {
  padding: 8px;
  word-break: break-word;
  white-space: pre-wrap;
}

.selectable:hover {
  cursor: pointer;
  background-color: #e1e1e1;
}

.failed {
  background-color: rgba(255, 204, 204, 0.6);
}
</style>
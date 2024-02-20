<script setup lang="ts">
import type {Rubric, TestResult} from "@/types/types";
import ResultsPopup from "@/views/PhaseView/ResultsPopup.vue";
import {ref} from "vue";

defineProps<{
  rubric: Rubric;
}>();

const selectedTestResults = ref<TestResult | undefined>(undefined);
const selectedTextResults = ref<string | undefined>(undefined);

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
      <tr v-if="rubric.passoffTests">
        <td>{{rubric.passoffTests.category}}</td>
        <td>{{rubric.passoffTests.criteria}}</td>
        <td>{{rubric.passoffTests.results.notes}}</td>
        <td>{{rubric.passoffTests.results.score}} / {{rubric.passoffTests.results.possiblePoints}}</td>
        <td
            class="selectable"
            @click="() => {
              console.log('hi')
              selectedTestResults = rubric.passoffTests.results.testResults;
              selectedTextResults = undefined;
            }"
        >Click here</td>
      </tr>
      <tr v-if="rubric.unitTests">
        <td>{{rubric.unitTests.category}}</td>
        <td>{{rubric.unitTests.criteria}}</td>
        <td>{{rubric.unitTests.results.notes}}</td>
        <td>{{rubric.unitTests.results.score}} / {{rubric.unitTests.results.possiblePoints}}</td>
        <td
            class="selectable"
            @click="() => {
              selectedTestResults = rubric.unitTests.results.testResults;
              selectedTextResults = undefined;
            }"
        >Click here</td>
      </tr>
      <tr v-if="rubric.styleTests">
        <td>{{rubric.styleTests.category}}</td>
        <td>{{rubric.styleTests.criteria}}</td>
        <td>{{rubric.styleTests.results.notes}}</td>
        <td>{{rubric.styleTests.results.score}} / {{rubric.styleTests.results.possiblePoints}}</td>
        <td
            class="selectable"
            @click="() => {
              selectedTestResults = undefined
              selectedTextResults = rubric.styleTests.results.textResults;
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
}

.selectable:hover {
  cursor: pointer;
  background-color: #e1e1e1;
}
</style>
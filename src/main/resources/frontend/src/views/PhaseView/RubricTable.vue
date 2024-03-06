<script setup lang="ts">
import type {Rubric, TestResult} from "@/types/types";
import ResultsPopup from "@/views/PhaseView/ResultsPopup.vue";
import {ref} from "vue";
import RubricTableRow from "@/views/PhaseView/RubricTableRow.vue";

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
      <RubricTableRow :rubric-item="rubric.passoffTests" :results-clicked="() => {selectedTestResults = rubric.passoffTests.results.testResults;}"/>
      <RubricTableRow :rubric-item="rubric.unitTests" :results-clicked="() => {selectedTestResults = rubric.unitTests.results.testResults;}"/>
      <RubricTableRow :rubric-item="rubric.quality" :results-clicked="() => {selectedTextResults = rubric.quality.results.textResults;}"/>
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

table, th {
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

</style>
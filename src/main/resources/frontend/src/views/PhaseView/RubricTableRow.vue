<script setup lang="ts">
import type {RubricItem, TestResult} from "@/types/types";

defineProps<{
  rubricItem: RubricItem;
  resultsClicked: () => void;
}>();

const failedTests = (testResults: TestResult) => {
  return testResults && (testResults.numTestsFailed > 0 || testResults.numTestsPassed === 0);
}

const addColor = (qualityNotes: string) => {
  return qualityNotes.replace(/✓/g, `<span class="success">✓</span>`).replace(/✗/g, `<span class="failure">✗</span>`);
}
</script>

<template>
  <tr
      v-if="rubricItem"
      :class="failedTests(rubricItem.results.testResults) && 'failed'"
  >
    <td>{{rubricItem.category}}</td>
    <td>{{rubricItem.criteria}}</td>
    <td v-html="addColor(rubricItem.results.notes)"/>
    <td>{{Math.round(rubricItem.results.score)}} / {{rubricItem.results.possiblePoints}}</td>
    <td
        class="selectable"
        @click="() => {resultsClicked();}"
    >Click here</td>
  </tr>
</template>

<style scoped>

td {
  border: 2px solid #ddd;
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
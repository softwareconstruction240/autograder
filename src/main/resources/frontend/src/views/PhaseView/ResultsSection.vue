<script setup lang="ts">
import {readableTimestamp} from "@/utils/utils";
import type {Submission, TestResult} from "@/types/types";

defineProps<{
  submission: Submission;
}>();

const prettifyResults = (node: TestResult, indent: string) => {
  if (!node) return "";

  let result = indent + node.testName;

  if (node.passed !== undefined) {
    result += node.passed ? ` <span class="success">✓</span>` : ` <span class="failure">✗</span>`;
    if (node.errorMessage !== null && node.errorMessage !== undefined && node.errorMessage !== "") {
      result += `<br/>${indent}   ↳<span class="failure">${node.errorMessage}</span>`;
    }
  } else {
    if (node.ecCategory !== undefined) {
      result += ` (${node.numExtraCreditPassed} passed, ${node.numExtraCreditFailed} failed)`
    } else {
      result += ` (${node.numTestsPassed} passed, ${node.numTestsFailed} failed)`
    }
  }
  result += "<br/>";

  for (const key in node.children) {
    if (node.children.hasOwnProperty(key)) {
      result += prettifyResults(node.children[key], indent + "&nbsp;&nbsp;&nbsp;&nbsp;");
    }
  }

  return result;
}

</script>

<template>
  <div class="container" v-if="submission !== undefined && submission !== null">
    <div id="submission-title">
      <h2>Results from {{ readableTimestamp(submission.timestamp) }}</h2>
    </div>
    <div id="submission-score">
      <h3>Score: {{ submission.score * 100 }}%</h3>
    </div>
    <div id="submission-notes">
      {{ submission.notes }}
    </div>
    <div
        id="submission-results-container"
        v-html="prettifyResults(submission.testResults, '')"
    ></div>
  </div>
</template>

<style scoped>
#submission-title, #submission-score, #submission-notes {
  text-align: center;
}

#submission-results-container {
  /* sunken 3d */
  border: 1px solid #ccc;
  border-radius: 5px;
  padding: 10px;
  margin: 10px;

  box-shadow: inset 0 0 2px #000000;
}
</style>
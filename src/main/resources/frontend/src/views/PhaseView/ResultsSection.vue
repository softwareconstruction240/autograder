<script setup lang="ts">
import {readableTimestamp} from "@/utils/utils";
import type {Submission, TestResult} from "@/types/types";

const props = defineProps<{
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
    result += ` (${node.numTestsPassed} passed, ${node.numTestsFailed} failed)`
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
      <h2>Results for {{ readableTimestamp(new Date(submission.timestamp)) }}</h2>
    </div>
    <div id="submission-notes">
      some notes here
    </div>
    <div
        id="submission-results-container"
        v-html="prettifyResults(submission.testResults, '')"
    ></div>
  </div>
</template>

<style scoped>
#submission-results-container {
  /* sunken 3d */
  border: 1px solid #ccc;
  border-radius: 5px;
  padding: 10px;
  margin: 10px;
}
</style>
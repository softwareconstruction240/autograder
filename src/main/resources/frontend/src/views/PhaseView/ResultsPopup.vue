<script setup lang="ts">

import PopUp from "@/components/PopUp.vue";
import {defineEmits} from "vue";
import type {TestResult} from "@/types/types";

defineProps<{
  testResults?: TestResult | undefined;
  textResults?: string | undefined;
}>();

defineEmits({
  closePopUp: null,
});

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
  <PopUp @closePopUp="$emit('closePopUp')">
    <div class="results-popup">
      <h2>Results</h2>
      <div v-if="testResults" v-html="prettifyResults(testResults, '')"></div>
      <div v-if="textResults" v-html="textResults"></div>
    </div>
  </PopUp>
</template>

<style scoped>

</style>
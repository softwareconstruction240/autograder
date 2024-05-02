<script setup lang="ts">
import Rubric from "@/views/PhaseView/RubricTable.vue";
import {type Submission} from "@/types/types";
import {commitVerificationFailed} from "@/utils/utils";

const props = defineProps<{
  submission: Submission;
}>();

const roundTwoDecimals = (num: number) => {
  return Math.round((num + Number.EPSILON) * 100) / 100;
}

const score = roundTwoDecimals(props.submission.score * 100);

</script>

<template>
  <h1 v-if="!submission.passed">Failed</h1>
  <h1 v-else-if="commitVerificationFailed(submission)">Score withheld for commits.<br>Raw score: {{score}}%</h1>
  <h1 v-else>Passed with {{score}}%</h1>
  <h2 v-html="submission.notes.replace('\n', '<br />')"></h2>
  <Rubric :rubric="submission.rubric" />
</template>

<style scoped>
h1, h2 {
  text-align: center;
}
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
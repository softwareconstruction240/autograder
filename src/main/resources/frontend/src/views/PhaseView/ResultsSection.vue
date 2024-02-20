<script setup lang="ts">
import Rubric from "@/views/PhaseView/RubricTable.vue";
import type {Submission} from "@/types/types";

defineProps<{
  submission: Submission;
}>();

const roundTwoDecimals = (num: number) => {
  return Math.round((num + Number.EPSILON) * 100) / 100;
}

</script>

<!--<template>-->
<!--  <div class="container" v-if="submission !== undefined && submission !== null">-->
<!--    <div id="submission-title">-->
<!--      <h2>Results from {{ readableTimestamp(submission.timestamp) }}</h2>-->
<!--    </div>-->
<!--    <div id="submission-score">-->
<!--      <h3>Score: {{ submission.score * 100 }}%</h3>-->
<!--    </div>-->
<!--    <div id="submission-notes">-->
<!--      {{ submission.notes }}-->
<!--    </div>-->
<!--    <div-->
<!--        id="submission-results-container"-->
<!--        v-html="prettifyResults(submission.testResults, '')"-->
<!--    ></div>-->
<!--  </div>-->
<!--</template>-->
<template>
  <h1 v-if="submission.passed">Passed with {{roundTwoDecimals(submission.score * 100)}}%</h1>
  <h1 v-else>Failed</h1>
  <h2>{{submission.notes}}</h2>
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
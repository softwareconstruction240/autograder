<script setup lang="ts">
import type { Submission } from '@/types/types'
import { scoreToPercentage, roundTwoDecimals } from '@/utils/utils'

defineProps<{
  submission: Submission;
}>();
</script>

<template>
  <div class="results-preview-container">
    <h2 v-if="submission.passed">Submission passed! <i class="fa-solid fa-circle-check" style="color: var(--success-color)"></i></h2>
    <h2 v-else>Submission failed <i class="fa-solid fa-circle-xmark" style="color: var(--failure-color);"></i></h2>
    <h3>Score: {{scoreToPercentage(submission.score)}}</h3>
    <p v-html="submission.notes.replace('\n', '<br />')"></p>
    <div class="rubric-item-summaries">
      <p v-if="submission.rubric.passoffTests">Functionality: {{ roundTwoDecimals(submission.rubric.passoffTests.results.score) }} / {{ submission.rubric.passoffTests.results.possiblePoints }}</p>
      <p v-if="submission.rubric.quality">Code Quality: {{ roundTwoDecimals(submission.rubric.quality.results.score) }} / {{ submission.rubric.quality.results.possiblePoints }}</p>
      <p v-if="submission.rubric.unitTests">Unit Tests: {{ roundTwoDecimals(submission.rubric.unitTests.results.score) }} / {{ submission.rubric.unitTests.results.possiblePoints }}</p>
    </div>
    <button class="secondary">See submission details</button>
  </div>
</template>

<style scoped>
.rubric-item-summaries {
  font-size: 20px;
}

.results-preview-container {
  height: 100%;
  flex-direction: column;
  display: flex;
  align-items: center;
}
</style>
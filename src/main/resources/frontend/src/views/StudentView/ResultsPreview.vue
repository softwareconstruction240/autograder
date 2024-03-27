<script setup lang="ts">
import type { Submission } from '@/types/types'
import { scoreToPercentage, roundTwoDecimals } from '@/utils/utils'
import PopUp from '@/components/PopUp.vue'
import SubmissionInfo from '@/views/StudentView/SubmissionInfo.vue'
import { ref } from 'vue'

defineProps<{
  submission: Submission;
}>();

const openDetails = ref<boolean>(false);
</script>

<template>
  <div class="results-preview-container">
    <div id="submission-score">
      <h2 v-if="false">
        <i class="fa-solid fa-triangle-exclamation" style="color: var(--failure-color)"/> <br/>
        <b>Submission needs approval before grading, go see a TA</b>
      </h2>
      <h2 v-else-if="submission.passed">Submission passed! <i class="fa-solid fa-circle-check" style="color: var(--success-color)"/></h2>
      <h2 v-else>Submission failed <i class="fa-solid fa-circle-xmark" style="color: var(--failure-color);"/></h2>
      <h3>Score: {{scoreToPercentage(submission.score)}}</h3>
    </div>
    <p class="submission-notes" v-html="submission.notes.replace('\n', '<br />')"></p>
    <div class="rubric-item-summaries">
      <p v-if="submission.rubric.passoffTests">Functionality: {{ roundTwoDecimals(submission.rubric.passoffTests.results.score) }} / {{ submission.rubric.passoffTests.results.possiblePoints }}</p>
      <p v-if="submission.rubric.quality">Code Quality: {{ roundTwoDecimals(submission.rubric.quality.results.score) }} / {{ submission.rubric.quality.results.possiblePoints }}</p>
      <p v-if="submission.rubric.unitTests">Unit Tests: {{ roundTwoDecimals(submission.rubric.unitTests.results.score) }} / {{ submission.rubric.unitTests.results.possiblePoints }}</p>
    </div>
    <button @click="() => {openDetails = true; console.log(submission)}" class="secondary">See submission details</button>
  </div>

  <PopUp
    v-if="openDetails"
    @closePopUp="openDetails = false">
    <SubmissionInfo :submission="submission"/>
  </PopUp>
</template>

<style scoped>
.rubric-item-summaries {
  font-size: 19px;
  margin: 10px 0;
  font-weight: 600;
}

#submission-score {
  margin-bottom: 10px;
}

.submission-notes {
  filter: brightness(0.8);
}

.results-preview-container {
  height: 100%;
  flex-direction: column;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
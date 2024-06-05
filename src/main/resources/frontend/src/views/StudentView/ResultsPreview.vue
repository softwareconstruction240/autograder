<script setup lang="ts">
import type { Submission } from '@/types/types'
import {scoreToPercentage, roundTwoDecimals, commitVerificationFailed, sortedItems} from '@/utils/utils'
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

      <div v-if="!submission.passed"> <!-- IF SUBMISSION FAILED -->
        <h2>Submission failed <i class="fa-solid fa-circle-xmark" style="color: red"/></h2>
        <h3>You received no points for this submission</h3>
      </div>

      <div v-else-if="commitVerificationFailed(submission)"> <!-- IF SUBMISSION IS BLOCKED -->
        <h2><i class="fa-solid fa-triangle-exclamation" style="color: red"/> Submission blocked! <i class="fa-solid fa-triangle-exclamation" style="color: red"/></h2>
        <h3>You can not receive points on this phase until you talk to a TA</h3>
      </div>

      <div v-else>
        <h2>Submission passed! <i class="fa-solid fa-circle-check" style="color: green"/></h2>
        <h3>Score: {{scoreToPercentage(submission.score)}}</h3>
      </div>

    </div>
    <p class="submission-notes" v-html="submission.notes.replace('\n', '<br />')"></p>
    <div class="rubric-item-summaries">
      <p v-if="submission.rubric.items"
         v-for="item in sortedItems(submission.rubric.items)">
        {{item.category}}: {{roundTwoDecimals(item.results.score)}} / {{item.results.possiblePoints}}
      </p>

      <!--TODO: Remove the following three deprecated elements after Spring 2024 -->
      <p v-if="submission.rubric.passoffTests">Functionality: {{ roundTwoDecimals(submission.rubric.passoffTests.results.score) }} / {{ submission.rubric.passoffTests.results.possiblePoints }}</p>
      <p v-if="submission.rubric.quality">Code Quality: {{ roundTwoDecimals(submission.rubric.quality.results.score) }} / {{ submission.rubric.quality.results.possiblePoints }}</p>
      <p v-if="submission.rubric.unitTests">Unit Tests: {{ roundTwoDecimals(submission.rubric.unitTests.results.score) }} / {{ submission.rubric.unitTests.results.possiblePoints }}</p>
    </div>
    <button @click="() => {openDetails = true}" class="secondary">See submission details</button>
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
  text-align: center;
  text-wrap: balance;
}
</style>
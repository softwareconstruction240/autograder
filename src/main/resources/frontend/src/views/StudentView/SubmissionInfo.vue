<script setup lang="ts">
import type {Submission} from "@/types/types";
import 'ag-grid-community/styles/ag-grid.css';
import "ag-grid-community/styles/ag-theme-quartz.css";
import {
  commitVerificationFailed,
  generateClickableCommitLink,
  generateClickableLink,
  nameOnSubmission, phaseString,
  readableTimestamp,
  scoreToPercentage, sortedItems
} from '@/utils/utils'
import RubricItemView from '@/views/StudentView/RubricItemView.vue'
import InfoPanel from '@/components/InfoPanel.vue'

const { submission } = defineProps<{
  submission: Submission;
}>();

</script>

<template>
  <div class="container">
    <h2>{{phaseString(submission.phase)}}</h2>
    <h3>{{nameOnSubmission(submission)}} ({{submission.netId}})</h3>
    <p>{{readableTimestamp(submission.timestamp)}}</p>
    <p v-html="generateClickableLink(submission.repoUrl)"/>
    <p>commit: <span v-html="generateClickableCommitLink(submission.repoUrl, submission.headHash)"/></p>
    <p>status:
      <span v-if="!submission.passed">failed <i class="fa-solid fa-circle-xmark" style="color: red"/></span>
      <span v-else-if="commitVerificationFailed(submission)"><i class="fa-solid fa-triangle-exclamation" style="color: red"/> <b>needs approval, go see a TA</b> <i class="fa-solid fa-triangle-exclamation" style="color: red"/></span>
      <span v-else>passed <i class="fa-solid fa-circle-check" style="color: green"/></span>
    </p>

    <div id="important">
      <InfoPanel class="info-box">
        <p>Score:</p>
        <h1 v-if="!submission.passed">Failed</h1>
        <h1 v-else-if="commitVerificationFailed(submission)">Score withheld for commits<br>Raw Score: {{scoreToPercentage(submission.score)}}</h1>
        <h1 v-else v-html="scoreToPercentage(submission.score)"/>
      </InfoPanel>
      <InfoPanel id="notesBox" class="info-box">
        <p>Notes:</p>
        <p v-html="submission.notes.replace('\n', '<br />')"/>
      </InfoPanel>
    </div>
  </div>
  <div class="container">
    <RubricItemView v-if="submission.rubric.items" v-for="item in sortedItems(submission.rubric.items)" :rubric-item="item"/>

    <!-- TODO: Remove the following three deprecated views after Spring 2024 -->
    <RubricItemView
      v-if="submission.rubric.passoffTests"
      :rubric-item="submission.rubric.passoffTests"
    />
    <RubricItemView
      v-if="submission.rubric.quality"
      :rubric-item="submission.rubric.quality"
    />
    <RubricItemView
      v-if="submission.rubric.unitTests"
      :rubric-item="submission.rubric.unitTests"
    />
  </div>



<!--  .info-box {-->
<!--  background-color: var(&#45;&#45;opposite);-->
<!--  display: flex;-->
<!--  flex-direction: column;-->
<!--  align-items: center;-->
<!--  justify-content: center;-->
<!--  color: var(&#45;&#45;opposite-text-color);-->
<!--  margin: 10px;-->
<!--  text-align: center;-->
<!--  padding: 5px 15px;-->
<!--  border-radius: 5px;-->
<!--  }-->

</template>

<style scoped>
.container {
  flex-direction: column;
  text-align: left;
  max-width: 600px;
}

.container p {
  padding: 1px 0;
}

#important {
  display: flex;
  flex-direction: row;
}

#notesBox {
  width: 100%;
}
</style>
<script setup lang="ts">
import type {Phase, Submission} from "@/types/types";
import {useSubmissionStore} from "@/stores/submissions";
import {readableTimestamp} from "@/utils/utils";

defineEmits<{
  'show-results': [submission: Submission]
}>();

const props = defineProps<{
  phase: Phase
}>();

useSubmissionStore().getSubmissions(props.phase);

const isPassFail = props.phase !== '6';

const passFail = (submission: Submission) => {
  return submission.score === 100 ? 'Pass' : 'Fail';
}
</script>

<template>
  <ul
      id="past-submissions"
      v-if="useSubmissionStore().submissionsByPhase[props.phase].length > 0">
    <li
        v-for="submission in useSubmissionStore().submissionsByPhase[props.phase]"
        :key="`${submission.headHash}-${submission.timestamp}`"
        @click="$emit('show-results', submission)">
      {{ readableTimestamp(new Date(submission.timestamp)) }} -
      {{ submission.score }}% {{ isPassFail ? `(${passFail(submission)})` : ''}}
    </li>
  </ul>
  <p v-else>No previous results to show</p>
</template>

<style scoped>
#past-submissions {
  list-style: none;
  padding-left: 0;

}

#past-submissions li {
  cursor: pointer;
}

#past-submissions li::before {
  content: "— ";

  margin-left: 1rem;
}
</style>
<script setup lang="ts">
import type {Phase, Submission} from "@/types/types";
import {useSubmissionStore} from "@/stores/submissions";
import {readableTimestamp} from "@/utils/utils";
import {computed} from "vue";

defineEmits<{
  'show-results': [submission: Submission]
}>();

const props = defineProps<{
  phase: Phase
}>();

useSubmissionStore().getSubmissions(props.phase);

const isPassFail = props.phase !== '6';

const passFail = (submission: Submission) => {
  return submission.score === 1 ? 'Pass' : 'Fail';
}

const submissionsByPhaseDesc = computed(() => {
  const submissions = useSubmissionStore().submissionsByPhase[props.phase];
  if (!submissions) return [];
  // @ts-ignore
  return submissions.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
});
</script>

<template>
  <ul
      id="past-submissions"
      v-if="useSubmissionStore().submissionsByPhase[phase]?.length > 0">
    <li
        v-for="submission in submissionsByPhaseDesc"
        :key="`${submission.headHash}-${submission.timestamp}`"
        @click="$emit('show-results', submission)">
      {{ readableTimestamp(new Date(submission.timestamp)) }} -
      {{ (submission.score * 100).toPrecision(4) }}% {{ isPassFail ? `(${passFail(submission)})` : ''}}
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
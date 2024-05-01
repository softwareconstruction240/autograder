<script setup lang="ts">

import {onMounted, ref} from "vue";
import type {Submission, TestResult} from "@/types/types";
import {subscribeToGradingUpdates} from "@/stores/submissions";
import ResultsPopup from "@/views/PhaseView/ResultsPopup.vue";

const emit = defineEmits<{
  "show-results": [submission: Submission];
}>();

const status = ref<string>("");
const errorDetails = ref<string>("");
const errorAnalysis = ref<TestResult | undefined>(undefined);
const displayError = ref<boolean>(false);

onMounted(() => {
  subscribeToGradingUpdates((event: MessageEvent) => {
    const messageData = JSON.parse(event.data);

    switch (messageData.type) {
      case 'queueStatus':
        status.value = `You are currently #${messageData.position} in line`;
        return;
      case 'started':
        status.value = `Autograding has started`;
        return;
      case 'update':
        status.value =  messageData.message;
        return;
      case 'results':
        status.value = `Finished!`;
        emit("show-results", JSON.parse(messageData.results));
        return;
      case 'error':
        status.value = `Error: ${messageData.message}`;
        errorDetails.value = messageData.details;
        errorAnalysis.value = messageData.analysis;
        return;
    }
  });
});

</script>

<template>
<div class="container">
  <span id="status">{{ status }}</span>
  <div v-if="errorDetails || errorAnalysis"
       class="selectable">
    <button @click="() => {displayError = true;}">Click here</button>
  </div>
  <ResultsPopup
      v-if="displayError"
      :text-results="errorDetails"
      :test-results="errorAnalysis"
      @closePopUp="() => displayError = false"
    />
</div>
</template>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
}

#status {
  font-size: 1.5rem;
  font-weight: bold;
}
</style>
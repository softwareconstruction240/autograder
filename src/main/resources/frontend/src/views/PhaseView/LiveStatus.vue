<script setup lang="ts">

import {onMounted, ref} from "vue";
import type {Submission} from "@/types/types";
import {subscribeToGradingUpdates} from "@/stores/submissions";

const emit = defineEmits<{
  "show-results": [submission: Submission];
}>();

const status = ref<string>("");

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
        return;
    }
  });
});

</script>

<template>
<div class="container">
  <span id="status">{{ status }}</span>
</div>
</template>

<style scoped>
.container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

#status {
  font-size: 1.5rem;
  font-weight: bold;
}
</style>
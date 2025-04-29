<script setup lang="ts">
import { computed } from "vue";
import { useConfigStore } from "@/stores/config";
import { readableTimestamp } from "@/utils/utils";

const showWarning = computed(() => {
  const shutdownDate = new Date(useConfigStore().public.shutdown.timestamp);
  const now = new Date();
  const warningWindow = new Date(
    now.getTime() + useConfigStore().public.shutdown.warningMilliseconds,
  );
  return shutdownDate <= warningWindow;
});
</script>

<template>
  <div v-if="showWarning" id="warningBox">
    <div id="warningHeader">
      <i class="fa-solid fa-circle-exclamation" />
      <p>WARNING</p>
      <i class="fa-solid fa-circle-exclamation" />
    </div>
    <p>
      Per University Policy, no work may be submitted after the last day of classes. As such, the CS
      240 Autograder will stop accepting submissions on all graded phases on
      {{ readableTimestamp(useConfigStore().public.shutdown.timestamp) }} (Utah Time)
    </p>
  </div>
</template>

<style scoped>
* {
  color: white;
}

#warningBox {
  width: 100%;
  background-color: red;
  border-radius: 5px;
  padding: 10px;
  justify-content: center;
  text-align: center;
}

#warningHeader {
  display: flex;
  flex-direction: row;
  font-size: 24px;
  justify-content: center;
  align-items: center;
}
</style>

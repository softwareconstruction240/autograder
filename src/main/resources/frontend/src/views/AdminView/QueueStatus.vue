<script lang="ts" setup>
import {onMounted, onUnmounted, ref} from "vue";
import {getQueueStatus} from "@/services/adminService";
import { logoutPost } from '@/services/authService'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { reRunSubmissionsPost } from '@/services/submissionService'

const currentlyGrading = ref<string[]>([]);
const inQueue = ref<string[]>([]);

const getQueueStatusPoll = async () => {
  const queueStatus = await getQueueStatus();
  currentlyGrading.value = queueStatus.currentlyGrading;
  inQueue.value = queueStatus.inQueue;
};

let intervalId: number;
onMounted(async () => {
  await getQueueStatusPoll();
  intervalId = setInterval(getQueueStatusPoll, 5000);
});

onUnmounted(() => {
  clearInterval(intervalId);
});

const reRunQueue = async () => {
  await reRunSubmissionsPost()
}
</script>

<template>
  <div class="container">
    <div>
      <h3>Currently Grading</h3>
      <ul v-if="currentlyGrading.length > 0">
        <li v-for="submission in currentlyGrading" :key="submission">{{ submission }}</li>
      </ul>
      <p v-else>No submissions are being graded</p>
    </div>
    <div>
      <h3>In Queue</h3>
      <ol v-if="inQueue.length > 0">
        <li v-for="submission in inQueue" :key="submission">{{ submission }}</li>
      </ol>
      <p v-else>No submissions in queue</p>
    </div>
  </div>
  <div>
    <button @click="reRunQueue">Rerun Submissions In Queue</button>
  </div>
</template>

<style scoped>
.container {
  padding: 10px;
}
</style>
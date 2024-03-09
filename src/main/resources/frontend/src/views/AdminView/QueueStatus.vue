<script lang="ts" setup>
import {onMounted, onUnmounted, ref} from "vue";
import {getQueueStatus} from "@/services/adminService";
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

let reRunInProgress = false;
const reRunQueue = async () => {
  reRunInProgress = await reRunSubmissionsPost()
}
</script>

<template>
  <div class="container">
    <div class="grading-queue">
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
      <p v-if="reRunInProgress">The queue has been refreshed and all submissions previously stuck in the queue are running through the grader again</p>
    </div>
  </div>
</template>

<style scoped>
.container {
  padding: 10px;
  display: grid;
  grid-template-columns: 2fr 1fr;
  align-items: center;
}

.grading-queue {
margin: 10px;
padding: 10px;
border: 1px solid #ccc;
border-radius: 5px;
background-color: #f2f2f2;
cursor: pointer;
}
</style>
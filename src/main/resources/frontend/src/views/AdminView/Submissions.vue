<script setup lang="ts">
import {onMounted, ref} from "vue";
import type {Submission} from "@/types/types";
import {submissionsLatestGet} from "@/services/adminService";
import {readableTimestamp} from "@/utils/utils";
import {useAdminStore} from "@/stores/admin";

const latestSubmissions = ref<Submission[]>([])

onMounted(async () => {
  const latestData = await submissionsLatestGet();
  latestData.sort((a, b) => b.timestamp.localeCompare(a.timestamp)) // Sort by timestamp descending
  latestSubmissions.value = latestData;
})

const getNameFromSubmission = (submission: Submission) => {
  const user = useAdminStore().usersByNetId[submission.netId];
  return `${user.firstName} ${user.lastName}`
}

</script>

<template>
  <table>
    <thead>
    <tr>
      <th>Name</th>
      <th>NetId</th>
      <th>Phase</th>
      <th>Timestamp</th>
      <th>Score</th>
      <th>Notes</th>
    </tr>
    </thead>
    <tbody>
    <tr v-for="submission in latestSubmissions" :key="`${submission.netId}-${submission.phase}`">
      <td>{{ getNameFromSubmission(submission) }}</td>
      <td>{{ submission.netId }}</td>
      <td>{{ submission.phase }}</td>
      <td>{{ readableTimestamp(new Date(submission.timestamp)) }}</td>
      <td>{{ submission.score * 100 }}%</td>
      <td>{{ submission.notes }}</td>
    </tr>
    </tbody>
  </table>

</template>

<style scoped>
table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 0.25rem;
  border: 1px solid #ccc;
}

tr:nth-child(even) {
  background-color: #eee;
}

tr:nth-child(odd) {
  background-color: #fff;
}

th {
  background-color: #333;
  color: #fff;
}
</style>
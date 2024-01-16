<script setup lang="ts">
import {onMounted, ref} from "vue";
import type {Submission} from "@/types/types";
import {submissionsLatestGet} from "@/services/adminService";
import {readableTimestamp} from "@/utils/utils";

const latestSubmissions = ref<Submission[]>([])

onMounted(async () => {
  latestSubmissions.value = await submissionsLatestGet();
})
</script>

<template>
  <table>
    <thead>
    <tr>
      <th>NetId</th>
      <th>Phase</th>
      <th>Timestamp</th>
      <th>Score</th>
      <th>Notes</th>
    </tr>
    </thead>
    <tbody>
    <tr v-for="submission in latestSubmissions" :key="`${submission.netId}-${submission.phase}`">
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
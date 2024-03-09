<script setup lang="ts">
import type {User} from "@/types/types";

defineProps<{
  student: User;
}>();

</script>

<template>
  <h4>Student Info</h4>
  <h3>{{student.firstName}} {{student.lastName}}</h3>
  <p class="netID">netID: {{student.netId}}</p>
  <p class="repo">Github Repo: <a href="{{student.repoUrl}}">{{student.repoUrl}}</a> </p>
</template>

<style scoped>

</style>
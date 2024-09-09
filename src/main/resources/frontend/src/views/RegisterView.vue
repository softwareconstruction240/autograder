<script setup lang="ts">
import {useAppConfigStore} from "@/stores/appConfig";
import {onBeforeMount, onMounted} from 'vue';
import {meGet} from "@/services/authService";
import {useAuthStore} from "@/stores/auth";
import router from "@/router";

onBeforeMount(async () => {
  const loggedInUser = await meGet()
  if (loggedInUser == null)
    return;

  useAuthStore().user = loggedInUser;
  router.push({ name: 'home' });
})


</script>

<template>
  <div id="content">
    <h2>Welcome to the CS 240 Autograder</h2>
    <p>Please enter your GitHub Repo URL</p>
    <button @click="">Submit and Verify</button>
  </div>
</template>

<style scoped>
#content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

button {
  margin-top: 1rem;
}
</style>
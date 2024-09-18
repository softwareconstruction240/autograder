<script setup lang="ts">
import { onBeforeMount } from 'vue'
import {meGet} from "@/services/authService";
import {useAuthStore} from "@/stores/auth";
import router from "@/router";
import { uiConfig } from '@/stores/uiConfig'
import { Phase } from '@/types/types'
import RepoEditor from '@/components/RepoEditor.vue'

onBeforeMount(async () => {
  const loggedInUser = await meGet()
  if (loggedInUser == null)
    return;

  useAuthStore().user = loggedInUser;
  router.push({ name: 'home' });
})

const goToApp = () => {
  window.location.href = '/'
}
</script>

<template>
  <div id="content">
    <h2>Welcome to the CS 240 Autograder</h2>
    <p>Your code for this semester will need to be stored in a Public GitHub Repository, in order for the Autograder to work.</p>
    <a
      target="_blank"
      :href="uiConfig.getSpecLink(Phase.GitHub)">
      <span>Click here for more info</span>
    </a>
    <RepoEditor
      @repoEditSuccess="goToApp"
      :user="useAuthStore().user"/>
  </div>
</template>

<style scoped>
#content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
}

button {
  margin-top: 1rem;
}
</style>
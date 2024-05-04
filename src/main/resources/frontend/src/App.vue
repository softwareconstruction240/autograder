<script setup lang="ts">

import { computed, onMounted, ref } from 'vue'
import {useAuthStore} from "@/stores/auth";
import { logoutPost } from '@/services/authService'
import router from '@/router'
import '@/assets/fontawesome/css/fontawesome.css'
import '@/assets/fontawesome/css/solid.css'
import { getConfig } from '@/services/configService'

const greeting = computed(() => {
  if (useAuthStore().isLoggedIn) {
    return `${useAuthStore().user?.firstName} ${useAuthStore().user?.lastName} - ${useAuthStore().user?.netId} (${useAuthStore().user?.role.toLowerCase()}) - `
  }
});

const logOut = async () => {
  try {
    await logoutPost()
    useAuthStore().user = null
  } catch (e) {
    alert(e)
  }
  router.push({name: "login"})
}

const bannerMessage = ref<string>()
// TODO: Build central store for config, so that you don't have to have it in each component
const loadConfig = async () => {
  bannerMessage.value = (await getConfig()).bannerMessage
}
onMounted( () => {
  loadConfig()
})
</script>

<template>
  <header>
    <h1>CS 240 Autograder</h1>
    <h3>This is where you can submit your assignments and view your scores.</h3>
    <p>{{ greeting }} <a v-if="useAuthStore().isLoggedIn" @click="logOut">Logout</a></p>
    <p>{{ useAuthStore().user?.repoUrl }}</p>
    <div v-if="bannerMessage" id="bannerMessage">
      <span v-text="bannerMessage"/>
    </div>
  </header>
  <main>
    <router-view/>
  </main>
</template>

<style scoped>
#bannerMessage {
  width: 100%;
  background-color: #4fa0ff;
  border-radius: 3px;
  padding: 7px;
  margin-top: 15px;
}
header {
  text-align: center;

  margin-bottom: 20px;

  padding: 20px;

  width: 100%;

  background-color: var(--color--secondary--background);
  color: var(--color--secondary--text);
}

h1 {
  font-weight: bold;
}

main {
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: var(--color--surface--background);
  color: var(--color--surface--text);
  padding: 20px;
  border-radius: 3px;

  width: 60vw;

  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.2);
}

a {
  color: white;
  text-decoration: underline;
  cursor: pointer;
}

@media only screen and (max-width: 600px) {
  main {
    width: 95%;
    max-width: none;
    margin: 0 0 20px;
  }
}

@media only screen and (min-width: 601px) and (max-width: 900px){
  main {
    width: 75%;
    max-width: none;
    margin: 0 0 20px;
  }
}
</style>

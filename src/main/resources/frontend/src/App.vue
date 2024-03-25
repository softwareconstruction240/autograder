<script setup lang="ts">

import { computed } from 'vue'
import {useAuthStore} from "@/stores/auth";
import { logoutPost } from '@/services/authService'
import router from '@/router'
import '@/assets/fontawesome/css/fontawesome.css'
import '@/assets/fontawesome/css/solid.css'
import { generateClickableLink } from './utils/utils'

const identity = computed(() => {
  if (useAuthStore().isLoggedIn) {
    return `${useAuthStore().user?.firstName} ${useAuthStore().user?.lastName} - ${useAuthStore().user?.netId}`
  }
});

const user = computed( () => {
  return useAuthStore().user
})

const logOut = async () => {
  try {
    await logoutPost()
    useAuthStore().user = null
  } catch (e) {
    alert(e)
  }
  router.push({name: "login"})
}
</script>

<template>
  <header>
    <h1 id="class_number">CS 240</h1>
    <h2 id="autograder-text">AUTOGRADER</h2>
    <p>The automatic code checker and grader for BYU's Advanced Software Construction Class</p>
  </header>
  <main>
    <router-view/>
  </main>
  <footer>
    <div class="footer" v-if="user">
      <div id="userInfo">
        <p>{{identity}}</p>
        <span v-html="generateClickableLink(user.repoUrl)"/>
      </div>
      <div id="actions">
        <button @click="logOut">Logout</button>
      </div>
    </div>
    <div class="footer" v-else>
      Idk, maybe something down here
    </div>
  </footer>


<!--  <header>-->
<!--    <h1>CS 240 Autograder</h1>-->
<!--    <h3>This is where you can submit your assignments and view your scores.</h3>-->
<!--    <p>{{ greeting }} <a v-if="useAuthStore().isLoggedIn" @click="logOut">Logout</a></p>-->
<!--    <p>{{ useAuthStore().user?.repoUrl }}</p>-->
<!--  </header>-->
<!--  <main>-->
<!--    <router-view/>-->
<!--  </main>-->
</template>

<style scoped>
main {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}
footer {
  position: fixed;
  bottom: 0;
  width: 100%;
  background-color: #36373F;
  padding: 15px 25px;
  overflow-x: hidden;
}
.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
#userInfo {
  overflow: hidden; /* Hide content that overflows */
  white-space: nowrap;
  margin-right: 10px;
}
#class_number {
  font-size: 75px;
  padding-bottom: 0;
  margin-bottom: -20px;
}
#autograder-text {
  font-size: 43px;
  text-align: center;
  padding-bottom: 1rem;
}
header {
  width: 100%;
  font-weight: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  font-family: Monaco,sans-serif;
  margin: 1rem
}

/*
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
  background-color: var(--color--surface--background);
  color: var(--color--surface--text);
  padding: 20px;
  border-radius: 3px;

  width: 66vw;

  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.2);
}

a {
  color: white;
  text-decoration: underline;
  cursor: pointer;
} */
</style>

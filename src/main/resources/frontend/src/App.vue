<script setup lang="ts">

import { computed, onMounted, ref } from 'vue'
import {useAuthStore} from "@/stores/auth";
import { logoutPost } from '@/services/authService'
import router from '@/router'
import '@/assets/fontawesome/css/fontawesome.css'
import '@/assets/fontawesome/css/solid.css'
import { generateClickableLink } from './utils/utils'
import { uiConfig } from './stores/uiConfig'
import PopUp from '@/components/PopUp.vue'

const openMenu = ref<boolean>(false)

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

/* LIGHT/DARK MODE MANAGEMENT */
onMounted( () => {
  lightMode.value = localStorage.getItem('isLightMode') == "true"
  if (lightMode.value) {
    document.body.classList.add("light-mode")
  }
})
const lightMode = ref<boolean>(false)
const toggleLightMode = () => {
  document.body.classList.toggle("light-mode")
  lightMode.value = !lightMode.value
  localStorage.setItem('isLightMode', lightMode.value ? "true" : "false")
}
/* END LIGHT/DARK MODE MANAGEMENT */
</script>

<template>
  <header>
    <h1 id="class_number">CS 240</h1>
    <h2 id="autograder-text">AUTOGRADER</h2>
    <p id="program-description">The automatic code checker and grader for BYU's Advanced Software Construction Class</p>
  </header>

  <main>
    <router-view/>
  </main>

  <div class="footer-spacer"/>
  <footer>
    <div class="footer" v-if="user">
      <div id="userInfo">
        <p style="font-weight: bold">{{identity}}</p>
        <span v-html="generateClickableLink(user.repoUrl)"/>
      </div>

      <!-- This actions div appears when the window is wide enough  -->
      <div id="actions">
        <a target="_blank" :href="uiConfig.links.helpQueue"><button><i class="fa-solid fa-handshake-angle"/></button></a>
        <a target="_blank" :href="uiConfig.links.canvas"><button><i class="fa-solid fa-graduation-cap"/></button></a>
        <button @click="toggleLightMode">
          <i v-if="lightMode" class="fa-solid fa-moon"/><i v-else class="fa-solid fa-sun"/>
        </button>
        <button class="secondary" @click="logOut">Logout <i class="fa-solid fa-right-from-bracket"/></button>
      </div>

      <!--  This div is what appears when the window is too narrow    -->
      <div id="menu">
        <button class="secondary" @click="openMenu = true">Menu <i class="fa-solid fa-bars"/></button>

        <PopUp
          v-if="openMenu"
          @closePopUp="openMenu = false"
          id="menuPopUp">
          <h2>Menu</h2>
          <a target="_blank" :href="uiConfig.links.helpQueue"><button>Help Queue <i class="fa-solid fa-handshake-angle"/></button></a>
          <a target="_blank" :href="uiConfig.links.canvas"><button>Canvas <i class="fa-solid fa-graduation-cap"/></button></a>
          <button @click="toggleLightMode">
            Toggle Light/Dark
            <i v-if="lightMode" class="fa-solid fa-moon"/><i v-else class="fa-solid fa-sun"/>
          </button>
          <button class="secondary" @click="logOut">Logout <i class="fa-solid fa-right-from-bracket"/></button>
        </PopUp>
      </div>

    </div>
    <div class="footer" v-else>
      Idk, maybe something down here
    </div>
  </footer>
</template>

<style scoped>
main {
  width: 100%;
  max-width: 700px;
  padding: 0 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
footer {
  position: fixed;
  bottom: 0;
  width: 100%;
  background-color: var(--plain-200);
  overflow-x: hidden;
  z-index: 10;
}
.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 25px;
}
.footer-spacer {
  height: 100px;
}
#userInfo {
  overflow: hidden; /* Hide content that overflows */
  white-space: nowrap;
  margin-right: 10px;
  text-align: left;
}
:is(#actions, #menu) button {
  margin: 5px;
}
#class_number {
  font-size: 75px;
  padding-bottom: 0;
  margin: 10px 0 -10px;

  line-height: 1;
}
#autograder-text {
  font-size: 44px;
  text-align: center;
  padding: 0;
  margin: 0;
}
#program-description {
  max-width: 500px;
  padding: 10px 0 30px;
}
header {
  width: 100%;
  font-weight: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  font-family: Monaco,sans-serif;
}
#menuPopUp {
  display: flex;
  flex-direction: column;
}
#menuPopUp button {
  width: 70%;
  margin: 10px;
}

@media screen and (max-width: 700px) {
  #actions {
    display: none;
  }
  button {
    width: 110px;
  }
}

@media screen and (min-width: 701px) {
  #actions {
    display: flex;
  }
  #menu {
    display: none;
  }
}
</style>

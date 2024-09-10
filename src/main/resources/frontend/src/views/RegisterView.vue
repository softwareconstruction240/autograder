<script setup lang="ts">
import { onBeforeMount, reactive } from 'vue'
import {meGet} from "@/services/authService";
import {useAuthStore} from "@/stores/auth";
import router from "@/router";
import { isPlausibleRepoUrl } from '@/utils/utils'
import { uiConfig } from '@/stores/uiConfig'
import { Phase } from '@/types/types'
import { studentUpdateRepo } from '@/services/userService'

onBeforeMount(async () => {
  const loggedInUser = await meGet()
  if (loggedInUser == null)
    return;

  useAuthStore().user = loggedInUser;
  router.push({ name: 'home' });
})

let studentRepo = reactive( {
  value: ""
})
let waitingForRepoCheck = reactive({ value: false })

const submitAndCheckRepo = async () => {
  waitingForRepoCheck.value = true

  try {
    await studentUpdateRepo(studentRepo.value)

  } catch (error) {
    if (error instanceof Error) { alert("Failed to save your Github Repo: " + error.message) }
    else { alert("Unknown error updating Github Repo") }
    waitingForRepoCheck.value = false
    return
  }

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
    <p><em>Please enter your GitHub Repo link here:</em></p>
    <input v-model="studentRepo.value" type="text" id="repoUrlInput" placeholder="Github Repo URL"/>
    <button
      :disabled="waitingForRepoCheck.value || !isPlausibleRepoUrl(studentRepo.value)"
      class="primary"
      @click="submitAndCheckRepo">Submit and Register</button>
    <div id="urlTips">
      <p>Your url should look something like this:</p>
      <p><em>https://github.com/{username}/{name_of_project}</em></p>
    </div>
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
#repoUrlInput {
  width: 80%;
  padding: 10px;
  margin-right: 10px;
}
#urlTips {
  font-size: smaller;
  flex-direction: column;
}
</style>
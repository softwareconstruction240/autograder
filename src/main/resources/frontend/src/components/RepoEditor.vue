<script setup lang="ts">
import { isPlausibleRepoUrl } from '@/utils/utils'
import { defineEmits, reactive } from 'vue'
import { studentUpdateRepo } from '@/services/userService'

defineEmits({
  repoEditSuccess: null,
});

let studentRepo = reactive( {
  value: ""
})
let waitingForRepoCheck = reactive({ value: false })

const submitAndCheckRepo = async (sendEmit: (event: any) => void) => {
  waitingForRepoCheck.value = true

  try {
    await studentUpdateRepo(studentRepo.value)

  } catch (error) {
    if (error instanceof Error) { alert("Failed to save your Github Repo: " + error.message) }
    else { alert("Unknown error updating Github Repo") }
    waitingForRepoCheck.value = false
    return
  }

  sendEmit('repoEditSuccess')
}
</script>

<template>
<div>
  <p><em>Please enter your GitHub Repo link here:</em></p>
  <input v-model="studentRepo.value" type="text" id="repoUrlInput" placeholder="Github Repo URL"/>
  <button
    :disabled="waitingForRepoCheck.value || !isPlausibleRepoUrl(studentRepo.value)"
    class="primary"
    @click="submitAndCheckRepo($emit)">Submit and Save</button>
  <p v-if="waitingForRepoCheck.value">Verifying repo URL... please wait...</p>
  <div id="urlTips">
    <p>Your url should look something like this:</p>
    <p><em>https://github.com/{username}/{name_of_project}</em></p>
  </div>
</div>
</template>

<style scoped>
#repoUrlInput {
  width: 80%;
  padding: 10px;
  margin-right: 10px;
}
#urlTips {
  font-size: smaller;
  flex-direction: column;
}
button {
  margin: 5px;
}
</style>
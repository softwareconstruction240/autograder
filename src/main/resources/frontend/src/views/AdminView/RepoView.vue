<script setup lang="ts">
import type { User } from '@/types/types'
import { generateClickableLink } from '@/utils/utils'
import RepoEditor from '@/components/RepoEditor.vue'
import { useAuthStore } from '@/stores/auth'
import { loadUser } from '@/services/authService'

const { student } = defineProps<{
  student: User;
}>();

</script>

<template>
<div>
  <h3>GitHub Repo URL for {{student.firstName}} {{student.lastName}}</h3>
  <p>Current Repo: <span v-html="generateClickableLink(student.repoUrl)"/></p>

  <br/>

  <div id="changeAndHistory">
    <div>
      <h4>Change Their Repo</h4>
      <RepoEditor
        :user="student"
         @repoEditSuccess=""/>
    </div>
    <div>
      <h4>Repo Change History</h4>
      <p><em>Coming soon to an autograder near you</em></p>
    </div>
  </div>
</div>
</template>

<style scoped>
#changeAndHistory {
  display: grid;
  grid-template-columns: 2fr 3fr;
  text-align: center;
}
</style>
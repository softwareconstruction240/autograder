<script setup lang="ts">
import { ref } from 'vue'
import Panel from '@/components/Panel.vue'
import { combineDateAndTime } from '@/utils/utils'
import { setGraderShutdown } from '@/services/configService'

const { closeEditor } = defineProps<{
  closeEditor: () => void
}>();

const shutdownDate = ref<string>("")
const shutdownTime = ref<string>("")

const submitShutdown = async () => {
  await setGraderShutdown(combineDateAndTime(shutdownDate.value, shutdownTime.value))
  closeEditor()
}

const cancelShutdown = () => {
  const confirm = window.confirm("Are you sure you want to cancel the already scheduled shutdown?")
  if (confirm) {
    alert("NOT IMPLEMENTED")
    closeEditor()
  }
}
</script>

<template>
  <div style="max-width: 500px">
    <p>University Policy dictates that all graded work must be completed by the last day of classes.
      To enforce that policy, enter the date below that the Autograder should stop accepting graded submissions.</p>
    <input type="date" v-model="shutdownDate"/><input type="time" v-model="shutdownTime"/>
    <p><em>If no time is selected, it will expire at the end of the day (Utah Time)</em></p>

    <button @click="submitShutdown">Submit</button> <button @click="cancelShutdown">Cancel Shutdown</button>

    <Panel>
      <p>Admin submissions will not be affected.</p>
      <p>Any ungraded phases will remain available for students to submit to.</p>
      <p>The Autograder will present a message to students 24 hours before the shutdown warning them of the deadline.</p>
    </Panel>
  </div>
</template>

<style scoped>

</style>

<script setup lang="ts">
import { ref } from 'vue'
import Panel from '@/components/Panel.vue'
import { combineDateAndTime } from '@/utils/utils'
import { setGraderShutdown } from '@/services/configService'
import { useAppConfigStore } from '@/stores/appConfig'

const { closeEditor } = defineProps<{
  closeEditor: () => void
}>()

const shutdownDate = ref<string>('')
const shutdownTime = ref<string>('')
const shutdownWarningHours = ref<number>(24)

const submitShutdown = async () => {
  try {
    await setGraderShutdown(
      combineDateAndTime(shutdownDate.value, shutdownTime.value),
      shutdownWarningHours.value
    )
  } catch (e) {
    alert('There was a problem scheduling the shutdown\n' + e)
  }
  closeEditor()
}

const cancelShutdown = async () => {
  const confirm = window.confirm('Are you sure you want to cancel the already scheduled shutdown?')
  if (confirm) {
    await setGraderShutdown('', 0)
    closeEditor()
  }
}
</script>

<template>
  <div style="max-width: 500px">
    <div class="section">
      <p>
        University Policy dictates that all graded work must be completed by the last day of
        classes. To enforce that policy, enter the date below that the Autograder should stop
        accepting graded submissions.
      </p>
      <input type="date" v-model="shutdownDate" /><input type="time" v-model="shutdownTime" />
      <p><em>If no time is selected, it will expire at the end of the day (Utah Time)</em></p>
    </div>

    <div class="section">
      <p>
        How many hours before the shutdown should the Autograder show a warning to students?
        <em>Default is 24 hours</em>
      </p>
      <input type="number" v-model="shutdownWarningHours" /> <br />
    </div>

    <div class="section">
      <button :disabled="!shutdownDate" @click="submitShutdown">Submit</button>
      <button v-if="useAppConfigStore().shutdownSchedule != 'never'" @click="cancelShutdown">
        Cancel Shutdown
      </button>
    </div>

    <Panel>
      <p>Admin submissions will not be affected.</p>
      <p>
        Any phases that do not receive a grade (like style checker) will remain available for
        students to submit to.
      </p>
      <p>
        The Autograder will present a message to students {{ shutdownWarningHours }} hours before
        the shutdown warning them of the deadline.
      </p>
    </Panel>
  </div>
</template>

<style scoped>
.section {
  margin-bottom: 15px;
}
</style>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAppConfigStore } from '@/stores/appConfig'
import { readableTimestamp } from '@/utils/utils'

const showWarning = ref<boolean>(false)

onMounted(() => {
  const shutdownDate: Date = new Date(useAppConfigStore().shutdownSchedule)
  const now = new Date()
  const twentyFourHoursFromNow = new Date(now.getTime() + 24 * 60 * 60 * 1000)
  showWarning.value = shutdownDate > now && shutdownDate <= twentyFourHoursFromNow
})
</script>

<template>
  <div v-if="showWarning" id="warningBox">
    <div id="warningHeader">
      <i class="fa-solid fa-circle-exclamation"/>
      <p>WARNING</p>
      <i class="fa-solid fa-circle-exclamation"/>
    </div>
    <p>Per University Policy, no work may be submitted after the last day of classes. As such, the
      CS 240 Autograder will stop accepting submissions on all graded phases on
      {{readableTimestamp(useAppConfigStore().shutdownSchedule)}} (Utah Time)</p>
  </div>
</template>

<style scoped>
* {
  color: white;
}

#warningBox {
  width: 100%;
  background-color: red;
  border-radius: 5px;
  padding: 10px;
  justify-content: center;
  text-align: center;
}

#warningHeader {
  display: flex;
  flex-direction: row;
  font-size: 24px;
  justify-content: center;
  align-items: center;
}
</style>

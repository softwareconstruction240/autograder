<script setup lang="ts">
import { useAppConfigStore } from '@/stores/appConfig'
import { ref } from 'vue'
import { setPenalties } from '@/services/configService'

const { closeEditor } = defineProps<{
  closeEditor: () => void
}>();

const appConfig = useAppConfigStore();

const gitPenalty = ref<number>(Math.round(appConfig.gitCommitPenalty * 100))
const latePenalty = ref<number>(Math.round(appConfig.perDayLatePenalty * 100))
const maxLateDays = ref<number>(appConfig.maxLateDaysPenalized)

const submit = async () => {
  try {
    await setPenalties(maxLateDays.value, gitPenalty.value / 100, latePenalty.value / 100)
    closeEditor()
  } catch (e) {
    appConfig.updateConfig()
    alert("There was a problem saving the penalties")
  }
}
</script>

<template>
  <div class="penalty">
    <p class="penaltyName">Git Commit Penalty</p>
    <p class="penaltyDescription">Applied when students don't have enough commits</p>
    <p><input type="number" v-model="gitPenalty"/>%</p>
  </div>

  <div class="penalty">
    <p class="penaltyName">Late Penalty</p>
    <p class="penaltyDescription">Applied per day the submission is late</p>
    <p><input type="number" v-model="latePenalty"/>%</p>
  </div>

  <div class="penalty">
    <p class="penaltyName">Max Late Days</p>
    <p class="penaltyDescription">Days after which the late penalty caps out</p>
    <p><input type="number" v-model="maxLateDays"/> days</p>
  </div>

  <button @click="submit">Submit</button>
</template>

<style scoped>
.penaltyName {
  font-weight: bold;
}
.penaltyDescription {
  font-style: italic;
}
.penalty {
  margin-top: 5px;
}

button {
  margin-top: 15px;
}
</style>

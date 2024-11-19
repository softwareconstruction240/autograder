<script setup lang="ts">
import { listOfPhases, Phase } from '@/types/types'
import { useAppConfigStore } from '@/stores/appConfig'
import { setLivePhases } from '@/services/configService'

const appConfigStore = useAppConfigStore();

const { closeEditor } = defineProps<{
  closeEditor: () => void
}>();

const setAllPhases = (setting: boolean) => {
  for (const phase of listOfPhases() as Phase[]) {
    appConfigStore.phaseActivationList[phase] = setting
  }
}
const submitLivePhases = async () => {
  let livePhases: Phase[] = []
  for (const phase of listOfPhases() as Phase[]) {
    if (useAppConfigStore().phaseActivationList[phase]) {
      livePhases.push(phase);
    }
  }

  try {
    await setLivePhases(livePhases)
  } catch (e) {
    alert("There was a problem in saving live phases")
  }
  closeEditor()
}
</script>

<template>
  <div class="checkboxes">
    <label v-for="(phase, index) in listOfPhases()" :key="index">
      <span><input type="checkbox" v-model="appConfigStore.phaseActivationList[phase]"> {{ phase }}</span>
    </label>
  </div>

  <div class="submitChanges">
    <p><em>This will not effect admin submissions</em></p>
    <div>
      <button @click="setAllPhases(true)" class="small">Enable all</button>
      <button @click="setAllPhases(false)" class="small">Disable all</button>
    </div>
    <button @click="submitLivePhases">Submit Changes</button>
  </div>
</template>

<style scoped>
.checkboxes {
  display: flex;
  flex-direction: column;
}
.submitChanges {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.submitChanges >* {
  margin: 5px;
}
</style>

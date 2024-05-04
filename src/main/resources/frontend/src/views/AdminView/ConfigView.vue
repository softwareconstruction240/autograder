<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { type Config, listOfPhases, Phase } from '@/types/types'
import PopUp from '@/components/PopUp.vue'
import { getConfig, setBannerMessage, setLivePhases } from '@/services/configService'
import { phaseString } from '@/utils/utils'

// PopUp Control
const openLivePhases = ref<boolean>(false);
const openBannerMessage = ref<boolean>(false);
// =========================

// Initial Config Loading
const config = ref<Config>();
onMounted(async () => { loadConfig() })
const loadConfig = async () => {
  config.value = await getConfig()

  bannerMessage.value = config.value?.bannerMessage
  //TODO: add live phases here
}
// =========================

// Banner Message Setting
const bannerMessage = ref<string>("Default Message")
const clearBannerMessage = () => {
  bannerMessage.value = ""
}
const submitBannerMessage = async () => {
  try {
    await setBannerMessage(bannerMessage.value)
  } catch (e) {
    alert("There was a problem in saving the updated banner message")
  }
  openBannerMessage.value = false
  await loadConfig()
}
// =========================

// Live Phase Setting
const activePhaseList = ref<Array<boolean>>([]) // using the enum, if activePhaseList[phase] == true, then that phase is active
const deactivateAllPhases = () => { setAllPhases(false) }
const activateAllPhases = () => { setAllPhases(true) }
const setAllPhases = (setting: boolean) => {
  for (const phase of listOfPhases() as Phase[]) {
    activePhaseList.value[phase] = setting
  }
}
const submitLivePhases = async () => {
  let livePhases: Phase[] = []
  for (const phase of listOfPhases() as Phase[]) {
    if (activePhaseList.value[phase]) {
      livePhases.push(phase);
    }
  }

  try {
    await setLivePhases(livePhases)
  } catch (e) {
    alert("There was a problem in saving live phases")
  }
  openLivePhases.value = false
  await loadConfig()
}
// =========================

</script>

<template>
  <div id="configContainer">
    <div class="configCategory">
      <h3>Canvas Integration</h3>
      <p><span class="infoDescription">Course ID:</span> 234563</p>

      <h4>Assignments</h4>
      <div v-for="phase in Object.values(Phase) as Phase[]">
        <p><span class="infoDescription">{{phase}} Assignment ID:</span> 234563</p>
        <p><span class="infoDescription">-Quality Rubric ID:</span> 34543</p>
        <p><span class="infoDescription">-Git Rubric ID:</span> 34543</p>
        <p><span class="infoDescription">-Main Rubric ID:</span> 34543</p>
      </div>
      <button>Change</button>
    </div>

    <div class="configCategory">
      <h3>Live Phases</h3>
      <p>These are the phases are live and open for students to submit to</p>
      <div v-for="phase in listOfPhases()">
        <p>
          <i v-if="activePhaseList[phase]" class="fa-solid fa-circle-check" style="color: green"/>
          <i v-else class="fa-solid fa-x" style="color: red"/>
          {{phase}}</p>
      </div>
      <button @click="openLivePhases = true">Update</button>
    </div>

    <div class="configCategory">
      <h3>Banner message</h3>
      <p v-if="config?.bannerMessage"><span class="infoDescription">Current Message: </span><span v-text="config.bannerMessage"/></p>
      <p v-else>There is currently no banner message</p>
      <button @click="openBannerMessage = true">Set</button>
    </div>
  </div>

  <PopUp
    v-if="openLivePhases"
    @closePopUp="openLivePhases = false">
    <h3>Live Phases</h3>
    <p>Enable student submissions for the following phases:</p>
    <label v-for="(phase, index) in listOfPhases()" :key="index">
      <input type="checkbox" v-model="activePhaseList[phase]"> {{ phase }}
    </label>

    <div class="submitChanges">
      <p><em>This will not effect admin submissions</em></p>
      <div>
        <button @click="activateAllPhases" class="small">Enable all</button>
        <button @click="deactivateAllPhases" class="small">Disable all</button>
      </div>
      <button @click="submitLivePhases">Submit Changes</button>
    </div>
  </PopUp>

  <PopUp
    v-if="openBannerMessage"
    @closePopUp="openBannerMessage = false">
    <h3>Banner Message</h3>
    <p>Set a message for students to see from the Autograder</p>
    <input v-model="bannerMessage" type="text" id="repoUrlInput" placeholder="No Banner Message"/>
    <div>
      <button class="small" @click="submitBannerMessage">Save</button>
      <button class="small" @click="clearBannerMessage">Clear</button>
    </div>
  </PopUp>
</template>

<style scoped>
.submitChanges {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.submitChanges >* {
  margin: 5px;
}
.infoDescription {
  font-weight: bold;
}

#configContainer {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr; /* Three columns of 100px each */
  grid-gap: 10px; /* Gap between grid items */
  margin: 10px;
}

button {
  margin-right: 5px;
  margin-top: 5px;
}

input {
  padding: 5px;
  width: 100%;
}
</style>
<script setup lang="ts">
import { ref } from 'vue'
import { listOfPhases, Phase } from '@/types/types'
import PopUp from '@/components/PopUp.vue'
import { setBannerMessage, setLivePhases } from '@/services/configService'
import { useAppConfigStore } from '@/stores/appConfig'

// PopUp Control
const openLivePhases = ref<boolean>(false);
const openBannerMessage = ref<boolean>(false);
// =========================

// Banner Message Setting
const bannerMessageToSubmit = ref<string>("")
const clearBannerMessage = () => {
  bannerMessageToSubmit.value = ""
}
const submitBannerMessage = async () => {
  try {
    await setBannerMessage(bannerMessageToSubmit.value)
  } catch (e) {
    alert("There was a problem in saving the updated banner message")
  }
  openBannerMessage.value = false
}
// =========================

// Live Phase Setting
const setAllPhases = (setting: boolean) => {
  for (const phase of listOfPhases() as Phase[]) {
    useAppConfigStore().phaseActivationList[phase] = setting
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
  openLivePhases.value = false
}
// =========================

</script>

<template>
  <div id="configContainer">
    <div class="configCategory">
      <h3>Live Phases</h3>
      <p>These are the phases are live and open for students to submit to</p>
      <div v-for="phase in listOfPhases()">
        <p>
          <i v-if="useAppConfigStore().phaseActivationList[phase]" class="fa-solid fa-circle-check" style="color: green"/>
          <i v-else class="fa-solid fa-x" style="color: red"/>
          {{phase}}</p>
      </div>
      <button @click="openLivePhases = true">Update</button>
    </div>

    <div class="configCategory">
      <h3>Banner message</h3>
      <p v-if="useAppConfigStore().bannerMessage"><span class="infoDescription">Current Message: </span><span v-text="useAppConfigStore().bannerMessage"/></p>
      <p v-else>There is currently no banner message</p>
      <button @click="openBannerMessage = true">Set</button>
    </div>
  </div>

  <PopUp
    v-if="openLivePhases"
    @closePopUp="openLivePhases = false; useAppConfigStore().updateConfig()">
    <h3>Live Phases</h3>
    <p>Enable student submissions for the following phases:</p>

    <div class="checkboxes">
      <label v-for="(phase, index) in listOfPhases()" :key="index">
        <span><input type="checkbox" v-model="useAppConfigStore().phaseActivationList[phase]"> {{ phase }}</span>
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
  </PopUp>

  <PopUp
    v-if="openBannerMessage"
    @closePopUp="openBannerMessage = false">
    <h3>Banner Message</h3>
    <p>Set a message for students to see from the Autograder</p>
    <input v-model="bannerMessageToSubmit" type="text" id="repoUrlInput" placeholder="No Banner Message"/>
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

input[type="text"]{
  padding: 5px;
  width: 100%;
}

.checkboxes {
  display: flex;
  flex-direction: column;
}
</style>
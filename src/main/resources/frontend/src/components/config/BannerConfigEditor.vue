<script setup lang="ts">
import { ref } from 'vue'
import { useAppConfigStore } from '@/stores/appConfig'
import { setBanner } from '@/services/configService'
import { combineDateAndTime } from '@/utils/utils'

const { closeEditor } = defineProps<{
  closeEditor: () => void
}>()

const appConfigStore = useAppConfigStore()

const bannerMessageToSubmit = ref<string>(appConfigStore.bannerMessage)
const bannerColorToSubmit = ref<string>(appConfigStore.bannerColor)
const bannerLinkToSubmit = ref<string>(appConfigStore.bannerLink)
const bannerWillExpire = ref<boolean>(false)
const bannerExpirationDate = ref<string>('')
const bannerExpirationTime = ref<string>('')
const clearBannerMessage = () => {
  bannerMessageToSubmit.value = ''
  bannerLinkToSubmit.value = ''
  bannerColorToSubmit.value = ''
  bannerColorToSubmit.value = ''
  bannerWillExpire.value = false
}
const submitBanner = async () => {
  let combinedDateTime
  if (bannerWillExpire.value) {
    combinedDateTime = combineDateAndTime(bannerExpirationDate.value, bannerExpirationTime.value)
  } else {
    combinedDateTime = ''
  }
  try {
    await setBanner(
      bannerMessageToSubmit.value,
      bannerLinkToSubmit.value,
      bannerColorToSubmit.value,
      combinedDateTime
    )
    closeEditor()
  } catch (e) {
    appConfigStore.updateConfig()
    alert('There was a problem in saving the updated banner message:\n' + e)
  }
}
</script>

<template>
  <p>Set a message for students to see from the Autograder</p>
  <input v-model="bannerMessageToSubmit" type="text" placeholder="No Banner Message" />
  <p>Set a url that the user will be taken to if they click on the banner</p>
  <input v-model="bannerLinkToSubmit" type="text" placeholder="No Destination URL" />
  <p>Choose a background color</p>
  <select
    id="bannerColorSelect"
    v-model="bannerColorToSubmit"
    :style="{
      backgroundColor: bannerColorToSubmit,
      color: bannerColorToSubmit ? '#ffffff' : '#000000'
    }"
  >
    <option selected value="">Default</option>
    <option value="#d62b18">Red</option>
    <option value="#eb700c">Orange</option>
    <option value="#ded77a">Yellow</option>
    <option value="#0cab11">Green</option>
    <option value="#002E5D">BYU Blue</option>
    <option value="#5e12b5">Purple</option>
    <option value="#424142">Gray</option>
    <option value="#000000">Black</option>
  </select>
  <p>Message Expires: <input type="checkbox" v-model="bannerWillExpire" /></p>
  <div v-if="bannerWillExpire">
    <input type="date" v-model="bannerExpirationDate" /><input
      type="time"
      v-model="bannerExpirationTime"
    />
    <p><em>If no time is selected, it will expire at the end of the day (Utah Time)</em></p>
  </div>

  <div>
    <button @click="submitBanner" :disabled="bannerWillExpire && bannerExpirationDate.length == 0">
      Submit
    </button>
    <button class="small" @click="clearBannerMessage">Clear Editor</button>
  </div>
</template>

<style scoped></style>

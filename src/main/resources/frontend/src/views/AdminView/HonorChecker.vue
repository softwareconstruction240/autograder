<script setup lang="ts">

import {ref} from "vue";
import {honorCheckerZipGet} from "@/services/adminService";

const selectedSection = ref<number>(1)
const infoText = ref<string>('')
const buttonDisabled = ref<boolean>(false)

const onSelectionChange = (event: Event) => {
  const selectElement = event.target as HTMLSelectElement;
  selectedSection.value = Number(selectElement.value);
}

const getData = async () => {
  buttonDisabled.value = true
  infoText.value = 'Downloading... (this shouldn\'t take more than 30 seconds)'
  const data: Blob = await honorCheckerZipGet(selectedSection.value)
  triggerDownload(data, 'section-' + selectedSection.value + '.zip')
  if (data.size == 0) {
    infoText.value = 'Error occurred server side. Check logs or browser console.'
  } else {
    infoText.value = 'Complete! Check your downloads folder for the .zip file.'
  }
  buttonDisabled.value = false
}

const triggerDownload = (data: Blob, filename: string) => {
  const link = document.createElement('a')

  link.href = window.URL.createObjectURL(data)
  link.download = filename

  document.body.appendChild(link)

  link.click()

  document.body.removeChild(link)
}

</script>

<template>
  <div class="container">
    <h3>Honor Checker Download</h3>
    <p class="desc">If you are a professor who wants to run the honor checker on your section, you've come to the
        right place! The honor checker itself is located elsewhere, but below, you can download .zip files of your section(s).
        Each section .zip file contains a .zip file for every student containing their source code.</p>
    <label for="section">Choose a section to download: </label>
    <select name="section" @change="onSelectionChange">
      <option value="1">Section 1</option>
      <option value="2">Section 2</option>
      <option value="3">Section 3</option>
      <option value="4">Section 4</option>
      <option value="5">Section 5</option>
    </select>
    <button :disabled="buttonDisabled" @click="getData">Download</button>
    <p>{{ infoText }}</p>
  </div>
</template>

<style scoped>
.container {
  padding: 10px;
  text-align: center;
}

.desc {
  text-align: center;
  width: 70%;
  margin: 0 auto;
}

button {
  margin: 1rem;
}
</style>

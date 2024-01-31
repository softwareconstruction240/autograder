<script setup lang="ts">

import {ref} from "vue";
import {commitAnalyticsGet} from "@/services/adminService";

const infoText = ref<string>('This data takes around 5 minutes to compile. By default, it is sorted by section and then by netID.')
const buttonDisabled = ref<boolean>(false)

const getData = async () => {
  buttonDisabled.value = true
  infoText.value = 'Downloading data... (should take around 5 minutes)'
  console.log('getting data...')
  const data: string = await commitAnalyticsGet()
  triggerDownload(data, 'commit-data-' + Math.floor(Date.now() / 1000) + '.csv')
  if (data.length == 0) {
    infoText.value = 'Error occurred server side. Check logs or browser console.'
  } else {
    infoText.value = 'Complete! Check your downloads folder for the .csv file.'
  }
  buttonDisabled.value = false
}

const triggerDownload = (csvData: string, filename: string) => {
  const blob = new Blob([csvData], { type: 'text/csv' })
  const link = document.createElement('a')

  link.href = window.URL.createObjectURL(blob)
  link.download = filename

  document.body.appendChild(link)

  link.click()

  document.body.removeChild(link)
}

</script>

<template>
  <div class="container">
    <button :disabled="buttonDisabled" @click="getData">Download Commit CSV Data</button>
    <p>CSV commit data contains these columns:</p>
    <table>
      <thead>
      <tr>
        <th>netID</th>
        <th>phase</th>
        <th>numCommits</th>
        <th>numDays</th>
        <th>section</th>
        <th>timestamp</th>
      </tr>
      </thead>
      <tbody>
      <tr>
        <td>
          The netID of the student
        </td>
        <td>
          The phase that was passed off
        </td>
        <td>
          The total number of commits for this phase
        </td>
        <td>
          How many distinct days are represented amongst the commits
        </td>
        <td>
          The section this student is in
        </td>
        <td>
          When this passoff occurred (mountain time)
        </td>
      </tr>
      </tbody>
    </table>
    <p>{{ infoText }}</p>
  </div>
</template>

<style scoped>
.container {
  padding: 10px;
  text-align: center;
}

table {
  width: 100%;
}

th {
  background-color: #333;
  color: #fff;
}

th, td {
  padding: 0.25rem;
  border: 1px solid #ccc;
}
</style>
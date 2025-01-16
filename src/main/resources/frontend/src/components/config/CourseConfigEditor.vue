<script setup lang="ts">
import { useConfigStore } from '@/stores/config'
import { ref } from 'vue'
import { setCourseId } from '@/services/configService'

const config = useConfigStore();

const courseNumber = ref<number>(config.admin.courseNumber)

const { closeEditor } = defineProps<{
  closeEditor: () => void
}>();

const valueReady = ():boolean => {
  const value : unknown = courseNumber.value
  return typeof value === 'number' && !isNaN(value);
}

const submit = async () => {
  try {
    await setCourseId(courseNumber.value)
    closeEditor()
  } catch (e) {
    alert(e)
  }
}

</script>

<template>
  <p>The Autograder uses Canvas to know who is enrolled in CS 240, and to save official grades. Students and TAs can not log in
    unless they are enrolled in the Canvas course configured here.</p>
  <p>Canvas Course Number</p>
  <p><input type="number" v-model="courseNumber"/></p>
  <p>You can find the course number in the course homepage URL.</p>
  <p><em>https://byu.instructure.com/courses/<b>12345</b></em> would mean the course number is <b>12345</b></p>
  <button :disabled="!valueReady()" @click="submit">Submit</button>
</template>

<style scoped>
.advanced {
  font-style: italic;
  text-decoration: underline;
  cursor: pointer;
  width: 150px;
}
.inline-container {
  display: flex;
  align-items: center;
  margin-right: 10px; /* Optional: Adjust spacing between elements */
  margin-left: 10px;
}

.inline-container label {
  margin-right: 5px; /* Optional: Adjust spacing between label and input */
}
</style>

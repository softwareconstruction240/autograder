<script setup lang="ts">
import { useConfigStore } from "@/stores/config";
import { ref } from "vue";
import { reloadCourseIds, setCourseId } from "@/services/configService";

const config = useConfigStore();

const courseNumber = ref<number>(config.admin.courseNumber);

const { closeEditor } = defineProps<{
  closeEditor: () => void;
}>();

const valueReady = (): boolean => {
  const value: unknown = courseNumber.value;
  return typeof value === "number" && !isNaN(value);
};

const submit = async () => {
  try {
    await setCourseId(courseNumber.value);
    closeEditor();
  } catch (e) {
    alert(e);
  }
};

const reloadIds = async () => {
  try {
    await reloadCourseIds();
    closeEditor();
  } catch (e) {
    alert(e);
  }
};
</script>

<template>
  <p>
    The Autograder uses Canvas to know who is enrolled in CS 240, and to save official grades.
    Students and TAs can not log in unless they are enrolled in the Canvas course configured here.
  </p>
  <p>Canvas Course Number</p>
  <p><input type="number" v-model="courseNumber" /></p>
  <p>You can find the course number in the course homepage URL.</p>
  <p>
    <em>https://byu.instructure.com/courses/<b>12345</b></em> would mean the course number is
    <b>12345</b>
  </p>
  <button :disabled="!valueReady()" @click="submit">Submit</button>
  <hr />
  <div>
    <h4>Other Ids</h4>
    <p><em>You should not need to use this button in most cases:</em></p>
    <p>
      The Autograder uses the assignment and rubric ids to map submissions to their proper place in
      Canvas. These are automatically pulled from Canvas when you set the course id, but if rubric
      items or assignments were changed after the fact, then you made need to reload them.
    </p>
    <button class="small" @click="reloadIds">Reload Assignment/Rubric Ids</button>
    <p>
      <em
        >This will reload assignment and rubric ids from the current course:
        {{ config.admin.courseNumber }}</em
      >
    </p>
  </div>
</template>

<style scoped>
hr {
  margin: 10px 0;
}
</style>

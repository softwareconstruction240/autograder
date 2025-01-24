<script setup lang="ts">
import { ref, computed } from "vue";
import Panel from "@/components/Panel.vue";
import { updateHolidays } from "@/services/configService";

const { closeEditor } = defineProps<{
  closeEditor: () => void;
}>();

// Base set of holidays
const holidaysSet = ref<Set<string>>(new Set<string>());

// Computed property that gives us sorted holidays
const sortedHolidays = computed(() => {
  return Array.from(holidaysSet.value).sort();
});

const currentDateToAdd = ref<string>("");

const addHolidayToList = () => {
  holidaysSet.value.add(currentDateToAdd.value);
  // Create new Set to trigger reactivity
  holidaysSet.value = new Set(holidaysSet.value);
  currentDateToAdd.value = "";
}

const removeHolidayFromList = (holiday: string) => {
  holidaysSet.value.delete(holiday);
  // Create new Set to trigger reactivity
  holidaysSet.value = new Set(holidaysSet.value);
}

const clearHolidayList = () => {
  holidaysSet.value = new Set();
}

const submitHolidays = () => {
  updateHolidays([...holidaysSet.value])
}
</script>

<template>
  <p>Remove holidays from the list or add new ones.</p>
  <div v-for="holiday in sortedHolidays"
       style="display: flex; align-items: center;"
       :key="holiday">
    <p>{{ holiday }}</p>
    <i class="fa-solid fa-trash cursor-pointer hover:text-red-500"
       @click="removeHolidayFromList(holiday)"/>
  </div>
  <div>
    <button class="small"
            :disabled="holidaysSet.size === 0"
            @click="clearHolidayList">Clear list</button>
    <input type="date" v-model="currentDateToAdd" />
    <button class="small"
            :disabled="!currentDateToAdd"
            @click="addHolidayToList">
      <i class="fa-solid fa-plus"/>
    </button>
  </div>
  <button @click="submitHolidays" style="margin-top: 10px">
    Submit
  </button>
  <Panel style="max-width: 400px">
    <p>Holidays don't count towards the late-day count for submissions. The Autograder already treats every Saturday and Sunday as a "holiday", so it is only necessary to add BYU specific holidays.</p>
  </Panel>
</template>

<style scoped>

</style>

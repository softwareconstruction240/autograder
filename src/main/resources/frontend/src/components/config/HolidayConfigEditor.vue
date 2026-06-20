<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import Panel from "@/components/Panel.vue";
import { updateHolidays } from "@/services/configService";
import { simpleDate } from "@/utils/utils";
import { useConfigStore } from "@/stores/config";

const { closeEditor } = defineProps<{
  closeEditor: () => void;
}>();

const holidaysSet = ref<Set<string>>(new Set<string>());

onMounted(() => {
  if (useConfigStore().admin.holidays.length > 0) {
    useConfigStore().admin.holidays.forEach((holiday) => {
      holidaysSet.value.add(holiday.toString());
    });
  }
});

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
};

const removeHolidayFromList = (holiday: string) => {
  holidaysSet.value.delete(holiday);
  // Create new Set to trigger reactivity
  holidaysSet.value = new Set(holidaysSet.value);
};

const clearHolidayList = () => {
  holidaysSet.value = new Set();
};

const submitHolidays = async () => {
  try {
    await updateHolidays([...holidaysSet.value]);
    closeEditor();
  } catch (e) {
    useConfigStore().updatePublicConfig();
    alert("There was a problem while saving holidays");
  }
};

const noFutureHolidays = () => {
  const today = new Date();
  today.setHours(0, 0, 0, 0); // Set to beginning of today

  // Return true if there are NO future holidays (which would be an error)
  return !sortedHolidays.value.some((holiday) => {
    const holidayDate = new Date(holiday);
    return holidayDate > today;
  });
};
</script>

<template>
  <p>Remove holidays from the list or add new ones.</p>
  <div v-for="holiday in sortedHolidays" style="display: flex; align-items: center" :key="holiday">
    <p>{{ simpleDate(holiday) }}</p>
    <i
      class="fa-solid fa-trash cursor-pointer hover:text-red-500"
      @click="removeHolidayFromList(holiday)"
    />
  </div>
  <div>
    <button class="small" :disabled="holidaysSet.size === 0" @click="clearHolidayList">
      Clear list
    </button>
    <input type="date" v-model="currentDateToAdd" />
    <button class="small" :disabled="!currentDateToAdd" @click="addHolidayToList">
      <i class="fa-solid fa-plus" />
    </button>
  </div>
  <button
    @click="submitHolidays"
    :disabled="holidaysSet.size > 0 && noFutureHolidays()"
    style="margin-top: 10px"
  >
    Submit
  </button>
  <div
    v-if="noFutureHolidays()"
    style="
      background-color: red;
      color: white;
      padding: 5px;
      margin: 5px;
      border-radius: 5px;
      max-width: 400px;
      text-align: center;
      align-self: center;
    "
  >
    <p><b>There must be at least one future holiday</b></p>
    <p>
      The Autograder will throw an error and refuse to grade submissions if there are some holidays
      scheduled but none in the future. Having only holidays in the past implies that the system
      config is not up to date
    </p>
  </div>
  <Panel style="max-width: 400px">
    <p>
      Holidays don't count towards the late-day count for submissions. The Autograder already treats
      every Saturday and Sunday as a "holiday", so it is only necessary to add BYU specific
      holidays.
    </p>
    <p>
      See the <a href="https://academiccalendar.byu.edu/" target="_blank">BYU Academic Calendar</a>
    </p>
  </Panel>
</template>

<style scoped></style>

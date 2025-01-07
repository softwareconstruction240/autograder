<script setup lang="ts">
import { listOfPhases, Phase, type RubricInfo, type RubricType } from '@/types/types';
import {
  convertPhaseStringToEnum,
  convertRubricTypeToHumanReadable,
  getRubricTypes,
  isPhaseGraded,
} from '@/utils/utils';
import { computed, type WritableComputedRef } from 'vue';
import { setCanvasCourseIds, setCourseIds } from '@/services/configService';
import { useAppConfigStore } from '@/stores/appConfig';

const appConfigStore = useAppConfigStore();

const { closeEditor } = defineProps<{
  closeEditor: () => void;
}>();

const assignmentIdProxy = (phase: Phase): WritableComputedRef<number> =>
  computed({
    get: (): number => appConfigStore.assignmentIds.get(phase) || -1,
    set: (value: number) => appConfigStore.assignmentIds.set(phase, value),
  });

const rubricIdInfoProxy = (phase: Phase, rubricType: RubricType): WritableComputedRef<string> => {
  return getProxy(
    phase,
    rubricType,
    (rubricInfo) => rubricInfo.id,
    (rubricInfo, value) => (rubricInfo.id = value),
    'No Rubric ID found',
  );
};

const rubricPointsInfoProxy = (
  phase: Phase,
  rubricType: RubricType,
): WritableComputedRef<number> => {
  return getProxy(
    phase,
    rubricType,
    (rubricInfo) => rubricInfo.points,
    (rubricInfo, value) => (rubricInfo.points = value),
    -1,
  );
};

const getProxy = <T,>(
  phase: Phase,
  rubricType: RubricType,
  getFunc: (rubricInfo: RubricInfo) => T,
  setFunc: (rubricInfo: RubricInfo, value: T) => void,
  defaultValue: T,
): WritableComputedRef<T> =>
  computed({
    get: (): T => {
      const rubricIdMap = appConfigStore.rubricInfo.get(phase);
      if (!rubricIdMap) return defaultValue;
      const rubricInfo = rubricIdMap.get(rubricType);
      if (!rubricInfo) return defaultValue;
      return getFunc(rubricInfo);
    },
    set: (value: T) => {
      const rubricTypeMap = appConfigStore.rubricInfo.get(phase);
      if (!rubricTypeMap) return;
      const rubricInfo = rubricTypeMap.get(rubricType);
      if (!rubricInfo) return;
      setFunc(rubricInfo, value);
    },
  });

const submitManuelCourseIds = async () => {
  const userConfirmed = window.confirm(
    "Are you sure you want to manually override? \n\nIf you changed the course ID incorrectly, it won't be able to reset properly.",
  );
  if (userConfirmed) {
    try {
      await setCourseIds(
        appConfigStore.courseNumber,
        appConfigStore.assignmentIds,
        appConfigStore.rubricInfo,
      );
      closeEditor();
    } catch (e) {
      alert('There was problem manually setting the course-related IDs: ' + (e as Error).message);
    }
  }
};

const submitCanvasCourseIds = async () => {
  const userConfirmed = window.confirm(
    'Are you sure you want to use Canvas to reset ID values? \n\nNote: This will fail if the currently saved Course ID is incorrect.',
  );
  if (userConfirmed) {
    try {
      await setCanvasCourseIds();
    } catch (e) {
      alert(
        'There was problem getting and setting the course-related IDs using Canvas: ' +
          (e as Error).message,
      );
    }
    closeEditor();
  }
};
</script>

<template>
  <p>
    <i class="fa-solid fa-triangle-exclamation" style="color: orangered" />
    Note: All the default input values are the values that are currently being used.
  </p>

  <br />
  <h4>Course Number</h4>
  <label for="courseIdInput">Course Number: </label>
  <input
    id="courseIdInput"
    type="number"
    v-model.number="appConfigStore.courseNumber"
    placeholder="Course Number"
  />
  <br /><br />
  <h4>Assignment and Rubric IDs/Points</h4>
  <div v-for="(phase, phaseIndex) in listOfPhases()" :key="phaseIndex">
    <div v-if="isPhaseGraded(phase)">
      <h4>{{ phase }}:</h4>
      <label :for="'assignmentIdInput' + phaseIndex">Assignment ID: </label>
      <input
        :id="'assignmentIdInput' + phaseIndex"
        type="number"
        v-model.number="assignmentIdProxy(phase).value"
        placeholder="Assignment ID"
      />
      <br />

      <ol>
        <li
          v-for="(rubricType, rubricIndex) in getRubricTypes(
            convertPhaseStringToEnum(phase as unknown as string),
          )"
          :key="rubricIndex"
        >
          <u>{{ convertRubricTypeToHumanReadable(rubricType) }}</u
          >:
          <div class="inline-container">
            <label :for="'rubricIdInput' + phaseIndex + rubricIndex">Rubric&nbsp;ID: </label>
            <input
              :id="'rubricIdInput' + phaseIndex + rubricIndex"
              type="text"
              v-model="rubricIdInfoProxy(phase, rubricType).value"
              placeholder="Rubric ID"
            />
          </div>
          <div class="inline-container">
            <label :for="'rubricPointsInput' + phaseIndex + rubricIndex">Rubric Points: </label>
            <input
              :id="'rubricPointsInput' + phaseIndex + rubricIndex"
              type="number"
              v-model.number="rubricPointsInfoProxy(phase, rubricType).value"
              placeholder="Points"
            />
          </div>
        </li>
      </ol>
    </div>
  </div>

  <br />
  <button @click="submitManuelCourseIds">Submit</button>
  <button @click="submitCanvasCourseIds">Reset IDs Via Canvas</button>
</template>

<style scoped>
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

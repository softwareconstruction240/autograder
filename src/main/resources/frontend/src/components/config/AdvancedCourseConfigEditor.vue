<script setup lang="ts">
import { listOfPhases, Phase, type RubricInfo, type RubricType } from '@/types/types'
import {
  convertPhaseStringToEnum,
  convertRubricTypeToHumanReadable,
  getRubricTypes,
  isPhaseGraded
} from '@/utils/utils'
import { computed, type WritableComputedRef } from 'vue'
import { setCanvasCourseIds, setCourseIds } from '@/services/configService'
import { useConfigStore } from '@/stores/config'

const config = useConfigStore()

const { closeEditor } = defineProps<{
  closeEditor: () => void
}>()

// Helper to find assignment by phase
const findAssignment = (phase: Phase) => {
  return config.admin.assignments.find(a => a.phase === phase)
}

const assignmentIdProxy = (phase: Phase): WritableComputedRef<number> => computed({
  get: (): number => findAssignment(phase)?.assignmentId || -1,
  set: (value: number) => {
    const assignment = findAssignment(phase)
    if (assignment) {
      assignment.assignmentId = value
    } else {
      // Create new assignment if it doesn't exist
      config.admin.assignments.push({
        phase,
        assignmentId: value,
        rubricItems: new Map()
      })
    }
  }
})

const rubricIdInfoProxy = (phase: Phase, rubricType: RubricType): WritableComputedRef<string> => {
  return getProxy(
    phase,
    rubricType,
    (rubricInfo) => rubricInfo.id,
    (rubricInfo, value) => rubricInfo.id = value,
    "No Rubric ID found"
  )
}

const rubricPointsInfoProxy = (phase: Phase, rubricType: RubricType): WritableComputedRef<number> => {
  return getProxy(
    phase,
    rubricType,
    (rubricInfo) => rubricInfo.points,
    (rubricInfo, value) => rubricInfo.points = value,
    -1
  )
}

const getProxy = <T>(
  phase: Phase,
  rubricType: RubricType,
  getFunc: (rubricInfo: RubricInfo) => T,
  setFunc: (rubricInfo: RubricInfo, value: T) => void,
  defaultValue: T,
): WritableComputedRef<T> => computed({
  get: (): T => {
    const assignment = findAssignment(phase)
    if (!assignment) return defaultValue
    const rubricInfo = assignment.rubricItems.get(rubricType)
    if (!rubricInfo) return defaultValue
    return getFunc(rubricInfo)
  },
  set: (value: T) => {
    const assignment = findAssignment(phase)
    if (!assignment) return
    const rubricInfo = assignment.rubricItems.get(rubricType)
    if (!rubricInfo) {
      // Create new rubric info if it doesn't exist
      assignment.rubricItems.set(rubricType, {
        id: rubricType === RubricType.AUTOMATED ? value as string : '',
        points: rubricType === RubricType.AUTOMATED ? -1 : value as number
      })
    } else {
      setFunc(rubricInfo, value)
    }
  }
})

const submitManuelCourseIds = async () => {
  const userConfirmed = window.confirm(
    "Are you sure you want to manually override? \n\nIf you changed the course ID incorrectly, it won't be able to reset properly."
  )
  if (userConfirmed) {
    try {
      await setCourseIds(config.admin)
      closeEditor()
    } catch (e) {
      alert("There was problem manually setting the course-related IDs: " + (e as Error).message)
    }
  }
}

const submitCanvasCourseIds = async () => {
  const userConfirmed = window.confirm(
    "Are you sure you want to use Canvas to reset ID values? \n\nNote: This will fail if the currently saved Course ID is incorrect."
  )
  if (userConfirmed) {
    try {
      await setCanvasCourseIds()
    } catch (e) {
      alert("There was problem getting and setting the course-related IDs using Canvas: " + (e as Error).message)
    }
    closeEditor()
  }
}
</script>

<template>
  <p>
    <i class="fa-solid fa-triangle-exclamation" style="color: orangered" />
    Note: All the default input values are the values that are currently being used.
  </p>

  <br>
  <h4>Course Number</h4>
  <label for="courseIdInput">Course Number: </label>
  <input id="courseIdInput" type="number" v-model.number="config.admin.courseNumber" placeholder="Course Number">

  <br><br>
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
      >
      <br>

      <ol>
        <li
          v-for="(rubricType, rubricIndex) in getRubricTypes(convertPhaseStringToEnum(phase as unknown as string))"
          :key="rubricIndex"
        >
          <u>{{ convertRubricTypeToHumanReadable(rubricType) }}</u>:
          <div class="inline-container">
            <label :for="'rubricIdInput' + phaseIndex + rubricIndex">Rubric&nbsp;ID: </label>
            <input
              :id="'rubricIdInput' + phaseIndex + rubricIndex"
              type="text"
              v-model="rubricIdInfoProxy(phase, rubricType).value"
              placeholder="Rubric ID"
            >
          </div>
          <div class="inline-container">
            <label :for="'rubricPointsInput' + phaseIndex + rubricIndex">Rubric Points: </label>
            <input
              :id="'rubricPointsInput' + phaseIndex + rubricIndex"
              type="number"
              v-model.number="rubricPointsInfoProxy(phase, rubricType).value"
              placeholder="Points"
            >
          </div>
        </li>
      </ol>
    </div>
  </div>

  <br>
  <button @click="submitManuelCourseIds">Submit</button>
  <button @click="submitCanvasCourseIds">Reset IDs Via Canvas</button>
</template>

<style scoped>

</style>

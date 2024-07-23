<script setup lang="ts">
import {computed, ref, type WritableComputedRef} from 'vue'
import PopUp from '@/components/PopUp.vue'
import {listOfPhases, Phase, type RubricInfo, type RubricType} from '@/types/types'
import {setBannerMessage, setLivePhases, setCanvasCourseIds, setCourseIds} from '@/services/configService'
import { useAppConfigStore } from '@/stores/appConfig'
import {
  convertPhaseStringToEnum,
  convertRubricTypeToHumanReadable,
  getRubricTypes,
  isPhaseGraded
} from "@/utils/utils";

const appConfigStore = useAppConfigStore();

// PopUp Control
const openLivePhases = ref<boolean>(false);
const openBannerMessage = ref<boolean>(false);
const openCanvasCourseIds = ref<boolean>(false);
const openManuelCourseIds = ref<boolean>(false);

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
    appConfigStore.phaseActivationList[phase] = setting
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

// Course ID Setting

const getUpdatedConfig = async () => {
  await appConfigStore.updateConfig();
  openManuelCourseIds.value = true;
}

const assignmentIdProxy = (phase: Phase): WritableComputedRef<number> => computed({
  get: (): number => appConfigStore.assignmentIds.get(phase) || -1,
  set: (value: number) => appConfigStore.assignmentIds.set(phase, value)
})

const rubricIdInfoProxy = (phase: Phase, rubricType: RubricType): WritableComputedRef<string> => {
  return getProxy(
      phase,
      rubricType,
      (rubricInfo) => rubricInfo.id,
      (rubricInfo, value) => rubricInfo.id = value,
      "No Rubric ID found"
  );
}

const rubricPointsInfoProxy = (phase: Phase, rubricType: RubricType): WritableComputedRef<number> => {
  return getProxy(
      phase,
      rubricType,
      (rubricInfo) => rubricInfo.points,
      (rubricInfo, value) => rubricInfo.points = value,
      -1
  );
}

const getProxy = <T>(
    phase: Phase,
    rubricType: RubricType,
    getFunc: (rubricInfo: RubricInfo) => T,
    setFunc: (rubricInfo: RubricInfo, value: T) => void,
    defaultValue: T,
): WritableComputedRef<T> => computed({
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
  }
});

const submitManuelCourseIds = async () => {
  const userConfirmed = window.confirm("Are you sure you want to manually override?");
  if (userConfirmed) {
    try {
      await setCourseIds(appConfigStore.courseNumber, appConfigStore.assignmentIds, appConfigStore.rubricInfo);
      openManuelCourseIds.value = false;
    } catch (e) {
      alert("There was problem manually setting the course-related IDs: " + (e as Error).message);
    }
  }
}

const submitCanvasCourseIds = async () => {
  try {
    await setCanvasCourseIds();
    openCanvasCourseIds.value = false;
  } catch (e) {
    alert("There was problem getting and setting the course-related IDs using Canvas: " + (e as Error).message);
  }
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
          <i v-if="appConfigStore.phaseActivationList[phase]" class="fa-solid fa-circle-check" style="color: green"/>
          <i v-else class="fa-solid fa-x" style="color: red"/>
          {{phase}}</p>
      </div>
      <button @click="openLivePhases = true">Update</button>
    </div>

    <div class="configCategory">
      <h3>Banner message</h3>
      <p v-if="appConfigStore.bannerMessage"><span class="infoDescription">Current Message: </span><span v-text="appConfigStore.bannerMessage"/></p>
      <p v-else>There is currently no banner message</p>
      <button @click="openBannerMessage = true">Set</button>
    </div>

    <div class="configCategory">
      <h3>Course Related IDs</h3>
      <button @click="openCanvasCourseIds = true">Update using Canvas</button>
      <button @click="getUpdatedConfig">Update Manually</button>
    </div>
  </div>

  <PopUp
    v-if="openLivePhases"
    @closePopUp="openLivePhases = false; appConfigStore.updateConfig()">
    <h3>Live Phases</h3>
    <p>Enable student submissions for the following phases:</p>

    <div class="checkboxes">
      <label v-for="(phase, index) in listOfPhases()" :key="index">
        <span><input type="checkbox" v-model="appConfigStore.phaseActivationList[phase]"> {{ phase }}</span>
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

  <PopUp
    v-if="openCanvasCourseIds"
    @closePopUp="openCanvasCourseIds = false">

    <h3 style="text-align: center">Course Related IDs</h3>
    <p style="text-align: center">
      Are you sure you want to use Canvas for retrieving assignment IDs, rubric IDs, and rubric points?<br />
      This will overwrite the preexisting values and cannot be restored.
    </p>

    <span class="center-buttons">
      <button @click="submitCanvasCourseIds">Yes</button>
      <button @click="openCanvasCourseIds = false">Go Back</button>
    </span>
  </PopUp>

  <PopUp
    v-if="openManuelCourseIds"
    @closePopUp="openManuelCourseIds = false">
    <h3>Course Related IDs</h3>
    <p>
      <i class="fa-solid fa-triangle-exclamation" style="color: orangered"/>
      Note: All the default input values are the values that are currently being used.
    </p>

    <br>
    <h4>Course Number</h4>
    <label for="courseIdInput">Course Number: </label>
    <input id="courseIdInput" type="number" v-model.number="appConfigStore.courseNumber" placeholder="Course Number">
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
          <li v-for="(rubricType, rubricIndex) in getRubricTypes(convertPhaseStringToEnum(phase as unknown as string))" :key="rubricIndex">
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
    <button @click="getUpdatedConfig">Reset Values</button>
    <button @click="openManuelCourseIds = false">Close</button>
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

.center-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
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
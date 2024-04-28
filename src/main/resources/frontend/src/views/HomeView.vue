<script setup lang="ts">

import Tabs from "@/components/tabs/Tabs.vue";
import Tab from "@/components/tabs/Tab.vue";
import PhaseView from "@/views/PhaseView/PhaseView.vue";
import {onMounted} from "vue";
import {useSubmissionStore} from "@/stores/submissions";
import {Phase} from "@/types/types";

// periodically check if grading is happening
onMounted(async () => {
  setInterval(async () => {
    if (!useSubmissionStore().currentlyGrading)
      await useSubmissionStore().checkGrading();
  }, 5000);
})

</script>

<template>
  <div>
    <h3 id="instruction-text">Select a phase from below to submit code, view test results, and review past attempts</h3>
  </div>
  <Tabs>
    <Tab title="Phase 0">
      <PhaseView phase-title="Phase 0: Chess piece move rules" phaseDescription="For this phase you must pass all of the provided automated tests." :phase=Phase.Phase0 />
    </Tab>
    <Tab title="Phase 1">
      <PhaseView phase-title="Phase 1: Full chess game logic" phaseDescription="For this phase you must pass all of the provided automated tests." :phase=Phase.Phase1 />
    </Tab>
    <Tab title="Phase 3">
      <PhaseView phase-title="Phase 3: Web API" phaseDescription="For this phase you must pass all of the provided automated tests and pass all of the unit tests that you were required to write." :phase=Phase.Phase3 />
    </Tab>
    <Tab title="Phase 4">
      <PhaseView phase-title="Phase 4: SQL DAOs" phaseDescription="For this phase you must pass all of the provided automated tests and pass all of the unit tests that you were required to write." :phase=Phase.Phase4 />
    </Tab>
    <Tab title="Phase 5">
      <PhaseView phase-title="Phase 5: Chess Client" phaseDescription="For this phase you must pass all of the unit tests that you were required to write." :phase=Phase.Phase5 />
    </Tab>
    <Tab title="Phase 6">
      <PhaseView phase-title="Phase 6: Websockets" phaseDescription="For this phase you must pass all of the provided automated tests." :phase=Phase.Phase6 />
    </Tab>
    <Tab title="Quality">
      <PhaseView phase-title="Run only code quality (not graded)" phaseDescription="Use this to run just the code quality without running test cases. This is NOT graded and will NOT be submitted to Canvas." :phase=Phase.Quality />
    </Tab>
  </Tabs>
</template>

<style scoped>
#instruction-text {
  text-align: center;
  margin-top: 5px;
  margin-bottom: 15px;
}
</style>
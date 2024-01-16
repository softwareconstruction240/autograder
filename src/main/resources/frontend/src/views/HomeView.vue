<script setup lang="ts">

import Tabs from "@/components/tabs/Tabs.vue";
import Tab from "@/components/tabs/Tab.vue";
import PhaseView from "@/views/PhaseView/PhaseView.vue";
</script>

<template>
  <div>
    <h3 id="instruction-text">Select a phase from below to submit code, view test results, and review past attempts</h3>
  </div>
  <Tabs>
    <Tab title="Phase 0">
      <PhaseView phase-title="Phase 0: Chess piece move rules" phaseDescription="For this phase you must pass all of the provided automated tests." phase="0" />
    </Tab>
    <Tab title="Phase 1">
      <PhaseView phase-title="Phase 1: Full chess game logic" phaseDescription="For this phase you must pass all of the provided automated tests." phase="1" />
    </Tab>
    <Tab disabled title="Phase 3">
      <PhaseView phase-title="Phase 3: Web API" phaseDescription="For this phase you must pass all of the provided automated tests." phase="3" />
    </Tab>
    <Tab title="Phase 4" disabled>Phase 4 content</Tab>
    <Tab title="Phase 6" disabled>Phase 6 content</Tab>
  </Tabs>
</template>

<style scoped>
#instruction-text {
  text-align: center;
  margin-top: 5px;
  margin-bottom: 15px;
}
</style>
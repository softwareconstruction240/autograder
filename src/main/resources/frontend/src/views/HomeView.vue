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
      <PhaseView phase-title="Phase 1: Full chess game logic" phaseDescription="For this phase you must pass all of the provided automated tests." phase="1"/>
    </Tab>
    <Tab title="Phase 3">Phase 3 content</Tab>
    <Tab title="Phase 4">Phase 4 content</Tab>
    <Tab title="Phase 6">Phase 6 content</Tab>
  </Tabs>
</template>

<style scoped>
#instruction-text {
  text-align: center;
  margin-top: 5px;
  margin-bottom: 15px;
}
</style>
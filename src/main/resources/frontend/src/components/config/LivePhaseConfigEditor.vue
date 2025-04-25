<script setup lang="ts">
import { listOfPhases, Phase } from "@/types/types";
import { useConfigStore } from "@/stores/config";
import { setLivePhases } from "@/services/configService";
import { onMounted, ref } from "vue";

const config = useConfigStore();

const { closeEditor } = defineProps<{
  closeEditor: () => void;
}>();

type PhaseSetting = {
  phase: Phase;
  active: boolean;
};
const phases = ref<PhaseSetting[]>([]);

onMounted(() => {
  for (const phase of listOfPhases()) {
    phases.value.push({
      phase: phase,
      active: useConfigStore().public.livePhases.includes(phase.toString()),
    });
  }
});

const setAllPhases = (setting: boolean) => {
  for (const phaseSetting of phases.value) {
    phaseSetting.active = setting;
  }
};
const submitLivePhases = async () => {
  let livePhases: Phase[] = [];
  for (const phaseSetting of phases.value) {
    if (phaseSetting.active) {
      livePhases.push(phaseSetting.phase);
    }
  }

  try {
    await setLivePhases(livePhases);
    closeEditor();
  } catch (e) {
    config.updatePublicConfig();
    alert("There was a problem in saving live phases");
  }
};
</script>

<template>
  <div class="checkboxes">
    <label v-for="phaseSetting in phases" :key="phaseSetting.phase">
      <span>
        <input type="checkbox" v-model="phaseSetting.active" />
        {{ phaseSetting.phase }}
      </span>
    </label>
  </div>

  <div class="submitChanges">
    <div>
      <button @click="setAllPhases(true)" class="small">Enable all</button>
      <button @click="setAllPhases(false)" class="small">Disable all</button>
    </div>
    <button @click="submitLivePhases">Submit</button>
    <p><em>This will not effect admin submissions</em></p>
  </div>
</template>

<style scoped>
.checkboxes {
  display: flex;
  flex-direction: column;
}
.submitChanges {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.submitChanges > * {
  margin: 5px;
}
</style>

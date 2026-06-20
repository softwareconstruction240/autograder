<script setup lang="ts">
import { useConfigStore } from "@/stores/config";
import { ref } from "vue";
import { setPenalties } from "@/services/configService";

const { closeEditor } = defineProps<{
  closeEditor: () => void;
}>();

const config = useConfigStore();

const latePenalty = ref<number>(Math.round(config.admin.penalty.perDayLatePenalty * 100));
const maxLateDays = ref<number>(config.admin.penalty.maxLateDaysPenalized);
const gitPenalty = ref<number>(Math.round(config.admin.penalty.gitCommitPenalty * 100));
const linesChangedPerCommit = ref<number>(config.admin.penalty.linesChangedPerCommit);
const clockForgivenessMinutes = ref<number>(config.admin.penalty.clockForgivenessMinutes);

const valuesReady = () => {
  return (
    gitPenalty.value >= 0 &&
    gitPenalty.value <= 100 &&
    latePenalty.value >= 0 &&
    latePenalty.value <= 100 &&
    maxLateDays.value >= 0 &&
    linesChangedPerCommit.value >= 0 &&
    clockForgivenessMinutes.value >= 0
  );
};

const submit = async () => {
  try {
    await setPenalties(
      maxLateDays.value,
      gitPenalty.value / 100,
      latePenalty.value / 100,
      linesChangedPerCommit.value,
      clockForgivenessMinutes.value,
    );

    closeEditor();
  } catch (e) {
    config.updateAdminConfig();
    alert("There was a problem saving the penalties");
  }
};
</script>

<template>
  <div class="penalty">
    <div class="value">
      <p class="valueName">Late Penalty</p>
      <p class="valueDescription">Applied per day the submission is late.</p>
      <p><input type="number" v-model="latePenalty" />%</p>
    </div>
    <div class="value">
      <p class="valueName">Max Late Days</p>
      <p class="valueDescription">Days after which the late penalty caps out.</p>
      <p><input type="number" v-model="maxLateDays" /> days</p>
    </div>
  </div>

  <hr style="width: 100%; color: black; margin-top: 10px" />

  <div class="penalty">
    <div class="value">
      <p class="valueName">Git Commit Penalty</p>
      <p class="valueDescription">
        Applied when students don't have enough commits and a TA determines they should receive a
        penalty.
      </p>
      <p><input type="number" v-model="gitPenalty" />%</p>
    </div>
    <div class="value">
      <p class="valueName">Lines Changed Per Commit</p>
      <p class="valueDescription">
        The minimum number of lines that must change for a commit to count.
      </p>
      <p><input type="number" v-model="linesChangedPerCommit" /> lines</p>
    </div>
    <div class="value">
      <p class="valueName">Clock Forgiveness</p>
      <p class="valueDescription">
        The number of minutes in the future we will tolerate local clock non-synchronization in Git
        Commit Verification.
      </p>
      <p><input type="number" v-model="clockForgivenessMinutes" /> minutes</p>
    </div>
  </div>

  <button :disabled="!valuesReady()" @click="submit">Submit</button>
  <p v-if="!valuesReady()" style="color: red">
    <em>All values must be non-negative, and penalties must be equal to or less than 100%</em>
  </p>
  <p><em>None of these values affect admin submissions</em></p>
</template>

<style scoped>
.valueName {
  font-weight: bold;
}
.valueDescription {
  font-style: italic;
}
.value {
  margin-top: 5px;
}
button {
  margin-top: 15px;
}
input {
  max-width: 75px;
}
</style>

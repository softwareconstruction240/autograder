<script setup lang="ts">
import { isPlausibleRepoUrl } from "@/utils/utils";
import { defineEmits, reactive } from "vue";
import { adminUpdateRepoPatch, studentUpdateRepoPatch } from "@/services/userService";
import type { User } from "@/types/types";
import { useAuthStore } from "@/stores/auth";

const { user } = defineProps<{
  user: User | null;
}>();

defineEmits({
  repoEditSuccess: null,
});

let newRepoUrl = reactive({
  value: "",
});
let waitingForRepoCheck = reactive({ value: false });
let success = reactive({ value: false });

const submitAndCheckRepo = async (sendEmit: (event: any) => void) => {
  success.value = false;
  waitingForRepoCheck.value = true;

  try {
    if (useAuthStore().user?.role == "ADMIN" && user != null) {
      await adminUpdateRepoPatch(newRepoUrl.value, user.netId);
    } else {
      await studentUpdateRepoPatch(newRepoUrl.value);
    }
  } catch (error) {
    if (error instanceof Error) {
      alert("Failed to save the Github Repo: \n" + error.message);
    } else {
      alert("Unknown error updating Github Repo");
    }
    waitingForRepoCheck.value = false;
    return;
  }

  waitingForRepoCheck.value = false;
  success.value = true;
  sendEmit("repoEditSuccess");
};
</script>

<template>
  <div>
    <p><em>Please enter the GitHub Repo link here:</em></p>
    <input v-model="newRepoUrl.value" type="text" id="repoUrlInput" placeholder="Github Repo URL" />
    <button
      :disabled="waitingForRepoCheck.value || !isPlausibleRepoUrl(newRepoUrl.value)"
      class="primary"
      @click="submitAndCheckRepo($emit)"
    >
      Submit and Save
    </button>
    <p v-if="waitingForRepoCheck.value">Verifying repo URL... please wait...</p>
    <p v-else-if="success.value">Repo successfully saved!</p>
    <div id="urlTips">
      <p>The url should look something like this:</p>
      <p><em>https://github.com/{username}/{name_of_project}</em></p>
    </div>
  </div>
</template>

<style scoped>
#repoUrlInput {
  width: 80%;
  padding: 10px;
  margin-right: 10px;
}
#urlTips {
  font-size: smaller;
  flex-direction: column;
}
button {
  margin: 5px;
}
</style>

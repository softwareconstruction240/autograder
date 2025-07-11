<script setup lang="ts">
import { ref } from "vue";
import { setSlackLink } from "@/services/configService";
import { useConfigStore } from "@/stores/config";

const config = useConfigStore();

const { closeEditor } = defineProps<{
  closeEditor: () => void;
}>();

const valueReady = (): boolean => {
  return slackLink.value.length !== 0;
};

const slackLink = ref<string>(config.public.slackLink);

const submitSlackLink = async () => {
  try {
    await setSlackLink(slackLink.value);
    closeEditor();
  } catch (e) {
    config.updatePublicConfig();
    alert("There was a problem in saving the slack link");
  }
};
</script>

<template>
  <p>
    Set the URL where users will be redirected to the Slack page for the current semester or term.
    This redirect will occur when a user visits:
    <br /><em>https://cs240.click/slack</em>
  </p>
  <input v-model="slackLink" type="text" placeholder="No Slack Link" />

  <button :disabled="!valueReady()" @click="submitSlackLink">Submit</button>
</template>

<style scoped>
button {
  margin-top: 10px;
}
</style>

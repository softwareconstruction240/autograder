<script lang="ts">
import { ref } from "vue";
import { useConfigStore } from "@/stores/config";

const config = useConfigStore();

const { closeEditor } = defineProps<{
  closeEditor: () => void;
}>();

const slackLink = ref<string>("");

const submitSlackLink = async () => {
  try {
    //TODO: await setSlackLink(slackLink);
    closeEditor();
  } catch (e) {
    config.updatePublicConfig();
    alert("There was a problem in saving the slack link");
  }
}

</script>

<template>
  <p>Top Text: Fix this!</p>
</template>

<style scoped></style>

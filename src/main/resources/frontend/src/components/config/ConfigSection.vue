<script setup lang="ts">
/**
 * A reusable wrapper component that provides a consistent interface for editable configuration sections.
 * Each section includes a title, description, current value display, and an editable popup interface.
 *
 * All editors should take a function as a prop for closing the editor popup. The function is provided by
 * the ConfigSection component by decomposition.
 *
 * <template #editor="{ closeEditor }"> gives any element inside the template tag access to the `closeEditor()`
 * function, which will close the editor popup.
 *
 * ConfigSection will automatically reload the config from the server each time an editor is opened, to ensure
 * the admin has the most upto date config
 *
 * @example
 * <ConfigSection
 *   title="Banner Message"
 *   description="A dynamic message displayed across the top of the Autograder"
 * >
 *   <template #current>
 *     <p>Current message: {{ currentMessage }}</p>
 *   </template>
 *   <template #editor="{ closeEditor }">
 *     <BannerConfigEditor :closeEditor="closeEditor"/>
 *   </template>
 * </ConfigSection>
 */

import { ref } from 'vue'
import PopUp from '@/components/PopUp.vue'
import { useAppConfigStore } from '@/stores/appConfig'

defineProps<{
  title: string
  description: string
}>()

const editorPopup = ref<boolean>(false);

const openEditor = () => {
  useAppConfigStore().updateConfig()
  editorPopup.value = true
}

const closeEditor = () => {
  editorPopup.value = false;
}
</script>

<template>
  <section class="config-section">
    <h3 @click="openEditor" style="cursor: pointer">{{ title }} <i class="fa-solid fa-pen-to-square"/></h3>
    <p>{{ description }}</p>
    <slot name="current"/>
    <PopUp
      v-if="editorPopup"
      @closePopUp="editorPopup = false">
      <h3>Edit {{ title }}</h3>
      <slot name="editor" :closeEditor="closeEditor"/>
    </PopUp>
  </section>
</template>

<style scoped>
.config-section {
  padding: 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 0.5rem;
  background-color: white;
}
</style>

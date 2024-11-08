<script setup lang="ts">
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
  useAppConfigStore().updateConfig()
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

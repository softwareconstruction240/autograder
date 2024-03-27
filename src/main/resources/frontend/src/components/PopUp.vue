<script setup lang="ts">
import { defineEmits } from 'vue';

defineEmits({
  closePopUp: null,
});
</script>

<template>
  <div class="popup-overlay" @click="$emit('closePopUp')">
    <div class="popup" @click.stop>
      <button @click="$emit('closePopUp')" class="close-button">X</button>
      <div class="popup-content">
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped>
.popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
}

.popup {
  background: var(--plain-100);
  padding: 10px;
  border-radius: 8px;
  position: fixed;
  max-height: 90vh;
  max-width: 95vw;
  overflow-y: auto;
  overflow-x: hidden;
}

.popup-content {
  margin-bottom: 10px;
  padding: 20px;
}

.close-button {
  position: absolute;
  top: 10px;
  right: 10px;
  cursor: pointer;
  background-color: var(--opposite);
}
</style>

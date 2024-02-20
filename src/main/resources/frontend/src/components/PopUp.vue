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
}

.popup {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  position: relative;
  max-height: 80vh;
  overflow-y: auto;
}

.popup-content {
  margin-bottom: 20px;
  padding: 20px;
}

.close-button {
  position: absolute;
  top: 10px;
  right: 10px;
  cursor: pointer;
}
</style>

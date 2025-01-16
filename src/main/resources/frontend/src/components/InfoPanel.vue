<script setup lang="ts"></script>

<template>
  <div class="info-panel-container">
    <slot />
  </div>
</template>

<style scoped>
.info-panel-container {
  align-items: center;
  margin: 10px;
  width: 100%;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  background-color: #bdbdbd;
  display: flex;
  flex-direction: column;
}
</style>

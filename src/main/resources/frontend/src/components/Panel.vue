<script setup lang="ts"></script>

<template>
  <div class="panel-content-container">
    <slot />
  </div>
</template>

<style scoped>
.panel-content-container {
  margin: 10px;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  background-color: #f2f2f2;
}
</style>

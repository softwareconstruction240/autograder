<script setup lang="ts">

import {computed} from "vue";
import {useAuthStore} from "@/stores/auth";

const greeting = computed(() => {
  if (useAuthStore().isLoggedIn) {
    return `${useAuthStore().user?.firstName} ${useAuthStore().user?.lastName} - ${useAuthStore().user?.netId} (${useAuthStore().user?.role.toLowerCase()})`
  }
});
</script>

<template>
  <header>
    <h1>CS 240 Autograder</h1>
    <h3>This is where you can submit your assignments and view your scores.</h3>
    <p>{{ greeting }}</p>
  </header>
  <main>
    <router-view/>
  </main>
</template>

<style scoped>
header {
  text-align: center;

  margin-bottom: 20px;

  padding: 20px;

  width: 100%;

  background-color: var(--color--secondary--background);
  color: var(--color--secondary--text);
}

h1 {
  font-weight: bold;
}

main {
  background-color: var(--color--surface--background);
  color: var(--color--surface--text);
  padding: 20px;
  border-radius: 3px;

  width: 66vw;

  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.2);
}
</style>

<script setup lang="ts">
import {useAppConfigStore} from "@/stores/appConfig";

const login = () => {
  window.location.href = useAppConfigStore().backendUrl + '/auth/login';
}
</script>

<template>
  <h2>You must log in before continuing</h2>
  <p>After selecting Log In, you will be redirected to BYU's authentication page.</p>
  <button @click="login">Log In</button>
</template>

<style scoped>
</style>
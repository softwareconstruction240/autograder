<script setup lang="ts">
import {useAppConfigStore} from "@/stores/appConfig";
import {onBeforeMount} from 'vue';
import {meGet} from "@/services/authService";
import {useAuthStore} from "@/stores/auth";
import router from "@/router";

onBeforeMount(async () => {
  const loggedInUser = await meGet()
  if (loggedInUser == null)
    return;

  if (loggedInUser === 403) {
    await router.push({name: 'register'});
    return;
  }

  useAuthStore().user = loggedInUser;
  await router.push({name: 'home'});
})

const login = () => {
  window.location.href = useAppConfigStore().backendUrl + '/auth/login';
}
</script>

<template>
  <div id="login-content">
    <h2>You must log in before continuing</h2>
    <p>After selecting Log In, you will be redirected to BYU's authentication page.</p>
    <button @click="login">Log In</button>
  </div>
</template>

<style scoped>
#login-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

button {
  margin-top: 1rem;
}
</style>
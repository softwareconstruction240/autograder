<script setup lang="ts">
import { useConfigStore } from "@/stores/config";
import { onBeforeMount, onMounted } from "vue";
import { loadUser } from "@/services/authService";
import router from "@/router";

onBeforeMount(async () => {
  await loadUser();
  await router.push({ name: "home" });
});

const login = () => {
  window.location.href = useConfigStore().backendUrl + "/auth/login";
};

onMounted(() => {
  const urlParams = new URLSearchParams(window.location.search);
  const error = urlParams.get("error");
  // clear the url params
  window.history.replaceState({}, document.title, window.location.pathname);
  if (error != null) alert(error);
});
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

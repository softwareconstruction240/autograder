<script setup lang="ts">

import {onBeforeMount, ref} from "vue";
import {meGet, registerPost} from "@/services/authService";
import router from "@/router";

onBeforeMount(async () => {
  const loggedInUser = await meGet()
  if (loggedInUser === 403)
    return;

  if (loggedInUser === null) {
    await router.push({name: 'login'});
    return;
  }

  await router.push({name: 'home'});
})

const firstName = ref('');
const lastName = ref('');
const repoUrl = ref('');

async function register() {
  const success = await registerPost(firstName.value, lastName.value, repoUrl.value);

  if (success) {
    await router.push('/');
    return;
  }

  alert('Registration failed. Please try again.');
}

</script>

<template>
  <div id="register-content">
    <h1>Create an account for the autograder</h1>
    <p>It looks like this is your first time logging in. Fill in the information below to create a profile. Don't worry,
      you will be able to change this later.</p>
    <div class="form">
      <label for="first-name">First Name:</label>
      <input
          type="text"
          id="first-name"
          name="first-name"
          v-model="firstName"
          :class="{required: firstName.length == 0 }">

      <label for="last-name">Last Name:</label>
      <input
          type="text"
          id="last-name"
          name="last-name"
          v-model="lastName"
          :class="{required: lastName.length == 0 }">

      <label for="repo-url">Repository URL:</label>
      <input
          type="url"
          id="repo-url"
          name="repo-url"
          v-model="repoUrl"
          :class="{required: repoUrl.length == 0 }"
          placeholder="https://github.com/cosmo/chess.git">

      <button
          id="register-btn"
          :disabled="firstName.length == 0 || lastName.length == 0 || repoUrl.length == 0"
          @click="register">
        Register
      </button>
    </div>
  </div>
</template>

<style scoped>
#register-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.form {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 50%;
}

label {
  margin-top: 1rem;
}

input {
  margin-top: 0.5rem;
  width: 100%;
  padding: 0.5rem;
  border-radius: 3px;
  border: 1px solid var(--color--secondary--background);
}

button {
  margin-top: 1rem;
}

.required {
  outline: none;
  border: 1px solid var(--color--text--error);
}
</style>
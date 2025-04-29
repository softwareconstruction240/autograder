<script setup lang="ts">
import { computed } from "vue";
import { useAuthStore } from "@/stores/auth";
import { useConfigStore } from "@/stores/config";

const authStore = useAuthStore();
const config = useConfigStore();

const bannerProps = computed(() => {
  if (authStore.isLoggedIn) {
    return {
      message: config.public.banner.message,
      link: config.public.banner.link,
      color: config.public.banner.color,
    };
  }
  return {};
});

const handleClick = () => {
  if (bannerProps.value.link) {
    window.open(bannerProps.value.link, "_blank");
  }
};
</script>

<template>
  <div
    v-if="bannerProps.message"
    id="bannerMessage"
    :style="{
      backgroundColor: bannerProps.color,
      cursor: bannerProps.link ? 'pointer' : 'default',
    }"
    @click="handleClick"
  >
    <span v-text="bannerProps.message" />
  </div>
</template>

<style scoped>
#bannerMessage {
  width: 100%;
  border-radius: 3px;
  padding: 7px;
  margin-top: 15px;
}
</style>

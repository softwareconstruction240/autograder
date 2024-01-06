<script setup lang="ts">
import {provide, ref} from "vue";
import {activeTabHashKey, addTabKey, type Tab as TabType} from "@/components/tabs/TabInjectionKeys";

const activeTabHash = ref("");
const tabs = ref<TabType[]>([]);

provide(addTabKey, (tab) => {
  const count = tabs.value.push(tab);

  if (count === 1) {
    activeTabHash.value = tab.hash;
  }
});
provide(activeTabHashKey, activeTabHash);
</script>

<template>
  <div>
    <ul>
      <li v-for="tab in tabs" :key="tab.title" @click="activeTabHash = tab.hash">
        {{ tab.title }}
      </li>
    </ul>
    <slot/>
  </div>
</template>

<style scoped>

</style>
<script setup lang="ts">
import {inject, onBeforeMount, ref, watch} from "vue";
import {activeTabHashKey, addTabKey, type Tab as TabType} from "@/components/tabs/TabInjectionKeys";
const props = defineProps<{
  title: string;
  disabled?: boolean;
}>();

const hash = ref("");
const isActive = ref(false);

const addTab = inject<(tab: TabType) => void>(addTabKey);
const activeTabHash = inject(activeTabHashKey) || ref("");

onBeforeMount(() => {
  hash.value = `#${props.title.toLowerCase().replace(/ /g, "-")}`;

  addTab?.({
    title: props.title,
    hash: hash.value,
    disabled: props.disabled
  });
});

watch(activeTabHash, () => {
  isActive.value = activeTabHash.value === hash.value;
});

</script>

<template>
  <div v-show="isActive">
    <slot />
  </div>
</template>

<style scoped>

</style>
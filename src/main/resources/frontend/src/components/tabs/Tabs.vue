<script setup lang="ts">
import { provide, ref } from 'vue'
import {
  activeTabHashKey,
  addTabKey,
  type Tab as TabType
} from '@/components/tabs/TabInjectionKeys'

const activeTabHash = ref('')
const tabs = ref<TabType[]>([])

provide(addTabKey, (tab) => {
  const count = tabs.value.push(tab)

  if (count === 1) {
    activeTabHash.value = tab.hash
  }
})
provide(activeTabHashKey, activeTabHash)

const setActiveTabHash = (tab: TabType) => {
  if (!tab.disabled) activeTabHash.value = tab.hash
}
</script>

<template>
  <div>
    <ul>
      <li
        v-for="tab in tabs"
        :key="tab.title"
        @click="setActiveTabHash(tab)"
        :class="{ active: tab.hash === activeTabHash, disabled: tab.disabled }"
      >
        {{ tab.title }}
      </li>
    </ul>
    <slot />
  </div>
</template>

<style scoped>
ul {
  list-style: none;
  display: flex;
  justify-content: center;
  padding: 0;
  margin: 0;
}

li {
  cursor: pointer;
  padding: 8px;
  color: var(--color--surface--text);

  font-size: 1.2rem;
  font-weight: bold;

  border-bottom: 1px solid var(--color--accent);

  user-select: none;
}

li:not(.active):hover {
  background-color: #dadada;
}

li.active {
  filter: brightness(0.9);
  border-bottom: none;
  border-left: 1px solid var(--color--accent);
  border-right: 1px solid var(--color--accent);
  border-top: 1px solid var(--color--accent);
}

.disabled {
  color: #dadada;
  cursor: not-allowed;
}
</style>

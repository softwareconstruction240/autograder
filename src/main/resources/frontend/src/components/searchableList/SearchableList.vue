<script setup lang="ts">

import {computed, ref} from "vue";
import type {ItemType} from "@/components/searchableList/ItemType";

const props = defineProps<{
  items: ItemType[];
}>();

defineEmits<{
  itemSelected: [item: ItemType]
}>();

const filterText = ref('')

const filteredItems = computed(() => {
  return props.items.filter(item => item.label.includes(filterText.value))
});
</script>

<template>
  <div class="container">
    <div class="search-container">
      <input v-model="filterText" type="text" placeholder="Search..."/>
    </div>
    <div>
      <ul>
        <li
            v-for="item in filteredItems"
            :key="item.label"
            @click="$emit('itemSelected', item)">
          {{ item.label }}
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.search-container {
  /* center */
  display: flex;
  justify-content: center;
}

input[type="text"] {
  border: 1px solid #ccc;
}

ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

li {
  cursor: pointer;
  background-color: white;
  text-align: center;
}

li:not(:first-child) {
  margin-top: 5px;
}

li:hover {
  background-color: #eee;
}
</style>
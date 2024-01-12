<script setup lang="ts">

import {computed, ref} from "vue";
import type {ItemType} from "@/components/searchableList/ItemType";

const props = defineProps<{
  items: ItemType[];
}>();

defineEmits<{
  itemSelected: [item: any]
}>();

const filterText = ref('')

const filteredItems = computed(() => {
  return props.items.filter(item => item.label.includes(filterText.value))
});

const isInputFocused = ref(false);
const isHoverOverList = ref(false);
</script>

<template>
  <div class="container">
    <input
        v-model="filterText"
        type="text" placeholder="Search..."
        @focus="() => isInputFocused = true"
        @blur="() => isInputFocused = false"
    />
    <div
        class="suggestions-container"
        :class="{hidden: !isInputFocused && !isHoverOverList}"
    >
      <ul @mouseenter="() => isHoverOverList = true"
          @mouseleave="() => isHoverOverList = false">
        <li
            v-for="item in filteredItems"
            :key="item.label"
            @click="$emit('itemSelected', item.item)">
          {{ item.label }}
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>

.container {
  position: relative;
}

.suggestions-container {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background-color: white;
  border: 1px solid #ccc;
  border-top: none;
  z-index: 1;
}

.hidden {
  display: none;
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
  padding: 2px;

}

li:not(:first-child) {
  margin-top: 5px;
}

li:hover {
  background-color: #eee;
}

</style>
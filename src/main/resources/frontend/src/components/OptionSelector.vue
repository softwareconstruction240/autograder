<script setup lang="ts">
import { defineEmits, ref } from 'vue'

const { options } = defineProps<{
  options: String[];
}>();

defineEmits({
  optionSelected: String,
});

const current = ref<String | null>(null)

const select = (option: String) => {
  current.value = option
}
</script>

<template>
  <div id="options">
    <p v-for="option in options"
       :key="option as string"
       class="option"
       :class="{ 'active': option === current }"
       @click=" () => {
         select(option)
         $emit('optionSelected', option)
       }"
    >
      {{option}}
    </p>
  </div>
</template>

<style>
#options {
  width: 100%;
  max-width: 600px;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 0 10px;
}
.option {
  width: 12%;
  aspect-ratio: 1/1;
  background-color: #8d8f99;
  font-weight: bold;
  font-size: 40px;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 2px solid #54555b;
  border-radius: 10px;
  transition: background-color, border-color;
  transition-duration: 0.2s;
}

#options > *:hover {
  background-color: #b2c4ff;
  border-color: #85a8ff;
  transition: background-color, border-color;
  transition-duration: 0.2s;
}

#options > *.active {
  background-color: #4a8dff;
  border-color: #0080ff;
}

@media screen and (max-width: 500px) {
  #options > * {
    font-size: 30px; /* Font size for smaller screens */
  }
}
</style>
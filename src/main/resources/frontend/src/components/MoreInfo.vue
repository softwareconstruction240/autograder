<script setup lang="ts">
import PopUp from '@/components/PopUp.vue'
import { ref } from 'vue'
const openInfo = ref<boolean>(false)

defineProps<{
  text?: string;
  iconSize?: number;
}>();

</script>

<template>
  <div
    class="clickable-style"
    id="infoIcon"
    @click="openInfo = true">
    <i
      class="fa-solid fa-circle-info"
      :style="{fontSize: `${iconSize ? iconSize : 30}px`}"
    />
    <p v-if="text" v-html="text"/>
  </div>
  <PopUp v-if="openInfo"
         @closePopUp="openInfo = false">
    <div id="slotInfo">
      <slot/>
    </div>
  </PopUp>
</template>

<style>
#infoIcon {
  display: flex;
  flex-direction: column;
  align-items: center;
}
#slotInfo {
  display: flex;
  flex-direction: column;
  align-items: start;
  text-align: left;
}
#slotInfo p a {
  padding: 3px;
}
</style>
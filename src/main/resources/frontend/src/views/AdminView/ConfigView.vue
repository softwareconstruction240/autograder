<script setup lang="ts">
import { ref } from 'vue'
import { Phase } from '@/types/types'
import PopUp from '@/components/PopUp.vue'

const openLivePhases = ref<boolean>(false);
const banana = ref<boolean>(true);
</script>

<template>
  <div id="configContainer">
    <div class="configCategory">
      <h3>Canvas Integration</h3>
      <p><span class="infoDescription">Course ID:</span> 234563</p>

      <h4>Assignments</h4>
      <div v-for="phase in Object.values(Phase).filter((v) => isNaN(Number(v)))">
        <p><span class="infoDescription">{{phase}} Assignment ID:</span> 234563</p>
        <p><span class="infoDescription">-Quality Rubric ID:</span> 34543</p>
        <p><span class="infoDescription">-Git Rubric ID:</span> 34543</p>
        <p><span class="infoDescription">-Main Rubric ID:</span> 34543</p>
      </div>
      <button>Change</button>
    </div>

    <div class="configCategory">
      <h3>Live Phases</h3>
      <p>These are the phases are live and open for students to submit to</p>
      <div v-for="phase in Object.values(Phase).filter((v) => isNaN(Number(v)))">
        <p><i class="fa-solid fa-circle-check" style="color: green"/> {{phase}}</p>
      </div>
      <button @click="openLivePhases = true">Update</button>
    </div>

    <div class="configCategory">
      <h3>Banner message</h3>
      <p>There is currently no banner message</p>
      <button>Change</button>
    </div>
  </div>

  <PopUp
    v-if="openLivePhases"
    @closePopUp="openLivePhases = false">
    <h3>Live Phases</h3>
    <p>Enable student submissions for the following phases:</p>
    <div v-for="phase in Object.values(Phase).filter((v) => isNaN(Number(v)))">
      <p><input type="checkbox" id="vehicle1" name="vehicle1" value="Bike"> {{phase}}</p>
    </div>
    <div class="submitChanges">
      <p><em>This will not effect admin submissions</em></p>
      <div>
        <button class="small">Enable all</button> <button class="small">Disable all</button>
      </div>
      <button @click="() => {
        openLivePhases = false;
      }">Submit Changes</button>
    </div>
  </PopUp>
</template>

<style scoped>
.submitChanges {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.submitChanges >* {
  margin: 5px;
}
.infoDescription {
  font-weight: bold;
}

#configContainer {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr; /* Three columns of 100px each */
  grid-gap: 10px; /* Gap between grid items */
  margin: 10px;
}
</style>
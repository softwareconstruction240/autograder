<script setup lang="ts">
import type { RubricItem } from '@/types/types'
import { ref } from 'vue'
import PopUp from '@/components/PopUp.vue'
import { generateResultsHtmlString, generateResultsHtmlStringFromText } from '@/utils/utils'
import MoreInfo from '@/components/MoreInfo.vue'

defineProps<{
  rubricItem: RubricItem;
}>();

const areErrorDetailsOpen = ref<boolean>(false)
</script>

<template>
  <div id="rubric-item-container">

    <p class="itemHeader">
      <span id="category" v-html="rubricItem.category + ' '"/>
      <span id="score" v-html="Math.round(rubricItem.results.score) + '/' + rubricItem.results.possiblePoints"/>
    </p>
    <hr/>
    <div id="details">
      <div class="rubricDetails">
        <h4>Requirements</h4>
        <p v-html="rubricItem.criteria"/>
      </div>
      <div class="rubricDetails">
        <h4>Result Notes</h4>
        <p v-html="generateResultsHtmlStringFromText(rubricItem.results.notes)"/>
      </div>

      <MoreInfo text="details">
        <div>
          <span id="category" v-html="rubricItem.category + ' '"/>
          <span id="score" v-html="Math.round(rubricItem.results.score) + '/' + rubricItem.results.possiblePoints + '<br/>'"/>
        </div>
        <hr style="min-width: 250px; width: 100%"> <!-- the min-width is a round about way to make the window wide enough the button -->
        <span v-html="generateResultsHtmlString(rubricItem)"/>

        <div class="itemHeader" id="programErrorWarning" v-if="rubricItem.results.testResults && rubricItem.results.testResults.error" >
          <h3 class="failure">Your program produced errors</h3>
          <button id="errorLogButton" @click="areErrorDetailsOpen = true">View error output</button>
        </div>
      </MoreInfo>

    </div>
  </div>

  <PopUp v-if="areErrorDetailsOpen"
         @closePopUp="areErrorDetailsOpen = false">
    <p id="category">Program Error Output</p>
    <hr>
    <span class="failure">{{rubricItem.results.testResults.error}}</span>
  </PopUp>
</template>

<style scoped>
#programErrorWarning {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  text-align: center;
}
#errorLogButton {
  font-size: 15px;
  margin-top: 10px;
}
#category {
  font-weight: 700;
  font-size: 20px;
}

#score {
  font-weight: normal;
}

#details {
  display: flex;
}

#details > .rubricDetails {
  width: 45%;
  margin: 0 5px;
}

#moreDetails i {
  font-size: 30px;
}

.itemHeader {
  margin-top: 10px;
}
</style>
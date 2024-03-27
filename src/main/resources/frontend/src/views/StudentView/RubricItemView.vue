<script setup lang="ts">
import type { RubricItem } from '@/types/types'

defineProps<{
  rubricItem: RubricItem;
}>();
</script>

<template>
<div id="rubric-item-container">
  <p id="item-header">
    <span id="category" v-html="rubricItem.category + ' '"/>
    <span id="score" v-html="Math.round(rubricItem.results.score) + '/' + rubricItem.results.possiblePoints"/>
  </p>
  <hr/>
  <div id="details">
    <div>
      <h4>Requirements</h4>
      <p v-html="rubricItem.criteria"/>
    </div>
    <div>
      <h4>Result Notes</h4>
      <p v-html="rubricItem.results.notes"/>
    </div>
  </div>
</div>
</template>

<style scoped>
#category {
  font-weight: 700;
  font-size: 20px;
}

#score {
  font-weight: normal;
}

#details {
  display: grid;
  grid-template-columns: 1fr 2fr;
  column-gap: 5px;
}

#item-header {
  margin-top: 10px;
}
</style>
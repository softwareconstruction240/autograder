<script setup lang="ts">
import type {Rubric} from "@/types/types";

defineProps<{
  rubric: Rubric;
}>();

</script>

<template>
  <div class="rubric-container">
    <table class="rubric-table">
      <tr>
        <th>Category</th>
        <th>Criteria</th>
        <th>Points</th>
        <th>Results</th>
      </tr>
      <tr v-if="rubric.passoffTests">
        <td>{{rubric.passoffTests.category}}</td>
        <td>{{rubric.passoffTests.criteria}}</td>
        <td>{{rubric.passoffTests.results.score}}</td>
        <td>Click here</td>
      </tr>
      <tr v-if="rubric.unitTests">
        <td>{{rubric.unitTests.category}}</td>
        <td>{{rubric.unitTests.criteria}}</td>
        <td>{{rubric.unitTests.results.score}}</td>
        <td>Click here</td>
      </tr>
      <tr v-if="rubric.styleTests">
        <td>{{rubric.styleTests.category}}</td>
        <td>{{rubric.styleTests.criteria}}</td>
        <td>{{rubric.styleTests.results.score}}</td>
        <td>Click here</td>
      </tr>
    </table>
  </div>
</template>

<style scoped>

table, th, td {
  border: 2px solid #ddd;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  background-color: #e3e2e2;
  padding: 8px;
  text-align: left;
}

td {
  padding: 8px;
}
</style>
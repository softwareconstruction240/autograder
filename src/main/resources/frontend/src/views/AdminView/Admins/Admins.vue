<script setup lang="ts">
import {usersGet} from "@/services/adminService";
import {useAdminStore} from "@/stores/admin";
import SearchableList from "@/components/searchableList/SearchableList.vue";
import type {ItemType} from "@/components/searchableList/ItemType";

usersGet().then((users) => {
  useAdminStore().users = users;
});

const itemSelected = (item: ItemType) => {
}


</script>

<template>
  <div id="container">
    <div>
      <SearchableList
          :items="useAdminStore().students.map(student => ({label: student.netId}))"
          @itemSelected="itemSelected"
      />
    </div>
    <div>
      <ul>
        <li v-for="admin in useAdminStore().admins" :key="admin.netId" >
<!--          {{ admin.firstName }} {{ admin.lastName }}-->
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
/* two columns */
#container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-gap: 10px;
}
</style>
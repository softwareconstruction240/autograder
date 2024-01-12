<script setup lang="ts">
import {userPatch} from "@/services/adminService";
import {useAdminStore} from "@/stores/admin";
import SearchableList from "@/components/searchableList/SearchableList.vue";
import type {User} from "@/types/types";

useAdminStore().updateUsers();

const makeAdmin = async (user: User) => {
  await userPatch({
    netId: user.netId,
    role: 'ADMIN'
  })
  await useAdminStore().updateUsers();
}


</script>

<template>
  <div class="container">
    <div>
      <p class="search-label-text">Selecting a user from the list will make them an admin.</p>
      <SearchableList
          :items="useAdminStore().students.map(student => (
              {
                label: `${student.firstName} ${student.lastName} - ${student.netId}`,
                item: student
              }))"
          @itemSelected="makeAdmin"
      />
    </div>
    <div>
      <ul>
        <li v-for="admin in useAdminStore().admins.sort(
            (user1, user2) => user1.firstName < user2.firstName ? -1 : 1)"
            :key="admin.netId">
          {{ admin.firstName }} {{ admin.lastName }} ({{ admin.netId }})
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
/* two columns */
.container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-gap: 10px;

  padding: 5px;
}

.search-label-text {
  font-size: 1rem;
  font-weight: bold;

  text-align: start;
}
</style>
<script setup lang="ts">
import {userPatch} from "@/services/adminService";
import {useAdminStore} from "@/stores/admin";
import SearchableList from "@/components/searchableList/SearchableList.vue";
import type {User} from "@/types/types";
import {useAuthStore} from "@/stores/auth";

useAdminStore().updateUsers();

const makeAdmin = async (user: User) => {
  await userPatch({
    netId: user.netId,
    role: 'ADMIN'
  })
  await useAdminStore().updateUsers();
}

const removeAdmin = async (user: User) => {
  if (user.netId === useAuthStore().user?.netId) {
    alert('You cannot remove yourself as an admin.');
    return;
  }

  await userPatch({
    netId: user.netId,
    role: 'STUDENT'
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
      <h3>Admins</h3>
      <ul>
        <li v-for="admin in useAdminStore().admins.sort(
            (user1, user2) => user1.firstName < user2.firstName ? -1 : 1)"
            :key="admin.netId">
          <i class="remove"
             @click="() => removeAdmin(admin)">
            X</i>
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

.remove {
  color: red;
  cursor: pointer;
}

.remove:hover {
  color: darkred;
}

ul {
  list-style-type: none;
  padding-left: 5px;
}
</style>
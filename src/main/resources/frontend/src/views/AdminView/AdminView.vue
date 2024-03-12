<script setup lang="ts">

import Tabs from "@/components/tabs/Tabs.vue";
import Tab from "@/components/tabs/Tab.vue";
import SubmissionsView from "@/views/AdminView/SubmissionsView.vue";
import Exceptions from "@/views/AdminView/Exceptions.vue";
import {testStudentModeGet} from "@/services/adminService";
import QueueStatus from "@/views/AdminView/QueueStatus.vue";
import Analytics from "@/views/AdminView/Analytics.vue";
import HonorChecker from "@/views/AdminView/HonorChecker.vue";
import {useAdminStore} from "@/stores/admin";
import Students from "@/views/AdminView/StudentsView.vue";

useAdminStore().updateUsers();

</script>

<template>
  <div class="container">
    <Tabs>
      <Tab title="Submissions">
        <SubmissionsView/>
      </Tab>
      <Tab title="Students">
        <Students/>
      </Tab>
      <Tab title="Queue Status">
        <QueueStatus/>
      </Tab>
      <Tab title="Downloads">
        <Analytics/>
        <HonorChecker/>
      </Tab>
    </Tabs>
  </div>
</template>

<style scoped>

</style>
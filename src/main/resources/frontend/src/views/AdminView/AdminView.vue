<script setup lang="ts">

import Tabs from "@/components/tabs/Tabs.vue";
import Tab from "@/components/tabs/Tab.vue";
import SubmissionsView from "@/views/AdminView/SubmissionsView.vue";
import QueueStatus from "@/views/AdminView/QueueStatus.vue";
import Analytics from "@/views/AdminView/Analytics.vue";
import HonorChecker from "@/views/AdminView/HonorChecker.vue";
import {useAdminStore} from "@/stores/admin";
import StudentsView from "@/views/AdminView/StudentsView.vue";
import LogsView from "@/views/AdminView/LogsView.vue";

useAdminStore().updateUsers();

</script>

<template>
  <div class="container">
    <Tabs>
      <Tab title="Submissions">
        <SubmissionsView/>
      </Tab>
      <Tab title="Students">
        <StudentsView/>
      </Tab>
      <Tab title="Queue Status">
        <QueueStatus/>
      </Tab>
      <Tab title="Logs">
        <LogsView/>
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
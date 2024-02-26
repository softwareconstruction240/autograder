<script setup lang="ts">

import Tabs from "@/components/tabs/Tabs.vue";
import Tab from "@/components/tabs/Tab.vue";
import SubmissionsView from "@/views/AdminView/Submissions.vue";
import Exceptions from "@/views/AdminView/Exceptions.vue";
import {testStudentModeGet} from "@/services/adminService";
import QueueStatus from "@/views/AdminView/QueueStatus.vue";
import Analytics from "@/views/AdminView/Analytics.vue";
import HonorChecker from "@/views/AdminView/HonorChecker.vue";
import {useAdminStore} from "@/stores/admin";
import LiveLogs from "@/views/AdminView/LiveLogs.vue";

useAdminStore().updateUsers();

const activateTestStudentMode = async () => {
  await testStudentModeGet()
  window.location.href = '/';
}
</script>

<template>
  <div class="container">
    <div class="test-student-mode-container">
      <p>Click on the button below to become the test student</p>
      <p>- you will need to log out and back in again to return to admin
        mode</p>
      <p>- you will not be able to enter student mode unless the Test Student has a submission for the GitHub Repository
        assignment on Canvas</p>
      <button @click="activateTestStudentMode">Test Student Mode</button>
    </div>
    <Tabs>
      <Tab title="Submissions">
        <SubmissionsView/>
      </Tab>
      <Tab title="Logs">
        <LiveLogs/>
      </Tab>
      <Tab title="Queue Status">
        <QueueStatus/>
      </Tab>
      <Tab title="Analytics">
        <Analytics/>
      </Tab>
      <Tab title="Honor Checker">
        <HonorChecker/>
      </Tab>
    </Tabs>
  </div>
</template>

<style scoped>
.container {
  min-height: 50vh;
}

.test-student-mode-container {
  margin-bottom: 20px;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  background-color: #f2f2f2;
  cursor: pointer;
}
</style>
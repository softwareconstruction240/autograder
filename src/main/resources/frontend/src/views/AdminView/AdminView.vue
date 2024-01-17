<script setup lang="ts">

import Tabs from "@/components/tabs/Tabs.vue";
import Tab from "@/components/tabs/Tab.vue";
import SubmissionsView from "@/views/AdminView/Submissions.vue";
import Exceptions from "@/views/AdminView/Exceptions.vue";
import AssignmentOptions from "@/views/AdminView/AssignmentOptions.vue";
import Admins from "@/views/AdminView/Admins/Admins.vue";
import {testStudentModeGet} from "@/services/adminService";

const activateTestStudentMode = async () => {
  await testStudentModeGet()
  window.location.href = '/';
}
</script>

<template>
  <div class="container">
    <div class="test-student-mode-container">
      <p>Click here to become the test student. Note, you will need to log out and back in again to return to admin
        mode.</p>
      <button @click="activateTestStudentMode">Test Student Mode</button>
    </div>
    <Tabs>
      <Tab title="Submissions">
        <SubmissionsView/>
      </Tab>
      <Tab title="Exceptions">
        <Exceptions/>
      </Tab>
      <Tab title="Assignment Options">
        <AssignmentOptions/>
      </Tab>
      <Tab title="Admins">
        <Suspense>
          <Admins/>
          <template #fallback>
            <div>Loading...</div>
          </template>
        </Suspense>
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
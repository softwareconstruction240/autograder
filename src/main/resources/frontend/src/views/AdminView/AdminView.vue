<script setup lang="ts">

import Tabs from "@/components/tabs/Tabs.vue";
import Tab from "@/components/tabs/Tab.vue";
import SubmissionsView from "@/views/AdminView/Submissions.vue";
import Exceptions from "@/views/AdminView/Exceptions.vue";
import AssignmentOptions from "@/views/AdminView/AssignmentOptions.vue";
import Admins from "@/views/AdminView/Admins/Admins.vue";
</script>

<template>
  <div class="container">
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
</style>
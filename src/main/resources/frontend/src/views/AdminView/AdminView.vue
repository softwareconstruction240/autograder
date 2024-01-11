<script setup lang="ts">

import Tabs from "@/components/tabs/Tabs.vue";
import Tab from "@/components/tabs/Tab.vue";
import SubmissionsView from "@/views/AdminView/Submissions.vue";
import Exceptions from "@/views/AdminView/Exceptions.vue";
import AssignmentOptions from "@/views/AdminView/AssignmentOptions.vue";
import Admins from "@/views/AdminView/Admins.vue";
</script>

<template>
  <Tabs>
    <Tab title="Submissions">
      <SubmissionsView />
    </Tab>
    <Tab title="Exceptions">
      <Exceptions />
    </Tab>
    <Tab title="Assignment Options">
      <AssignmentOptions />
    </Tab>
    <Tab title="Admins">
      <Admins />
    </Tab>
  </Tabs>
</template>

<style scoped>
</style>
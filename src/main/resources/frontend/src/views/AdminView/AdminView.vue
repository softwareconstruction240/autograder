<script setup lang="ts">

import Tabs from "@/components/tabs/Tabs.vue";
import Tab from "@/components/tabs/Tab.vue";
</script>

<template>
  <Tabs>
    <Tab title="Submissions">
      //view and manage student submissions
    </Tab>
    <Tab title="Exceptions">
      //view and manage student exceptions such as late submissions, insufficient commits, etc.
    </Tab>
    <Tab title="Assignment Options">
      //view and manage assignment options such as due dates, late penalties, etc.
    </Tab>
    <Tab title="Admins">
      //view and manage admins for the course
    </Tab>
  </Tabs>
</template>

<style scoped>
</style>
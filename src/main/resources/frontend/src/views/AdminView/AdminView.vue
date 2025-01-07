<script setup lang="ts">
import Tabs from '@/components/tabs/Tabs.vue';
import Tab from '@/components/tabs/Tab.vue';
import SubmissionsView from '@/views/AdminView/SubmissionsView.vue';
import QueueStatus from '@/views/AdminView/QueueStatus.vue';
import Analytics from '@/views/AdminView/Analytics.vue';
import HonorChecker from '@/views/AdminView/HonorChecker.vue';
import { useAdminStore } from '@/stores/admin';
import StudentsView from '@/views/AdminView/StudentsView.vue';
import ConfigView from '@/views/AdminView/ConfigView.vue';

useAdminStore().updateUsers();
</script>

<template>
  <div class="container">
    <Tabs>
      <Tab title="Submissions">
        <SubmissionsView />
        <QueueStatus />
      </Tab>
      <Tab title="Students">
        <StudentsView />
      </Tab>
      <Tab title="Downloads">
        <Analytics />
        <HonorChecker />
      </Tab>
      <Tab title="Config">
        <ConfigView />
      </Tab>
    </Tabs>
  </div>
</template>

<style scoped>
.container {
  width: 100%;
}
</style>

<template>
  <div class="workspace-view">
    <WorkspaceTabs />
    <div class="workspace-body">
      <WelcomeView v-if="!store.activeTab" embedded />
      <TableWorkspace v-else-if="store.activeTab.type === 'table'" :key="store.activeTab.id" :tab="store.activeTab" />
      <QueryWorkspace v-else :key="store.activeTab.id" :tab="store.activeTab" />
    </div>
  </div>
</template>

<script setup>
import { useWorkspaceStore } from '@/stores/workspace'
import WorkspaceTabs from '@/layout/WorkspaceTabs.vue'
import WelcomeView from '@/views/WelcomeView.vue'
import TableWorkspace from '@/views/workspace/TableWorkspace.vue'
import QueryWorkspace from '@/views/workspace/QueryWorkspace.vue'

const store = useWorkspaceStore()
</script>

<style scoped>
.workspace-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.workspace-body {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.workspace-body > * {
  flex: 1;
  min-width: 0;
}
</style>

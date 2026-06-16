<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import SidePanel from '@/components/SidePanel.vue'
</script>

<template>
  <div class="basic-layout">
    <GlobalHeader />
    <main class="layout-content">
      <router-view />
    </main>
    <SidePanel />
  </div>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: var(--color-bg-warm);
}

.layout-content {
  max-width: 1440px;
  margin: 0 auto;
}
</style>

<script setup lang="ts">
import { useThemeStore } from '@/stores/theme'
// 初始化主题状态，应用保存的主题偏好
useThemeStore()
</script>

<template>
  <router-view />
</template>

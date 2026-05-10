<template>
  <router-view />
</template>

<script setup lang="ts">
import { startAdminTokenMonitor } from '@/api'
startAdminTokenMonitor()
</script>

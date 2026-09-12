<template>
  <router-view />
  <div v-if="showNotificationCenter" class="global-notification-center">
    <NotificationCenter />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { startTokenMonitor } from '@/utils/request'
import { useUserStore } from '@/store/user'
import NotificationCenter from '@/components/NotificationCenter.vue'

const route = useRoute()
const userStore = useUserStore()
const showNotificationCenter = computed(() => {
  const path = route.path.toLowerCase()
  return userStore.isLoggedIn && path !== '/login' && path !== '/register'
})

startTokenMonitor()
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
}

html, body, #app {
  height: 100%;
  font-family: var(--font-sans);
}

body {
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

a {
  color: var(--color-primary);
  text-decoration: none;
}

.global-notification-center {
  position: fixed;
  top: 56px;
  right: 20px;
  z-index: 2000;
}

@media (max-width: 768px) {
  .global-notification-center {
    top: 12px;
    right: 72px;
  }
}

a:hover {
  color: var(--color-primary-light);
}

::selection {
  background: var(--color-primary-lighter);
  color: var(--color-primary-dark);
}
</style>

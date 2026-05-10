<template>
  <router-view />
</template>

<script setup lang="ts">
import { startTokenMonitor } from '@/utils/request'
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

a:hover {
  color: var(--color-primary-light);
}

::selection {
  background: var(--color-primary-lighter);
  color: var(--color-primary-dark);
}
</style>

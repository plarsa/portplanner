<template>
  <div class="app-shell">
    <router-view />
    <footer class="app-footer">Portplanner {{ version }}</footer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useAuthStore } from './stores/auth'
import { getVersion } from './api/version'

const auth = useAuthStore()
const version = ref('')

onMounted(() => {
  auth.checkAuth()
  getVersion().then(v => { version.value = v }).catch(() => {})
})
</script>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: system-ui, sans-serif; background: #f5f5f5; color: #333; }
</style>

<style scoped>
.app-shell { min-height: 100vh; display: flex; flex-direction: column; }
.app-footer {
  padding: 0.6rem 1rem;
  text-align: center;
  font-size: 0.75rem;
  color: #8a94a0;
  background: #eceff3;
}
</style>

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
/* Surface and type come from the shared style layer, imported in main.js. */
* { margin: 0; padding: 0; }
</style>

<style scoped>
.app-shell { min-height: 100vh; display: flex; flex-direction: column; }
.app-footer {
  padding: 14px 24px;
  text-align: center;
  font-family: var(--font-mono);
  font-size: 0.68rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  border-top: 1px solid var(--paper-border);
}
</style>

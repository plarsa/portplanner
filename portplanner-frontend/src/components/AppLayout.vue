<template>
  <div class="layout">
    <nav class="sidebar">
      <div class="logo">⚓ Portplanner</div>
      <router-link to="/dashboard">Dashboard</router-link>
      <router-link to="/persons">Personer</router-link>
      <router-link to="/boats">Båtar</router-link>
      <router-link to="/docks">Bryggor</router-link>
      <router-link to="/assignments">Tilldelningar</router-link>
      <router-link to="/queue">Kö</router-link>
      <router-link to="/tariffs">Tariff</router-link>
      <router-link to="/import-export">Import / Export</router-link>
      <router-link to="/audit">Händelselogg</router-link>
      <router-link to="/winter">Vinterupptagning</router-link>
      <div class="spacer" />
      <div class="user-info">
        <span>{{ auth.username }}</span>
        <span class="role">{{ auth.role }}</span>
      </div>
      <button class="logout" @click="auth.logout()">Logga ut</button>
    </nav>
    <main class="content">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { useAuthStore } from '../stores/auth'
const auth = useAuthStore()
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; }
.sidebar {
  width: 220px; background: #1a3a5c; color: white;
  display: flex; flex-direction: column; padding: 1.5rem 1rem;
  flex-shrink: 0;
}
.logo { font-size: 1.2rem; font-weight: 700; margin-bottom: 2rem; }
a {
  color: rgba(255,255,255,0.8); text-decoration: none;
  padding: 0.6rem 0.8rem; border-radius: 6px; margin-bottom: 0.2rem;
  display: block; font-size: 0.9rem;
}
a:hover, a.router-link-active { background: rgba(255,255,255,0.15); color: white; }
.spacer { flex: 1; }
.user-info { display: flex; flex-direction: column; font-size: 0.8rem; margin-bottom: 0.5rem; }
.role { color: rgba(255,255,255,0.5); font-size: 0.7rem; }
.logout {
  background: rgba(255,255,255,0.1); color: white;
  border: none; border-radius: 6px; padding: 0.5rem;
  cursor: pointer; font-size: 0.85rem;
}
.logout:hover { background: rgba(255,255,255,0.2); }
.content { flex: 1; padding: 2rem; overflow-y: auto; }
</style>

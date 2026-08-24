<template>
  <div id="app-root">
    <nav v-if="auth.isLoggedIn" class="navbar">
      <span class="nav-brand">⚓ Medlemsportal</span>
      <div class="nav-links">
        <router-link to="/dashboard">Hem</router-link>
        <router-link to="/slip">Min plats</router-link>
        <router-link to="/boats">Mina båtar</router-link>
        <router-link to="/queue">Kö</router-link>
        <router-link to="/profile">Profil</router-link>
      </div>
      <div class="nav-user">
        <span>{{ auth.username }}</span>
        <button @click="auth.logout()">Logga ut</button>
      </div>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useAuthStore } from './stores/auth'
const auth = useAuthStore()
</script>

<style>
*, *::before, *::after { box-sizing: border-box; }
body { margin: 0; font-family: system-ui, -apple-system, sans-serif; background: #f4f7fb; color: #1a2535; }
a { color: inherit; }
</style>

<style scoped>
#app-root { min-height: 100vh; display: flex; flex-direction: column; }

.navbar {
  background: #1a3a5c;
  color: white;
  padding: 0.75rem 1.5rem;
  display: flex;
  align-items: center;
  gap: 1.5rem;
  flex-wrap: wrap;
}
.nav-brand { font-weight: 700; font-size: 1.1rem; margin-right: auto; }
.nav-links { display: flex; gap: 1rem; }
.nav-links a { color: rgba(255,255,255,0.8); text-decoration: none; font-size: 0.9rem; padding: 0.25rem 0; }
.nav-links a.router-link-active { color: white; border-bottom: 2px solid #4da6ff; }
.nav-user { display: flex; align-items: center; gap: 0.75rem; font-size: 0.85rem; }
.nav-user button {
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.3);
  color: white;
  padding: 0.3rem 0.75rem;
  border-radius: 5px;
  cursor: pointer;
  font-size: 0.8rem;
}
.nav-user button:hover { background: rgba(255,255,255,0.25); }

.main-content { flex: 1; padding: 2rem 1.5rem; max-width: 960px; margin: 0 auto; width: 100%; }
</style>

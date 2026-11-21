<template>
  <div id="app-root">
    <header v-if="auth.isLoggedIn" class="appbar">
      <div class="appbar-inner">
        <router-link to="/dashboard" class="brand">
          <!-- Wordmark: a jetty seen from the side. -->
          <svg viewBox="0 0 64 64" aria-hidden="true">
            <path d="M6 50H58" /><path d="M15 50V16" /><path d="M28 50V16" />
            <path d="M41 50V16" /><path d="M54 50V16" />
            <circle cx="21.5" cy="36" r="5" fill="currentColor" stroke="none" />
          </svg>
          Medlemsportal
        </router-link>

        <button
          ref="hamburger"
          class="nav-toggle"
          :aria-expanded="navOpen"
          aria-controls="appnav"
          aria-label="Meny"
          @click="navOpen = !navOpen">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M4 12h16M4 17h16" /></svg>
        </button>

        <ul id="appnav" class="appnav" :class="{ open: navOpen }" @click="closeNav">
          <li><router-link to="/dashboard">Hem</router-link></li>
          <li v-if="modules.isAccessible('member-portal')"><router-link to="/slip">Min plats</router-link></li>
          <li v-if="modules.isAccessible('member-portal')"><router-link to="/boats">Mina båtar</router-link></li>
          <li v-if="modules.isAccessible('member-portal')"><router-link to="/queue">Kö</router-link></li>
          <li v-if="modules.isAccessible('storage')"><router-link to="/haul-out">Vinterupptagning</router-link></li>
          <li v-if="modules.isAccessible('rentals')"><router-link to="/rentals">Hyra</router-link></li>
          <li v-if="modules.isAccessible('member-portal')"><router-link to="/profile">Profil</router-link></li>
        </ul>

        <div class="appbar-actions">
          <span class="who">{{ auth.username }}</span>
          <button class="btn btn-secondary btn-sm" @click="auth.logout()">Logga ut</button>
        </div>
      </div>
    </header>

    <!-- Visibility plus opacity, not display: display would cancel the fade. -->
    <div class="nav-dimmer" :class="{ on: navOpen }" @click="closeNav"></div>

    <main class="sheet">
      <router-view />
    </main>

    <footer class="app-footer">Portplanner {{ version }}</footer>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useAuthStore } from './stores/auth'
import { useModulesStore } from './stores/modules'
import { getVersion } from './api/version'

const auth = useAuthStore()
const modules = useModulesStore()
const version = ref('')
const navOpen = ref(false)
const hamburger = ref(null)

/** Closing always returns focus to the control that opened the drawer. */
function closeNav() {
  if (!navOpen.value) return
  navOpen.value = false
  hamburger.value?.focus()
}

function onKeydown(e) {
  if (e.key === 'Escape') closeNav()
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  getVersion().then(v => { version.value = v }).catch(() => {})
})
onUnmounted(() => document.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
#app-root { min-height: 100vh; display: flex; flex-direction: column; }

.brand svg { width: 22px; height: 22px; stroke-width: 7; }

.who { font-size: 0.88rem; color: var(--text-muted); }

.nav-toggle {
  display: none;
  order: 3;
  margin-left: auto;
  background: none;
  border: 0;
  padding: 6px;
  color: var(--ink);
  cursor: pointer;
}
.nav-toggle svg { width: 24px; height: 24px; }

.nav-dimmer {
  position: fixed;
  inset: 0;
  background: rgba(16, 32, 52, 0.4);
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.25s ease, visibility 0.25s ease;
  z-index: 90;
}
.nav-dimmer.on { opacity: 1; visibility: visible; }

.sheet { flex: 1; padding-top: 32px; padding-bottom: 44px; width: 100%; }

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

/* Three links plus the actions run out of room well before the layout does. */
@media (max-width: 760px) {
  .nav-toggle { display: block; }
  .appbar-inner { flex-wrap: wrap; gap: 12px; }
  .appnav {
    order: 4;
    flex-basis: 100%;
    flex-direction: column;
    margin: 0;
    max-height: 0;
    overflow: hidden;
    transition: max-height 0.25s ease;
  }
  .appnav.open { max-height: 320px; }
  .appbar-actions { order: 2; margin-left: auto; }
  .nav-toggle { margin-left: 0; }
}
</style>

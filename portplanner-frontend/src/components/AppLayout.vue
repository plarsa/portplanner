<template>
  <div class="layout">
    <nav class="sidenav" :class="{ open: navOpen }">
      <div class="sidenav-top">
        <router-link to="/dashboard" class="brand">
          <!-- Wordmark: a jetty seen from the side. -->
          <svg viewBox="0 0 64 64" aria-hidden="true">
            <path d="M6 50H58" /><path d="M15 50V16" /><path d="M28 50V16" />
            <path d="M41 50V16" /><path d="M54 50V16" />
            <circle cx="21.5" cy="36" r="5" fill="currentColor" stroke="none" />
          </svg>
          Portplanner
        </router-link>
        <button
          ref="hamburger"
          class="nav-toggle"
          :aria-expanded="navOpen"
          aria-controls="sidenav-links"
          aria-label="Meny"
          @click="navOpen = !navOpen">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M4 12h16M4 17h16" /></svg>
        </button>
      </div>

      <div id="sidenav-links" class="sidenav-links" @click="closeNav">
        <router-link to="/dashboard" class="navlink">Dashboard</router-link>

        <p class="navgroup">Register</p>
        <router-link to="/persons" class="navlink">Personer</router-link>
        <router-link to="/boats" class="navlink">Båtar</router-link>
        <router-link to="/docks" class="navlink">Bryggor</router-link>

        <p class="navgroup">Platser</p>
        <router-link to="/assignments" class="navlink">Tilldelningar</router-link>
        <router-link to="/queue" class="navlink">Kö</router-link>
        <router-link to="/tariffs" class="navlink">Tariff</router-link>

        <template v-if="hasOperations">
          <p class="navgroup">Drift</p>
          <router-link v-if="modules.isAccessible('storage')" to="/winter" class="navlink">Vinterupptagning</router-link>
          <router-link v-if="modules.isAccessible('rentals')" to="/rentals" class="navlink">Uthyrning</router-link>
        </template>

        <template v-if="hasSystem">
          <p class="navgroup">System</p>
          <router-link v-if="modules.isAccessible('import-export')" to="/import-export" class="navlink">Import / Export</router-link>
          <router-link v-if="modules.isAccessible('audit')" to="/audit" class="navlink">Händelselogg</router-link>
        </template>

        <div class="sidenav-user">
          <span class="who">{{ auth.username }}</span>
          <span class="role">{{ auth.role }}</span>
          <button class="btn btn-secondary btn-sm" @click="auth.logout()">Logga ut</button>
        </div>
      </div>
    </nav>

    <!-- Visibility plus opacity, not display: display would cancel the fade. -->
    <div class="nav-dimmer" :class="{ on: navOpen }" @click="closeNav"></div>

    <main class="content">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useModulesStore } from '../stores/modules'

const auth = useAuthStore()
const modules = useModulesStore()
const navOpen = ref(false)
const hamburger = ref(null)

/** A group disappears when every destination in it is switched off. */
const hasOperations = computed(() =>
  modules.isAccessible('storage') || modules.isAccessible('rentals'))
const hasSystem = computed(() =>
  modules.isAccessible('import-export') || modules.isAccessible('audit'))

/** Closing always returns focus to the control that opened the drawer. */
function closeNav() {
  if (!navOpen.value) return
  navOpen.value = false
  hamburger.value?.focus()
}

function onKeydown(e) {
  if (e.key === 'Escape') closeNav()
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onUnmounted(() => document.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.layout { display: flex; flex: 1; align-items: flex-start; }

/*
 * The guide describes a top row for a portal. Admin has eleven destinations plus
 * sub-pages, which do not fit one row, so the navigation is a column instead. The
 * rules are the same: paper surface, a hairline instead of a border box, the active
 * item on --sea-pale, and no shadow, because the column is part of the page.
 */
.sidenav {
  position: sticky;
  top: 0;
  width: 232px;
  flex-shrink: 0;
  height: 100vh;
  overflow-y: auto;
  background: var(--paper-surface);
  border-right: 1px solid var(--paper-border);
  padding: 18px 14px;
  display: flex;
  flex-direction: column;
}

.sidenav-top { display: flex; align-items: center; justify-content: space-between; }

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-display);
  font-variation-settings: "opsz" 40;
  font-weight: 700;
  font-size: 1.05rem;
  letter-spacing: -0.01em;
  color: var(--ink);
  text-decoration: none;
  padding: 0 8px;
}
.brand svg { width: 20px; height: 20px; stroke-width: 7; }

.sidenav-links { display: flex; flex-direction: column; flex: 1; margin-top: 22px; }

.navgroup {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin: 18px 0 6px;
  padding: 0 8px;
}

.navlink {
  display: block;
  padding: 7px 8px;
  border-radius: var(--radius-sm);
  font-size: 0.92rem;
  font-weight: 500;
  color: var(--text-muted);
  text-decoration: none;
}
.navlink:hover { background: var(--paper-deep); color: var(--ink); }
.navlink.router-link-active { background: var(--sea-pale); color: var(--ink); }

.sidenav-user {
  margin-top: auto;
  padding: 18px 8px 0;
  border-top: 1px solid var(--paper-border);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}
.who { font-size: 0.9rem; color: var(--ink); }
.role {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.sidenav-user .btn { margin-top: 6px; }

.nav-toggle {
  display: none;
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

.content {
  flex: 1;
  min-width: 0;
  padding: 32px 24px 44px;
}

/* The column needs more room than a tab row, so it folds a breakpoint earlier. */
@media (max-width: 980px) {
  .layout { flex-direction: column; align-items: stretch; }
  .sidenav {
    width: auto;
    height: auto;
    padding: 11px 24px;
    border-right: 0;
    box-shadow: var(--shadow-header);
    z-index: 100;
  }
  .nav-toggle { display: block; }
  .sidenav-links {
    margin-top: 0;
    max-height: 0;
    overflow: hidden;
    transition: max-height 0.25s ease;
  }
  .sidenav.open .sidenav-links { max-height: 620px; margin-top: 14px; }
  .sidenav-user { margin-top: 18px; }
}
</style>

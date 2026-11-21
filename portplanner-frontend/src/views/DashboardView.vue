<template>
  <AppLayout>
    <div v-if="modules.notice" class="notice" @click="modules.notice = ''">{{ modules.notice }}</div>
    <div class="dashboard-header">
      <div>
        <h2>Dashboard</h2>
        <p class="welcome">Välkommen, {{ auth.username }}!</p>
      </div>
      <button class="btn btn-secondary btn-sm" @click="showConfig = !showConfig">
        {{ showConfig ? 'Stäng' : '⚙ Konfigurera' }}
      </button>
    </div>

    <div v-if="showConfig" class="config-panel">
      <h3>Välj badges att visa</h3>
      <div class="config-list">
        <label v-for="badge in allBadges" :key="badge.id" class="config-item">
          <input class="input" type="checkbox" :value="badge.id" v-model="visibleIds" />
          <div class="config-info">
            <span class="config-name">{{ badge.name }}</span>
            <span class="config-desc">{{ badge.description }}</span>
          </div>
        </label>
      </div>
    </div>

    <div v-if="loading" class="loading">Laddar...</div>

    <div v-else class="columns">
      <div
        v-for="badge in visibleBadges"
        :key="badge.id"
        class="column"
        :class="{ money: badge.id.includes('revenue') }"
      >
        <div class="column-title">{{ badge.name }}</div>
        <div class="column-value num">{{ badge.value }}</div>
        <div class="column-sub">{{ badge.description }}</div>
      </div>
      <div v-if="visibleBadges.length === 0 && !showConfig" class="empty">
        Inga badges valda. Klicka på <strong>Konfigurera</strong> för att lägga till.
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { useAuthStore } from '../stores/auth'
import { useModulesStore } from '../stores/modules'
import api from '../api/axios'

const STORAGE_KEY = 'dashboard-visible-badges'
const DEFAULT_IDS = ['available-slips', 'occupied-slips', 'queue-length', 'dock-count', 'occupied-revenue', 'total-revenue']

const auth = useAuthStore()
const modules = useModulesStore()
const allBadges = ref([])
const loading = ref(true)
const showConfig = ref(false)

const visibleIds = ref((() => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : DEFAULT_IDS
  } catch {
    return DEFAULT_IDS
  }
})())

watch(visibleIds, (ids) => {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(ids)) } catch {}
}, { deep: true })

const visibleBadges = computed(() =>
  allBadges.value.filter(b => visibleIds.value.includes(b.id))
)

onMounted(async () => {
  try {
    const { data } = await api.get('/dashboard/badges')
    allBadges.value = data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.notice {
  border-left: 2px solid var(--warn-border);
  background: var(--warn-bg);
  color: var(--warn-text);
  padding: 10px 14px;
  margin: 0 0 22px;
  font-size: 0.9rem;
  cursor: pointer;
}
.dashboard-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 22px; }
h2 { font-size: 1.45rem; margin: 0; }
.welcome { color: var(--text-muted); margin: 4px 0 0; }

.config-panel {
  margin-top: 22px;
  background: var(--surface);
  border: 1px solid var(--paper-border);
  border-radius: var(--radius);
  padding: 18px 22px;
}
.config-panel h3 { margin: 0 0 14px; }
.config-list { display: flex; flex-direction: column; gap: 10px; }
.config-item { display: flex; align-items: flex-start; gap: 10px; cursor: pointer; }
.config-item input { margin-top: 4px; }
.config-info { display: flex; flex-direction: column; }
.config-name { font-weight: 500; font-size: 0.92rem; color: var(--ink); }
.config-desc { font-size: 0.82rem; color: var(--text-muted); }

.loading, .empty { color: var(--text-muted); margin-top: 22px; }

.columns { margin-top: 32px; }
/* Money is still a number on a rule, not a card with an accent stripe. */
.column.money { border-top-color: var(--sea); }
.column-value { font-size: 1.75rem; }
.column-sub { font-size: 0.8rem; color: var(--text-muted); }
</style>

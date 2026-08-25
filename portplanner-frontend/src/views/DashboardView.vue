<template>
  <AppLayout>
    <div class="dashboard-header">
      <div>
        <h2>Dashboard</h2>
        <p class="welcome">Välkommen, {{ auth.username }}!</p>
      </div>
      <button class="btn-configure" @click="showConfig = !showConfig">
        {{ showConfig ? 'Stäng' : '⚙ Konfigurera' }}
      </button>
    </div>

    <div v-if="showConfig" class="config-panel">
      <h3>Välj badges att visa</h3>
      <div class="config-list">
        <label v-for="badge in allBadges" :key="badge.id" class="config-item">
          <input type="checkbox" :value="badge.id" v-model="visibleIds" />
          <div class="config-info">
            <span class="config-name">{{ badge.name }}</span>
            <span class="config-desc">{{ badge.description }}</span>
          </div>
        </label>
      </div>
    </div>

    <div v-if="loading" class="loading">Laddar...</div>

    <div v-else class="cards">
      <div
        v-for="badge in visibleBadges"
        :key="badge.id"
        class="card"
        :class="{ 'card-money': badge.id.includes('revenue') }"
      >
        <div class="card-title">{{ badge.name }}</div>
        <div class="card-value">{{ badge.value }}</div>
        <div class="card-sub">{{ badge.description }}</div>
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
import api from '../api/axios'

const STORAGE_KEY = 'dashboard-visible-badges'
const DEFAULT_IDS = ['available-slips', 'occupied-slips', 'queue-length', 'dock-count', 'occupied-revenue', 'total-revenue']

const auth = useAuthStore()
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
.dashboard-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}
h2 { font-size: 1.5rem; margin: 0; }
.welcome { color: #666; margin-top: 0.25rem; margin-bottom: 0; }

.btn-configure {
  margin-top: 0.25rem;
  padding: 0.4rem 1rem;
  background: #f0f4f8;
  border: 1px solid #ccd6e0;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.85rem;
  white-space: nowrap;
}
.btn-configure:hover { background: #e2eaf2; }

.config-panel {
  margin-top: 1.5rem;
  background: #f8fafc;
  border: 1px solid #dde6ef;
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
}
.config-panel h3 { margin: 0 0 1rem; font-size: 0.95rem; }
.config-list { display: flex; flex-direction: column; gap: 0.5rem; }
.config-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  cursor: pointer;
}
.config-item input { margin-top: 0.2rem; }
.config-info { display: flex; flex-direction: column; }
.config-name { font-weight: 600; font-size: 0.9rem; }
.config-desc { font-size: 0.8rem; color: #888; }

.loading { margin-top: 2rem; color: #888; }
.empty { color: #999; font-size: 0.95rem; margin-top: 0.5rem; }

.cards { display: flex; gap: 1.5rem; margin-top: 2rem; flex-wrap: wrap; }
.card {
  background: white;
  border-radius: 10px;
  padding: 1.5rem 2rem;
  min-width: 180px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.07);
}
.card-money { border-left: 4px solid #1a3a5c; }
.card-title { font-size: 0.85rem; color: #666; margin-bottom: 0.5rem; }
.card-value { font-size: 2rem; font-weight: 700; color: #1a3a5c; }
.card-sub { font-size: 0.78rem; color: #aaa; margin-top: 0.3rem; }
</style>

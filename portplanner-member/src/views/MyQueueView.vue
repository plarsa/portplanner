<template>
  <div>
    <h2>Min köstatus</h2>
    <div v-if="loading" class="loading">Laddar...</div>
    <div v-else-if="entries.length === 0" class="empty">Du är inte registrerad i kön.</div>
    <div v-else>
      <div v-for="entry in entries" :key="entry.id" class="card" :class="{ 'card-offer': entry.status === 'OFFERED' }">
        <div class="status-badge" :class="entry.status.toLowerCase()">{{ statusLabel(entry.status) }}</div>
        <div class="row"><span>Kö sedan</span><strong>{{ entry.requestedDate?.slice(0, 10) }}</strong></div>
        <div v-if="entry.notes" class="row"><span>Anteckningar</span><strong>{{ entry.notes }}</strong></div>
        <div v-if="entry.status === 'OFFERED'" class="offer-note">
          Erbjuden plats: {{ entry.offeredDockName }} – {{ entry.offeredSlipNumber }}.
          Gå till <router-link to="/slip">Min plats</router-link> för att svara.
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/axios'

const loading = ref(true)
const entries = ref([])

function statusLabel(s) {
  return { WAITING: 'Väntar', OFFERED: 'Erbjudande', ASSIGNED: 'Tilldelad', CANCELLED: 'Avbruten' }[s] ?? s
}

onMounted(async () => {
  try {
    const { data } = await api.get('/me/queue')
    entries.value = data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
h2 { margin: 0 0 1.5rem; }
.loading, .empty { color: #888; }
.card {
  background: white;
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.07);
  margin-bottom: 1rem;
}
.card-offer { border-left: 4px solid #e67e22; }
.row {
  display: flex;
  justify-content: space-between;
  padding: 0.4rem 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 0.9rem;
}
.row:last-child { border-bottom: none; }
.row span { color: #666; }
.status-badge {
  display: inline-block;
  padding: 0.2rem 0.7rem;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
}
.status-badge.waiting { background: #eaf4ff; color: #1a6fc4; }
.status-badge.offered { background: #fff3e0; color: #e67e22; }
.status-badge.assigned { background: #eafaf1; color: #27ae60; }
.status-badge.cancelled { background: #fdf0ef; color: #c0392b; }
.offer-note { margin-top: 0.75rem; font-size: 0.85rem; color: #e67e22; }
.offer-note a { color: #1a6fc4; }
</style>

<template>
  <div>
    <h2>Min köstatus</h2>
    <p v-if="loading" class="empty">Laddar...</p>
    <p v-else-if="entries.length === 0" class="empty">Du är inte registrerad i kön.</p>
    <div v-else>
      <div v-for="entry in entries" :key="entry.id" class="panel">
        <span class="pill" :class="pillClass(entry.status)">{{ statusLabel(entry.status) }}</span>
        <div class="rows">
          <div class="row"><span>Kö sedan</span><span class="num">{{ entry.requestedDate?.slice(0, 10) }}</span></div>
          <div v-if="entry.notes" class="row"><span>Anteckningar</span><span>{{ entry.notes }}</span></div>
        </div>
        <p v-if="entry.status === 'OFFERED'" class="offer-note">
          Erbjuden plats: <span class="num">{{ entry.offeredDockName }} {{ entry.offeredSlipNumber }}</span>.
          Gå till <router-link to="/slip">Min plats</router-link> för att svara.
        </p>
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

/** Green is done, yellow waits for an answer, grey is dormant. */
function pillClass(s) {
  return {
    WAITING: 'pill-info', OFFERED: 'pill-warn', ASSIGNED: 'pill-ok', CANCELLED: 'pill-idle',
  }[s] ?? 'pill-idle'
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
h2 { font-size: 1.45rem; margin: 0 0 24px; }
.pill { margin-bottom: 14px; }
.offer-note { margin: 14px 0 0; font-size: 0.9rem; color: var(--warn-text); }
</style>

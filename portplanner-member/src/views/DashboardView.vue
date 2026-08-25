<template>
  <div>
    <h2>Välkommen, {{ auth.username }}!</h2>

    <div v-if="loading" class="loading">Laddar...</div>

    <div v-else class="cards">
      <div class="card">
        <div class="card-title">Min plats</div>
        <div v-if="slip" class="card-value">{{ slip.dockName }} – {{ slip.slipNumber }}</div>
        <div v-else class="card-value muted">Ingen tilldelad plats</div>
        <router-link to="/slip" class="card-link">Visa mer →</router-link>
      </div>

      <div class="card">
        <div class="card-title">Mina båtar</div>
        <div class="card-value">{{ boats.length }}</div>
        <router-link to="/boats" class="card-link">Visa mer →</router-link>
      </div>

      <div class="card" :class="{ 'card-offer': hasOffer }">
        <div class="card-title">Köstatus</div>
        <div v-if="queueEntry" class="card-value">
          {{ statusLabel(queueEntry.status) }}
        </div>
        <div v-else class="card-value muted">Inte i kö</div>
        <div v-if="hasOffer" class="offer-alert">Erbjudande väntar! ↗</div>
        <router-link to="/slip" class="card-link">{{ hasOffer ? 'Svara på erbjudande →' : 'Visa mer →' }}</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import api from '../api/axios'

const auth = useAuthStore()
const loading = ref(true)
const me = ref(null)
const assignments = ref([])
const queue = ref([])
const boats = ref([])

const slip = computed(() => assignments.value[0] ?? null)
const queueEntry = computed(() => queue.value[0] ?? null)
const hasOffer = computed(() => queueEntry.value?.status === 'OFFERED')

function statusLabel(s) {
  return { WAITING: 'Väntar', OFFERED: 'Erbjudande!', ASSIGNED: 'Tilldelad' }[s] ?? s
}

onMounted(async () => {
  try {
    const [mRes, aRes, qRes, bRes] = await Promise.all([
      api.get('/me'),
      api.get('/me/assignments'),
      api.get('/me/queue'),
      api.get('/me/boats'),
    ])
    me.value = mRes.data
    assignments.value = aRes.data
    queue.value = qRes.data
    boats.value = bRes.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
h2 { margin: 0 0 1.5rem; font-size: 1.4rem; }
.loading { color: #888; }
.cards { display: flex; gap: 1.25rem; flex-wrap: wrap; }
.card {
  background: white;
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
  min-width: 180px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.07);
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.card-offer { border-left: 4px solid #e67e22; }
.card-title { font-size: 0.8rem; color: #666; }
.card-value { font-size: 1.5rem; font-weight: 700; color: #1a3a5c; }
.card-value.muted { color: #bbb; font-size: 1rem; font-weight: 400; }
.card-link { font-size: 0.8rem; color: #1a6fc4; text-decoration: none; margin-top: auto; }
.offer-alert { font-size: 0.8rem; color: #e67e22; font-weight: 600; }
</style>

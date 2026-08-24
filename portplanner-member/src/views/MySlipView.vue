<template>
  <div>
    <h2>Min plats</h2>

    <div v-if="loading" class="loading">Laddar...</div>
    <template v-else>
      <!-- Erbjudande-panel -->
      <div v-if="offer" class="offer-panel">
        <h3>Du har ett erbjudande om en plats!</h3>
        <div class="offer-details">
          <span>Brygga:</span><strong>{{ offer.offeredDockName }}</strong>
          <span>Plats:</span><strong>{{ offer.offeredSlipNumber }}</strong>
        </div>
        <div class="offer-actions">
          <button class="btn-accept" :disabled="acting" @click="acceptOffer(offer.id)">
            Acceptera
          </button>
          <button class="btn-decline" :disabled="acting" @click="declineOffer(offer.id)">
            Avböj
          </button>
        </div>
        <p v-if="actionError" class="error">{{ actionError }}</p>
      </div>

      <!-- Tilldelad plats -->
      <div v-if="slip" class="card">
        <div class="row"><span>Brygga</span><strong>{{ slip.dockName }}</strong></div>
        <div class="row"><span>Plats</span><strong>{{ slip.slipNumber }}</strong></div>
        <div class="row"><span>Tillagd</span><strong>{{ slip.startDate }}</strong></div>
      </div>
      <div v-else-if="!offer" class="empty">
        Du har ingen tilldelad plats.
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api/axios'

const loading = ref(true)
const acting = ref(false)
const actionError = ref('')
const assignments = ref([])
const queue = ref([])

const slip = computed(() => assignments.value[0] ?? null)
const offer = computed(() => queue.value.find(q => q.status === 'OFFERED') ?? null)

async function acceptOffer(entryId) {
  acting.value = true
  actionError.value = ''
  try {
    await api.post(`/queue/${entryId}/accept-offer`)
    await reload()
  } catch {
    actionError.value = 'Kunde inte acceptera erbjudandet.'
  } finally {
    acting.value = false
  }
}

async function declineOffer(entryId) {
  acting.value = true
  actionError.value = ''
  try {
    await api.post(`/queue/${entryId}/decline-offer`)
    await reload()
  } catch {
    actionError.value = 'Kunde inte avböja erbjudandet.'
  } finally {
    acting.value = false
  }
}

async function reload() {
  const [aRes, qRes] = await Promise.all([api.get('/me/assignments'), api.get('/me/queue')])
  assignments.value = aRes.data
  queue.value = qRes.data
}

onMounted(async () => {
  try { await reload() } finally { loading.value = false }
})
</script>

<style scoped>
h2 { margin: 0 0 1.5rem; }
.loading, .empty { color: #888; }

.offer-panel {
  background: #fff8f0;
  border: 1px solid #f0a040;
  border-left: 4px solid #e67e22;
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
  margin-bottom: 1.5rem;
}
.offer-panel h3 { margin: 0 0 1rem; color: #c0620a; }
.offer-details {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.4rem 1rem;
  margin-bottom: 1rem;
  font-size: 0.95rem;
}
.offer-actions { display: flex; gap: 0.75rem; }
.btn-accept {
  padding: 0.55rem 1.25rem;
  background: #27ae60;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.95rem;
}
.btn-accept:hover:not(:disabled) { background: #219a52; }
.btn-decline {
  padding: 0.55rem 1.25rem;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.95rem;
}
.btn-decline:hover:not(:disabled) { background: #c0392b; }
button:disabled { opacity: 0.6; cursor: not-allowed; }

.card {
  background: white;
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.07);
}
.row {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 0.95rem;
}
.row:last-child { border-bottom: none; }
.row span { color: #666; }
.error { color: #c0392b; font-size: 0.85rem; margin-top: 0.5rem; }
</style>

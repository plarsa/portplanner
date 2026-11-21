<template>
  <div>
    <h2>Min plats</h2>

    <p v-if="loading" class="empty">Laddar...</p>
    <template v-else>
      <div v-if="offer" class="panel offer-panel">
        <p class="section-title">Erbjudande om plats</p>
        <div class="rows">
          <div class="row"><span>Brygga</span><span class="num">{{ offer.offeredDockName }}</span></div>
          <div class="row"><span>Plats</span><span class="num">{{ offer.offeredSlipNumber }}</span></div>
        </div>
        <div class="offer-actions">
          <button class="btn btn-primary btn-sm" :disabled="acting" @click="acceptOffer(offer.id)">Acceptera</button>
          <button class="btn btn-secondary btn-sm" :disabled="acting" @click="declineOffer(offer.id)">Avböj</button>
        </div>
        <p v-if="actionError" class="error">{{ actionError }}</p>
      </div>

      <div v-if="slip" class="panel">
        <div class="rows">
          <div class="row"><span>Brygga</span><span class="num">{{ slip.dockName }}</span></div>
          <div class="row"><span>Plats</span><span class="num">{{ slip.slipNumber }}</span></div>
          <div class="row"><span>Tillagd</span><span class="num">{{ slip.startDate }}</span></div>
        </div>
      </div>
      <p v-else-if="!offer" class="empty">Du har ingen tilldelad plats.</p>
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
h2 { font-size: 1.45rem; margin: 0 0 24px; }
/* The offer is the one thing on this page that asks for an answer. */
.offer-panel { border-left: 2px solid var(--warn-border); background: var(--warn-bg); }
.offer-panel .section-title { color: var(--warn-text); }
.offer-actions { display: flex; gap: 10px; margin-top: 18px; }
button:disabled { opacity: 0.6; cursor: not-allowed; }
</style>

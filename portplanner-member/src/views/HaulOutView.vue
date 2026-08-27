<template>
  <div>
    <h2>Vinterupptagning</h2>

    <div v-if="myBookings.length" class="section">
      <div class="section-title">Mina anmälningar</div>
      <div v-for="bk in myBookings" :key="bk.id" class="card">
        <div class="card-header">
          <span class="boat-name">{{ bk.boatModel }}</span>
          <span :class="['status-chip', bk.status.toLowerCase()]">{{ statusLabel(bk.status) }}</span>
        </div>
        <div class="card-details">
          <span>{{ bk.slotDate }}</span>
          <span>{{ bk.slotTime }}</span>
        </div>
      </div>
    </div>

    <div class="section">
      <div class="section-title">Lediga tider</div>
      <div v-if="!availableSlots.length" class="empty">Inga lediga tider för tillfället.</div>
      <div v-for="slot in availableSlots" :key="slot.id" class="slot-card">
        <div class="slot-header">
          <div>
            <div class="slot-date">{{ slot.slotDate }}</div>
            <div class="slot-time">{{ slot.startTime }} – {{ slot.endTime }}</div>
          </div>
          <div class="slot-cap">{{ slot.capacity - slot.bookedCount }} platser kvar</div>
        </div>
        <div v-if="myBoats.length" class="slot-book">
          <select v-model="selectedBoat[slot.id]">
            <option value="">Välj båt…</option>
            <option v-for="b in myBoats" :key="b.id" :value="b.id">{{ b.model }}</option>
          </select>
          <button class="btn-primary" :disabled="!selectedBoat[slot.id]" @click="book(slot)">
            Anmäl
          </button>
        </div>
        <div v-else class="empty">Du har inga registrerade båtar.</div>
      </div>
    </div>

    <p v-if="err" class="error">{{ err }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAvailableSlots, getMyBookings, createMyBooking } from '../api/winterHaulOut'
import api from '../api/axios'

const availableSlots = ref([])
const myBookings = ref([])
const myBoats = ref([])
const selectedBoat = ref({})
const err = ref('')

function statusLabel(s) {
  return { REQUESTED: 'Anmäld', CONFIRMED: 'Bekräftad', COMPLETED: 'Genomförd', CANCELLED: 'Avbokad' }[s] ?? s
}

async function book(slot) {
  err.value = ''
  const boatId = selectedBoat.value[slot.id]
  if (!boatId) return
  try {
    await createMyBooking(slot.id, boatId)
    selectedBoat.value[slot.id] = ''
    await load()
  } catch (e) { err.value = e.response?.data?.error || 'Något gick fel' }
}

async function load() {
  const [slotsRes, bookingsRes, boatsRes] = await Promise.all([
    getAvailableSlots(), getMyBookings(), api.get('/me/boats')
  ])
  availableSlots.value = slotsRes.data
  myBookings.value = bookingsRes.data
  myBoats.value = boatsRes.data
}

onMounted(load)
</script>

<style scoped>
h2 { margin: 0 0 1.5rem; }
.section { margin-bottom: 2rem; }
.section-title { font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: #888; margin-bottom: 0.75rem; }
.empty { color: #bbb; font-size: 0.88rem; }
.error { color: #c0392b; font-size: 0.85rem; }

.card { background: white; border-radius: 10px; padding: 1rem; margin-bottom: 0.6rem; box-shadow: 0 2px 6px rgba(0,0,0,0.07); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.35rem; }
.boat-name { font-weight: 700; font-size: 0.95rem; }
.card-details { font-size: 0.82rem; color: #666; display: flex; gap: 1rem; }

.slot-card { background: white; border-radius: 10px; padding: 1rem; margin-bottom: 0.6rem; box-shadow: 0 2px 6px rgba(0,0,0,0.07); }
.slot-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem; }
.slot-date { font-weight: 700; font-size: 0.95rem; }
.slot-time { font-size: 0.82rem; color: #555; }
.slot-cap { font-size: 0.78rem; color: #888; }
.slot-book { display: flex; gap: 0.5rem; align-items: center; }
.slot-book select { padding: 0.45rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.88rem; flex: 1; }

.status-chip { font-size: 0.7rem; font-weight: 700; padding: 0.15rem 0.45rem; border-radius: 8px; }
.status-chip.requested { background: #fff3cd; color: #856404; }
.status-chip.confirmed { background: #d4edda; color: #155724; }
.status-chip.completed { background: #cce5ff; color: #004085; }
.status-chip.cancelled { background: #f0f0f0; color: #999; }

.btn-primary { background: #1a3a5c; color: white; padding: 0.45rem 1rem; border-radius: 6px; border: none; cursor: pointer; font-size: 0.85rem; white-space: nowrap; }
.btn-primary:hover { background: #234e7a; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
</style>

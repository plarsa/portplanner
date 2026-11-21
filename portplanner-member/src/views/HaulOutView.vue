<template>
  <div>
    <h2>Vinterupptagning</h2>

    <section v-if="myBookings.length" class="section">
      <p class="section-title">Mina anmälningar</p>
      <div class="panel">
        <div class="rows">
          <div v-for="bk in myBookings" :key="bk.id" class="row booking">
            <span class="boat-name">{{ bk.boatModel }}</span>
            <span class="when">
              <span class="num">{{ bk.slotDate }}</span>
              <span class="num">{{ bk.slotTime }}</span>
              <span class="pill" :class="pillClass(bk.status)">{{ statusLabel(bk.status) }}</span>
            </span>
          </div>
        </div>
      </div>
    </section>

    <section class="section">
      <p class="section-title">Lediga tider</p>
      <p v-if="!availableSlots.length" class="empty">Inga lediga tider för tillfället.</p>
      <div v-for="slot in availableSlots" :key="slot.id" class="panel slot">
        <div class="slot-header">
          <div>
            <div class="slot-date num">{{ slot.slotDate }}</div>
            <div class="slot-time num">{{ slot.startTime }} – {{ slot.endTime }}</div>
          </div>
          <div class="slot-cap"><span class="num">{{ slot.capacity - slot.bookedCount }}</span> platser kvar</div>
        </div>
        <div v-if="myBoats.length" class="slot-book">
          <div class="field">
            <label :for="`boat-${slot.id}`">Båt</label>
            <select :id="`boat-${slot.id}`" class="input" v-model="selectedBoat[slot.id]">
              <option value="">Välj båt…</option>
              <option v-for="b in myBoats" :key="b.id" :value="b.id">{{ b.model }}</option>
            </select>
          </div>
          <button class="btn btn-primary btn-sm" :disabled="!selectedBoat[slot.id]" @click="book(slot)">Anmäl</button>
        </div>
        <p v-else class="empty">Du har inga registrerade båtar.</p>
      </div>
    </section>

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

/** Yellow waits for the harbour, green is settled, grey is dormant. */
function pillClass(s) {
  return {
    REQUESTED: 'pill-warn', CONFIRMED: 'pill-ok', COMPLETED: 'pill-info', CANCELLED: 'pill-idle',
  }[s] ?? 'pill-idle'
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
h2 { font-size: 1.45rem; margin: 0 0 24px; }
.boat-name { font-weight: 600; color: var(--ink); }
.when { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; color: var(--text-muted); }
.slot-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 22px; }
.slot-date { font-weight: 500; color: var(--ink); font-size: 0.95rem; }
.slot-time { color: var(--text-muted); }
.slot-cap { font-size: 0.88rem; color: var(--text-muted); }
.slot-book { display: flex; align-items: flex-end; gap: 10px; margin-top: 14px; }
.slot-book .field { flex: 1; max-width: 260px; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
@media (max-width: 620px) {
  .row.booking { flex-direction: column; gap: 6px; }
  .slot-book { flex-direction: column; align-items: stretch; }
  .slot-book .field { max-width: none; }
}
</style>

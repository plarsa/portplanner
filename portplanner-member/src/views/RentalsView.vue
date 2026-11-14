<template>
  <div>
    <h2>Hyra</h2>

    <div v-if="myBookings.length" class="section">
      <div class="section-title">Mina bokningar</div>
      <div v-for="b in myBookings" :key="b.id" class="card">
        <div class="card-header">
          <span class="obj-name">{{ b.objectName }}</span>
          <span :class="['status-chip', b.status.toLowerCase()]">{{ statusLabel(b.status) }}</span>
        </div>
        <div class="card-details">
          <span>{{ formatTime(b.startAt) }} – {{ formatTime(b.endAt) }}</span>
        </div>
        <div v-if="canCancel(b) || canReturn(b)" class="card-actions">
          <button v-if="canCancel(b)" @click="cancel(b)">Avboka</button>
          <button v-if="canReturn(b)" class="btn-primary" @click="openReturn(b)">Återlämna</button>
        </div>
      </div>
    </div>

    <div class="section">
      <div class="section-title">Objekt att hyra</div>
      <div v-if="!objects.length" class="empty">Inget är öppet för bokning just nu.</div>
      <div v-for="o in objects" :key="o.id" class="obj-card" :class="{ on: selected?.id === o.id }">
        <img v-if="o.hasImage" :src="`/api/me/rentals/objects/${o.id}/image`" alt="" class="thumb" />
        <div class="obj-body">
          <div class="obj-name">{{ o.name }}</div>
          <p class="desc">{{ o.description || '' }}</p>
          <ul class="rules">
            <li>Längst {{ o.maxDurationHours }} timmar per bokning</li>
            <li>Kan bokas upp till {{ o.maxDaysAhead }} dagar fram</li>
            <li v-if="o.requiresApproval">Bokningen måste godkännas av hamnen</li>
          </ul>
          <button class="btn-primary" @click="choose(o)">
            {{ selected?.id === o.id ? 'Vald' : 'Boka' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="selected" class="section">
      <div class="section-title">Kalender – {{ selected.name }}</div>
      <BookingCalendar
        :busy="busy"
        :maxDurationHours="selected.maxDurationHours"
        :maxDaysAhead="selected.maxDaysAhead"
        @select="pick" />
      <button class="btn-primary book" :disabled="!chosen" @click="book">
        {{ chosen ? 'Boka vald tid' : 'Välj en tid i kalendern' }}
      </button>
    </div>

    <div v-if="returning" class="modal-backdrop" @click.self="returning = null">
      <div class="modal">
        <div class="section-title">Återlämna {{ returning.objectName }}</div>
        <p class="hint">Rapportera fel och brister, eller lämna tomt om allt är i ordning.</p>
        <textarea v-model="faultText" rows="4" placeholder="t.ex. Storseglet är trasigt"></textarea>
        <div class="modal-actions">
          <button @click="returning = null">Avbryt</button>
          <button class="btn-primary" @click="doReturn">Återlämna</button>
        </div>
      </div>
    </div>

    <p v-if="err" class="error">{{ err }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import BookingCalendar from '../components/BookingCalendar.vue'
import {
  getObjects, getAvailability, getMyBookings, createBooking, cancelBooking, returnBooking,
} from '../api/rentals'

const objects = ref([])
const myBookings = ref([])
const busy = ref([])
const selected = ref(null)
const chosen = ref(null)
const returning = ref(null)
const faultText = ref('')
const err = ref('')

const STATUS = {
  PENDING: 'Väntar på godkännande', CONFIRMED: 'Bekräftad', REJECTED: 'Avslagen',
  CANCELLED: 'Avbokad', RETURNED: 'Återlämnad',
}
const statusLabel = (s) => STATUS[s] ?? s

function formatTime(t) {
  return t ? new Date(t).toLocaleString('sv-SE', { dateStyle: 'short', timeStyle: 'short' }) : ''
}

const canCancel = (b) =>
  ['PENDING', 'CONFIRMED'].includes(b.status) && new Date(b.startAt) > new Date()
const canReturn = (b) => b.status === 'CONFIRMED'

function isoLocal(d) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T00:00:00`
}

async function choose(o) {
  err.value = ''
  selected.value = o
  chosen.value = null
  const from = new Date()
  const to = new Date()
  to.setDate(to.getDate() + o.maxDaysAhead + 1)
  const { data } = await getAvailability(o.id, isoLocal(from), isoLocal(to))
  busy.value = data
}

function pick(range) {
  chosen.value = range
}

async function book() {
  err.value = ''
  if (!chosen.value) return
  try {
    await createBooking({
      objectId: selected.value.id,
      startAt: chosen.value.startAt,
      endAt: chosen.value.endAt,
    })
    chosen.value = null
    await load()
    await choose(selected.value)
  } catch (e) {
    err.value = e.response?.data?.error || 'Något gick fel'
  }
}

async function cancel(b) {
  err.value = ''
  try {
    await cancelBooking(b.id)
    await load()
    if (selected.value) await choose(selected.value)
  } catch (e) {
    err.value = e.response?.data?.error || 'Kunde inte avboka'
  }
}

function openReturn(b) {
  returning.value = b
  faultText.value = ''
  err.value = ''
}

async function doReturn() {
  err.value = ''
  try {
    await returnBooking(returning.value.id, faultText.value)
    returning.value = null
    await load()
  } catch (e) {
    err.value = e.response?.data?.error || 'Kunde inte återlämna'
  }
}

async function load() {
  const [objectsRes, bookingsRes] = await Promise.all([getObjects(), getMyBookings()])
  objects.value = objectsRes.data
  myBookings.value = bookingsRes.data
}

onMounted(load)
</script>

<style scoped>
h2 { margin: 0 0 1.5rem; }
.section { margin-bottom: 2rem; }
.section-title { font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: #888; margin-bottom: 0.75rem; }
.empty { color: #bbb; font-size: 0.88rem; }
.error { color: #c0392b; font-size: 0.85rem; }
.hint { font-size: 0.8rem; color: #777; margin: 0 0 0.5rem; }

.card { background: white; border-radius: 10px; padding: 1rem; margin-bottom: 0.6rem; box-shadow: 0 2px 6px rgba(0,0,0,0.07); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.35rem; }
.obj-name { font-weight: 700; font-size: 0.95rem; }
.card-details { font-size: 0.82rem; color: #666; }
.card-actions { display: flex; gap: 0.5rem; margin-top: 0.75rem; }

.obj-card { background: white; border-radius: 10px; margin-bottom: 0.6rem; box-shadow: 0 2px 6px rgba(0,0,0,0.07);
  overflow: hidden; display: flex; gap: 1rem; }
.obj-card.on { outline: 2px solid #1a3a5c; }
.thumb { width: 120px; height: 120px; object-fit: cover; flex-shrink: 0; background: #eef2f7; }
.obj-body { padding: 0.9rem 1rem 0.9rem 0; display: flex; flex-direction: column; gap: 0.35rem; flex: 1; }
.obj-card .obj-body { padding-left: 1rem; }
.obj-card .thumb + .obj-body { padding-left: 0; }
.desc { font-size: 0.85rem; color: #666; margin: 0; }
.rules { list-style: none; padding: 0; margin: 0; font-size: 0.78rem; color: #777; }
.rules li { padding: 0.05rem 0; }
.obj-body button { align-self: flex-start; margin-top: auto; }

.book { margin-top: 0.75rem; width: 100%; }

button { background: #eef2f7; border: none; border-radius: 6px; padding: 0.45rem 1rem; cursor: pointer; font-size: 0.85rem; color: #1a3a5c; }
button:hover { background: #dce8f6; }
.btn-primary { background: #1a3a5c; color: white; }
.btn-primary:hover { background: #234e7a; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

.status-chip { font-size: 0.7rem; font-weight: 700; padding: 0.15rem 0.45rem; border-radius: 8px; }
.status-chip.pending { background: #fff3cd; color: #856404; }
.status-chip.confirmed { background: #d4edda; color: #155724; }
.status-chip.returned { background: #cce5ff; color: #004085; }
.status-chip.rejected, .status-chip.cancelled { background: #f0f0f0; color: #999; }

.modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex;
  align-items: center; justify-content: center; z-index: 100; padding: 1rem; }
.modal { background: white; border-radius: 10px; padding: 1.25rem; width: 100%; max-width: 420px; }
.modal textarea { width: 100%; padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px;
  font-size: 0.9rem; font-family: inherit; }
.modal-actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.75rem; }
</style>

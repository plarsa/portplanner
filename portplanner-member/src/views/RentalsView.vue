<template>
  <div>
    <h2>Hyra</h2>

    <section v-if="myBookings.length" class="section">
      <p class="section-title">Mina bokningar</p>
      <div v-for="b in myBookings" :key="b.id" class="panel booking">
        <div class="booking-head">
          <span class="obj-name">{{ b.objectName }}</span>
          <span class="pill" :class="pillClass(b.status)">{{ statusLabel(b.status) }}</span>
        </div>
        <p class="when num">{{ formatTime(b.startAt) }} – {{ formatTime(b.endAt) }}</p>
        <div v-if="canCancel(b) || canReturn(b)" class="booking-actions">
          <button v-if="canCancel(b)" class="btn btn-secondary btn-sm" @click="cancel(b)">Avboka</button>
          <button v-if="canReturn(b)" class="btn btn-primary btn-sm" @click="openReturn(b)">Återlämna</button>
        </div>
      </div>
    </section>

    <section class="section">
      <p class="section-title">Objekt att hyra</p>
      <p v-if="!objects.length" class="empty">Inget är öppet för bokning just nu.</p>
      <div v-for="o in objects" :key="o.id" class="panel object" :class="{ on: selected?.id === o.id }">
        <img v-if="o.hasImage" :src="`/api/me/rentals/objects/${o.id}/image`" alt="" class="thumb" />
        <div class="obj-body">
          <h3>{{ o.name }}</h3>
          <div v-if="o.description" class="desc markdown-body" v-html="renderMarkdown(o.description)"></div>
          <ul class="ticks">
            <li>Längst <span class="num">{{ o.maxDurationHours }}</span> timmar per bokning</li>
            <li>Kan bokas upp till <span class="num">{{ o.maxDaysAhead }}</span> dagar fram</li>
            <li v-if="o.requiresApproval">Bokningen måste godkännas av hamnen</li>
          </ul>
          <button class="btn btn-primary btn-sm" @click="choose(o)">
            {{ selected?.id === o.id ? 'Vald' : 'Boka' }}
          </button>
        </div>
      </div>
    </section>

    <section v-if="selected" class="section">
      <p class="section-title">Kalender – {{ selected.name }}</p>
      <BookingCalendar
        :busy="busy"
        :maxDurationHours="selected.maxDurationHours"
        :maxDaysAhead="selected.maxDaysAhead"
        @select="pick" />
      <button class="btn btn-primary book" :disabled="!chosen" @click="book">
        {{ chosen ? 'Boka vald tid' : 'Välj en tid i kalendern' }}
      </button>
    </section>

    <div v-if="returning" class="modal-backdrop" @click.self="returning = null">
      <div class="modal">
        <p class="section-title">Återlämna {{ returning.objectName }}</p>
        <p class="hint">Rapportera fel och brister, eller lämna tomt om allt är i ordning.</p>
        <textarea class="input" v-model="faultText" rows="4" placeholder="t.ex. Storseglet är trasigt"></textarea>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="returning = null">Avbryt</button>
          <button class="btn btn-primary btn-sm" @click="doReturn">Återlämna</button>
        </div>
      </div>
    </div>

    <p v-if="err" class="error">{{ err }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import BookingCalendar from '../components/BookingCalendar.vue'
import { renderMarkdown } from '../lib/markdown'
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

/** Yellow waits for approval, green is live, blue is closed, red was refused. */
const PILL = {
  PENDING: 'pill-warn', CONFIRMED: 'pill-ok', RETURNED: 'pill-info',
  REJECTED: 'pill-bad', CANCELLED: 'pill-idle',
}
const pillClass = (s) => PILL[s] ?? 'pill-idle'

function formatTime(t) {
  return t ? new Date(t).toLocaleString('sv-SE', { dateStyle: 'short', timeStyle: 'short' }) : ''
}

const canCancel = (b) =>
  ['PENDING', 'CONFIRMED'].includes(b.status) && new Date(b.startAt) > new Date()
// The backend refuses a return before the start time, so do not offer the button yet.
const canReturn = (b) => b.status === 'CONFIRMED' && new Date(b.startAt) <= new Date()

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
h2 { font-size: 1.45rem; margin: 0 0 24px; }

.booking-head { display: flex; justify-content: space-between; align-items: center; gap: 14px; }
.obj-name { font-weight: 600; color: var(--ink); }
.when { margin: 6px 0 0; color: var(--text-muted); }
.booking-actions { display: flex; gap: 10px; margin-top: 14px; }

.object { padding: 0; overflow: hidden; }
.object.on { border-color: var(--sea); }
.thumb { width: 100%; height: 170px; object-fit: cover; background: var(--paper-deep); }
.obj-body { padding: 18px 22px; display: flex; flex-direction: column; align-items: flex-start; gap: 10px; }
.desc { font-size: 0.9rem; color: var(--text-muted); }

/* The guide's ticks: the bullet is a 14px sea rule. */
.ticks { list-style: none; padding: 0; margin: 0; font-size: 0.9rem; color: var(--text-muted); }
.ticks li { display: flex; align-items: baseline; gap: 10px; padding: 2px 0; }
.ticks li::before { content: ""; width: 14px; height: 1px; background: var(--sea); flex-shrink: 0; }

/* The description is injected with v-html, so its elements need :deep to be reached. */
.markdown-body :deep(h4) { font-size: 0.95rem; margin: 10px 0 2px; color: var(--ink); }
.markdown-body :deep(h5) { font-size: 0.9rem; margin: 10px 0 2px; color: var(--ink); }
.markdown-body :deep(p) { margin: 4px 0; }
.markdown-body :deep(ul) { margin: 4px 0; padding-left: 18px; }
.markdown-body :deep(li) { margin: 2px 0; }
.markdown-body :deep(> :first-child) { margin-top: 0; }
.markdown-body :deep(> :last-child) { margin-bottom: 0; }

.book { margin-top: 14px; width: 100%; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.modal textarea { width: 100%; font-family: inherit; resize: vertical; }
</style>

<template>
  <AppLayout>
    <div class="toolbar">
      <h2>Uthyrning</h2>
      <button class="btn btn-primary btn-sm" @click="openCreate">Nytt objekt</button>
    </div>

    <div class="tabs">
      <button :class="{ on: tab === 'objects' }" @click="tab = 'objects'">Objekt</button>
      <button :class="{ on: tab === 'faults' }" @click="selectFaults">
        Felrapporter<span v-if="openFaultCount" class="count">{{ openFaultCount }}</span>
      </button>
    </div>

    <!-- Objects tab -->
    <template v-if="tab === 'objects'">
      <div class="objects">
        <div v-for="o in objects" :key="o.id" class="card" :class="{ selected: selected?.id === o.id }">
          <img v-if="o.hasImage" :src="imageUrl(o)" alt="" class="thumb" />
          <div v-else class="thumb empty-thumb">Ingen bild</div>
          <div class="card-body">
            <h3>{{ o.name }}</h3>
            <div v-if="o.description" class="desc markdown-body" v-html="renderMarkdown(o.description)"></div>
            <p v-else class="desc empty-desc">Ingen beskrivning</p>
            <ul class="rules">
              <li>{{ o.bookable ? 'Öppen för bokning' : 'Stängd för bokning' }}</li>
              <li>Längst {{ o.maxDurationHours }} timmar</li>
              <li>Högst {{ o.maxDaysAhead }} dagar fram</li>
              <li>{{ o.requiresApproval ? 'Kräver godkännande' : 'Inget godkännande' }}</li>
            </ul>
            <div class="card-actions">
              <button class="btn btn-secondary btn-sm" @click="select(o)">Bokningar</button>
              <button class="btn btn-secondary btn-sm" @click="openEdit(o)">Redigera</button>
              <label class="file-btn">
                Bild
                <input class="input" type="file" accept="image/jpeg,image/png,image/webp" @change="upload(o, $event)" />
              </label>
              <button class="btn btn-secondary btn-sm" v-if="o.hasImage" @click="removeImage(o)">Ta bort bild</button>
              <button class="btn btn-danger btn-sm" @click="remove(o)">Ta bort</button>
            </div>
          </div>
        </div>
        <p v-if="!objects.length" class="empty">Inga uthyrningsobjekt registrerade</p>
      </div>

      <!-- Bookings for the selected object -->
      <div v-if="selected" class="section">
        <div class="toolbar">
          <h3>Bokningar – {{ selected.name }}</h3>
          <button class="btn btn-primary btn-sm" @click="openBookFor">Boka åt medlem</button>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Medlem</th><th>Från</th><th>Till</th><th>Status</th><th>Återlämnad</th><th></th></tr>
            </thead>
            <tbody>
              <tr v-for="b in bookings" :key="b.id">
                <td>{{ b.personName }}</td>
                <td>{{ formatTime(b.startAt) }}</td>
                <td>{{ formatTime(b.endAt) }}</td>
                <td><span class="pill" :class="bookingPill(b.status)">{{ statusText(b.status) }}</span></td>
                <td>{{ b.returnedAt ? formatTime(b.returnedAt) : '–' }}</td>
                <td class="actions">
                  <button class="btn btn-secondary btn-sm" v-if="b.status === 'PENDING'" @click="act(approveBooking, b.id)">Godkänn</button>
                  <button v-if="b.status === 'PENDING'" class="btn btn-danger btn-sm" @click="act(rejectBooking, b.id)">Avslå</button>
                  <button class="btn btn-secondary btn-sm" v-if="canReturn(b)" @click="openReturn(b)">Återlämna</button>
                  <button v-if="['PENDING','CONFIRMED'].includes(b.status)" class="btn btn-danger btn-sm"
                          @click="act(cancelBooking, b.id)">Avboka</button>
                </td>
              </tr>
              <tr v-if="!bookings.length">
                <td colspan="6" class="empty">Inga bokningar i perioden</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- Fault reports tab -->
    <div v-else class="table-wrap">
      <table>
        <thead>
          <tr><th>Objekt</th><th>Rapporterad av</th><th>Tid</th><th>Beskrivning</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="f in faults" :key="f.id">
            <td>{{ f.objectName }}</td>
            <td>{{ f.reportedBy }}</td>
            <td>{{ formatTime(f.reportedAt) }}</td>
            <td class="desc-cell">{{ f.description }}</td>
            <td class="actions">
              <button class="btn btn-secondary btn-sm" @click="resolve(f)">Åtgärdad</button>
            </td>
          </tr>
          <tr v-if="!faults.length">
            <td colspan="5" class="empty">Inga öppna felrapporter</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Object form -->
    <BaseModal v-if="modal" :title="editing ? 'Redigera objekt' : 'Nytt objekt'"
               @close="modal = false" @save="save">
      <div class="form-grid">
        <label class="full">Namn *
          <input class="input" v-model="form.name" placeholder="t.ex. Upptagningstrailer" required />
        </label>
        <label class="full">Beskrivning
          <MarkdownEditor v-model="form.description"
                          placeholder="## Rubrik&#10;Text om objektet. Kräver dragkrok." />
        </label>
        <label>Längsta bokning (timmar) *
          <input class="input" v-model.number="form.maxDurationHours" type="number" min="1" required />
        </label>
        <label>Dagar fram i tiden *
          <input class="input" v-model.number="form.maxDaysAhead" type="number" min="1" required />
        </label>
        <label class="check"><input class="input" v-model="form.bookable" type="checkbox" /> Öppen för bokning</label>
        <label class="check"><input class="input" v-model="form.requiresApproval" type="checkbox" /> Kräver godkännande</label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>

    <!-- Book for a member -->
    <BaseModal v-if="bookModal" title="Boka åt medlem" saveLabel="Boka"
               @close="bookModal = false" @save="bookFor">
      <div class="form-grid">
        <label class="full">Medlem *
          <select class="input" v-model.number="bookForm.personId" required>
            <option value="">Välj medlem…</option>
            <option v-for="p in persons" :key="p.id" :value="p.id">
              {{ p.firstName }} {{ p.lastName }}
            </option>
          </select>
        </label>
        <label>Från *
          <input class="input" v-model="bookForm.startAt" type="datetime-local" step="3600" required />
        </label>
        <label>Till *
          <input class="input" v-model="bookForm.endAt" type="datetime-local" step="3600" required />
        </label>
      </div>
      <p class="hint">Bokningen kräver inget godkännande.</p>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>

    <!-- Return -->
    <BaseModal v-if="returnModal" title="Återlämna" saveLabel="Återlämna"
               @close="returnModal = false" @save="doReturn">
      <label class="full">Fel och brister
        <textarea class="input" v-model="faultText" rows="4" placeholder="Lämna tomt om allt är i ordning…"></textarea>
      </label>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>
  </AppLayout>
</template>

<script setup>
/* Booking state in the guide's five roles. */
const bookingPill = (s) =>
  ({ PENDING: 'pill-warn', CONFIRMED: 'pill-ok', RETURNED: 'pill-info',
     REJECTED: 'pill-bad', CANCELLED: 'pill-idle' })[s] ?? 'pill-idle'

import { ref, onMounted, computed } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import BaseModal from '../components/BaseModal.vue'
import MarkdownEditor from '../components/MarkdownEditor.vue'
import { renderMarkdown } from '../lib/markdown'
import {
  getObjects, createObject, updateObject, deleteObject, uploadImage, deleteImage,
  getBookings, createBooking, approveBooking, rejectBooking, returnBooking, cancelBooking,
  getFaultReports, resolveFaultReport,
} from '../api/rentals'
import { getPersons } from '../api/persons'

const tab = ref('objects')
const objects = ref([])
const bookings = ref([])
const faults = ref([])
const persons = ref([])
const selected = ref(null)
const openFaultCount = ref(0)

const modal = ref(false)
const bookModal = ref(false)
const returnModal = ref(false)
const editing = ref(null)
const returning = ref(null)
const faultText = ref('')
const err = ref('')
// Cache-buster: the URL does not change when the image is replaced.
const imageVersion = ref(0)

const emptyForm = () => ({
  name: '', description: '', bookable: true,
  maxDurationHours: 24, requiresApproval: false, maxDaysAhead: 30,
})
const form = ref(emptyForm())
const bookForm = ref({ personId: '', startAt: '', endAt: '' })

const imageUrl = (o) => `/api/rentals/objects/${o.id}/image?v=${imageVersion.value}`

function formatTime(t) {
  return t ? new Date(t).toLocaleString('sv-SE', { dateStyle: 'short', timeStyle: 'short' }) : '–'
}

const STATUS_TEXT = {
  PENDING: 'Väntar', CONFIRMED: 'Bekräftad', REJECTED: 'Avslagen',
  CANCELLED: 'Avbokad', RETURNED: 'Återlämnad',
}
const statusText = (s) => STATUS_TEXT[s] || s

// The backend refuses a return before the start time.
const canReturn = (b) => b.status === 'CONFIRMED' && new Date(b.startAt) <= new Date()

async function load() {
  const { data } = await getObjects()
  objects.value = data
  const open = await getFaultReports({})
  openFaultCount.value = open.data.length
  if (selected.value) {
    selected.value = data.find(o => o.id === selected.value.id) || null
  }
}

async function select(o) {
  selected.value = o
  const from = new Date()
  from.setMonth(from.getMonth() - 1)
  const to = new Date()
  to.setMonth(to.getMonth() + 6)
  const { data } = await getBookings(o.id, isoLocal(from), isoLocal(to))
  bookings.value = data
}

// The API takes a local date-time without a zone, which toISOString would shift.
function isoLocal(d) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:00:00`
}

async function selectFaults() {
  tab.value = 'faults'
  const { data } = await getFaultReports({})
  faults.value = data
  openFaultCount.value = data.length
}

function openCreate() {
  editing.value = null
  form.value = emptyForm()
  err.value = ''
  modal.value = true
}

function openEdit(o) {
  editing.value = o
  form.value = {
    name: o.name, description: o.description || '', bookable: o.bookable,
    maxDurationHours: o.maxDurationHours, requiresApproval: o.requiresApproval,
    maxDaysAhead: o.maxDaysAhead,
  }
  err.value = ''
  modal.value = true
}

async function save() {
  err.value = ''
  try {
    if (editing.value) await updateObject(editing.value.id, form.value)
    else await createObject(form.value)
    modal.value = false
    await load()
  } catch (e) {
    err.value = e.response?.data?.error || 'Något gick fel'
  }
}

async function remove(o) {
  if (!confirm(`Ta bort objektet "${o.name}"?`)) return
  try {
    await deleteObject(o.id)
    if (selected.value?.id === o.id) selected.value = null
    await load()
  } catch (e) {
    alert(e.response?.data?.error || 'Kunde inte ta bort objektet')
  }
}

async function upload(o, event) {
  const file = event.target.files?.[0]
  if (!file) return
  try {
    await uploadImage(o.id, file)
    imageVersion.value++
    await load()
  } catch (e) {
    alert(e.response?.data?.error || 'Kunde inte ladda upp bilden')
  } finally {
    event.target.value = ''
  }
}

async function removeImage(o) {
  try {
    await deleteImage(o.id)
    imageVersion.value++
    await load()
  } catch (e) {
    alert(e.response?.data?.error || 'Kunde inte ta bort bilden')
  }
}

async function openBookFor() {
  err.value = ''
  bookForm.value = { personId: '', startAt: '', endAt: '' }
  const { data } = await getPersons()
  persons.value = data
  bookModal.value = true
}

async function bookFor() {
  err.value = ''
  try {
    await createBooking({
      objectId: selected.value.id,
      personId: bookForm.value.personId,
      startAt: withSeconds(bookForm.value.startAt),
      endAt: withSeconds(bookForm.value.endAt),
    })
    bookModal.value = false
    await select(selected.value)
  } catch (e) {
    err.value = e.response?.data?.error || 'Kunde inte boka'
  }
}

// datetime-local gives "YYYY-MM-DDTHH:mm"; the API wants seconds too.
const withSeconds = (v) => (v && v.length === 16 ? `${v}:00` : v)

function openReturn(b) {
  returning.value = b
  faultText.value = ''
  err.value = ''
  returnModal.value = true
}

async function doReturn() {
  err.value = ''
  try {
    await returnBooking(returning.value.id, faultText.value)
    returnModal.value = false
    await select(selected.value)
    await load()
  } catch (e) {
    err.value = e.response?.data?.error || 'Kunde inte återlämna'
  }
}

async function act(fn, id) {
  try {
    await fn(id)
    await select(selected.value)
    await load()
  } catch (e) {
    alert(e.response?.data?.error || 'Något gick fel')
  }
}

async function resolve(f) {
  try {
    await resolveFaultReport(f.id)
    await selectFaults()
  } catch (e) {
    alert(e.response?.data?.error || 'Kunde inte markera som åtgärdad')
  }
}

onMounted(load)
</script>

<style scoped>
.tabs { display: flex; gap: 4px; margin-bottom: 22px; }
.tabs button {
  background: none;
  border: 0;
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  font-family: var(--font);
  font-size: 0.92rem;
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
}
.tabs button:hover { background: var(--paper-deep); color: var(--ink); }
.tabs button.on { background: var(--sea-pale); color: var(--ink); }
.count {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  color: var(--warn-text);
  background: var(--warn-bg);
  border: 1px solid var(--warn-border);
  border-radius: var(--radius-sm);
  padding: 0 5px;
  margin-left: 8px;
}

.objects { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; }
.card {
  background: var(--surface);
  border: 1px solid var(--paper-border);
  border-radius: var(--radius);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.card.selected { border-color: var(--sea); }
.thumb { width: 100%; height: 150px; object-fit: cover; display: block; background: var(--paper-deep); }
.empty-thumb { display: flex; align-items: center; justify-content: center; color: var(--text-muted); font-size: 0.88rem; }
.card-body { padding: 18px; display: flex; flex-direction: column; gap: 10px; flex: 1; }
.card-body h3 { margin: 0; }
.desc { color: var(--text-muted); font-size: 0.9rem; margin: 0; }
.empty-desc { color: var(--text-muted); }

/* The description is injected with v-html, so its elements need :deep to be reached. */
.markdown-body :deep(h4) { font-size: 0.95rem; margin: 10px 0 2px; color: var(--ink); }
.markdown-body :deep(h5) { font-size: 0.9rem; margin: 10px 0 2px; color: var(--ink); }
.markdown-body :deep(p) { margin: 4px 0; }
.markdown-body :deep(ul) { margin: 4px 0; padding-left: 18px; }
.markdown-body :deep(li) { margin: 2px 0; }
.markdown-body :deep(> :first-child) { margin-top: 0; }
.markdown-body :deep(> :last-child) { margin-bottom: 0; }

/* The guide's ticks: the bullet is a 14px sea rule. */
.rules { list-style: none; padding: 0; margin: 0; font-size: 0.88rem; color: var(--text-muted); }
.rules li { display: flex; align-items: baseline; gap: 10px; padding: 2px 0; }
.rules li::before { content: ""; width: 14px; height: 1px; background: var(--sea); flex-shrink: 0; }

.card-actions, .actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: auto; }
.file-btn {
  position: relative;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  padding: 9px 15px;
  border: 1px solid var(--paper-border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--sea);
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
}
.file-btn:hover { background: var(--sea-pale); border-color: var(--info-border); }
.file-btn input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }

.section { margin-top: 28px; }
.desc-cell { max-width: 260px; font-size: 0.88rem; color: var(--text-muted); }
.empty { color: var(--text-muted); text-align: center; padding: 44px; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 14px; }
.form-grid .full { grid-column: 1 / -1; }
.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.form-grid label.check {
  flex-direction: row;
  align-items: center;
  gap: 8px;
  font-family: var(--font);
  font-size: 0.92rem;
  letter-spacing: 0;
  text-transform: none;
  color: var(--text);
}
.form-grid .input { font-family: var(--font); text-transform: none; letter-spacing: 0; }
.form-grid textarea.input { resize: vertical; }
@media (max-width: 620px) { .form-grid { grid-template-columns: 1fr; } }
</style>

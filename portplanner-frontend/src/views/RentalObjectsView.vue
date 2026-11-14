<template>
  <AppLayout>
    <div class="page-header">
      <h2>Uthyrning</h2>
      <button class="btn-primary" @click="openCreate">+ Nytt objekt</button>
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
            <p class="desc">{{ o.description || 'Ingen beskrivning' }}</p>
            <ul class="rules">
              <li>{{ o.bookable ? 'Öppen för bokning' : 'Stängd för bokning' }}</li>
              <li>Längst {{ o.maxDurationHours }} timmar</li>
              <li>Högst {{ o.maxDaysAhead }} dagar fram</li>
              <li>{{ o.requiresApproval ? 'Kräver godkännande' : 'Inget godkännande' }}</li>
            </ul>
            <div class="card-actions">
              <button @click="select(o)">Bokningar</button>
              <button @click="openEdit(o)">Redigera</button>
              <label class="file-btn">
                Bild
                <input type="file" accept="image/jpeg,image/png,image/webp" @change="upload(o, $event)" />
              </label>
              <button v-if="o.hasImage" @click="removeImage(o)">Ta bort bild</button>
              <button class="danger" @click="remove(o)">Ta bort</button>
            </div>
          </div>
        </div>
        <p v-if="!objects.length" class="empty">Inga uthyrningsobjekt registrerade</p>
      </div>

      <!-- Bookings for the selected object -->
      <div v-if="selected" class="panel">
        <div class="panel-header">
          <h3>Bokningar – {{ selected.name }}</h3>
          <button class="btn-primary" @click="openBookFor">Boka åt medlem</button>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Från</th><th>Till</th><th>Status</th><th>Återlämnad</th><th></th></tr>
            </thead>
            <tbody>
              <tr v-for="b in bookings" :key="b.id">
                <td>{{ formatTime(b.startAt) }}</td>
                <td>{{ formatTime(b.endAt) }}</td>
                <td><span class="status" :class="b.status.toLowerCase()">{{ statusText(b.status) }}</span></td>
                <td>{{ b.returnedAt ? formatTime(b.returnedAt) : '–' }}</td>
                <td class="actions">
                  <button v-if="b.status === 'PENDING'" @click="act(approveBooking, b.id)">Godkänn</button>
                  <button v-if="b.status === 'PENDING'" class="danger" @click="act(rejectBooking, b.id)">Avslå</button>
                  <button v-if="b.status === 'CONFIRMED'" @click="openReturn(b)">Återlämna</button>
                  <button v-if="['PENDING','CONFIRMED'].includes(b.status)" class="danger"
                          @click="act(cancelBooking, b.id)">Avboka</button>
                </td>
              </tr>
              <tr v-if="!bookings.length">
                <td colspan="5" class="empty">Inga bokningar i perioden</td>
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
              <button @click="resolve(f)">Åtgärdad</button>
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
          <input v-model="form.name" placeholder="t.ex. Upptagningstrailer" required />
        </label>
        <label class="full">Beskrivning
          <input v-model="form.description" placeholder="t.ex. Kräver dragkrok" />
        </label>
        <label>Längsta bokning (timmar) *
          <input v-model.number="form.maxDurationHours" type="number" min="1" required />
        </label>
        <label>Dagar fram i tiden *
          <input v-model.number="form.maxDaysAhead" type="number" min="1" required />
        </label>
        <label class="check"><input v-model="form.bookable" type="checkbox" /> Öppen för bokning</label>
        <label class="check"><input v-model="form.requiresApproval" type="checkbox" /> Kräver godkännande</label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>

    <!-- Book for a member -->
    <BaseModal v-if="bookModal" title="Boka åt medlem" saveLabel="Boka"
               @close="bookModal = false" @save="bookFor">
      <div class="form-grid">
        <label class="full">Medlem *
          <select v-model.number="bookForm.personId" required>
            <option value="">Välj medlem…</option>
            <option v-for="p in persons" :key="p.id" :value="p.id">
              {{ p.firstName }} {{ p.lastName }}
            </option>
          </select>
        </label>
        <label>Från *
          <input v-model="bookForm.startAt" type="datetime-local" step="3600" required />
        </label>
        <label>Till *
          <input v-model="bookForm.endAt" type="datetime-local" step="3600" required />
        </label>
      </div>
      <p class="hint">Bokningen kräver inget godkännande.</p>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>

    <!-- Return -->
    <BaseModal v-if="returnModal" title="Återlämna" saveLabel="Återlämna"
               @close="returnModal = false" @save="doReturn">
      <label class="full">Fel och brister
        <textarea v-model="faultText" rows="4" placeholder="Lämna tomt om allt är i ordning…"></textarea>
      </label>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import BaseModal from '../components/BaseModal.vue'
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
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; }
h2 { font-size: 1.5rem; }
.tabs { display: flex; gap: 0.5rem; margin-bottom: 1.25rem; }
.tabs button { background: white; border: 1px solid #ddd; }
.tabs button.on { background: #1a3a5c; color: white; border-color: #1a3a5c; }
.count { background: #c0392b; color: white; border-radius: 10px; padding: 0 0.4rem; margin-left: 0.4rem; font-size: 0.75rem; }

.objects { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1rem; }
.card { background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.07); display: flex; flex-direction: column; }
.card.selected { outline: 2px solid #1a3a5c; }
.thumb { width: 100%; height: 150px; object-fit: cover; display: block; background: #eef2f7; }
.empty-thumb { display: flex; align-items: center; justify-content: center; color: #9aa7b5; font-size: 0.85rem; }
.card-body { padding: 1rem; display: flex; flex-direction: column; gap: 0.5rem; flex: 1; }
.card-body h3 { font-size: 1.05rem; margin: 0; }
.desc { color: #666; font-size: 0.85rem; margin: 0; }
.rules { list-style: none; padding: 0; margin: 0; font-size: 0.8rem; color: #555; }
.rules li { padding: 0.1rem 0; }
.card-actions { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-top: auto; }
.file-btn { position: relative; overflow: hidden; display: inline-flex; align-items: center;
  padding: 0.35rem 0.8rem; border-radius: 5px; cursor: pointer; font-size: 0.85rem;
  background: #e8f0fe; color: #1a3a5c; font-weight: 400; }
.file-btn input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }

.panel { margin-top: 1.75rem; }
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
.panel-header h3 { font-size: 1.1rem; }

.table-wrap { background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.07); }
table { width: 100%; border-collapse: collapse; }
th { background: #f8f8f8; padding: 0.75rem 1rem; text-align: left; font-size: 0.85rem; color: #555; border-bottom: 1px solid #eee; }
td { padding: 0.75rem 1rem; border-bottom: 1px solid #f0f0f0; font-size: 0.9rem; }
.desc-cell { color: #666; font-size: 0.85rem; max-width: 260px; }
.actions { display: flex; gap: 0.5rem; flex-wrap: wrap; }
.empty { color: #999; text-align: center; padding: 2rem; }
.status { border-radius: 4px; padding: 0.15rem 0.5rem; font-size: 0.78rem; font-weight: 700; }
.status.pending { background: #fff4e0; color: #9a6400; }
.status.confirmed { background: #e8f6ec; color: #1d7a3a; }
.status.returned { background: #eef2f7; color: #4a5a6a; }
.status.rejected, .status.cancelled { background: #fdeaea; color: #b3362c; }

button { padding: 0.35rem 0.8rem; border-radius: 5px; border: none; cursor: pointer; font-size: 0.85rem; background: #e8f0fe; color: #1a3a5c; }
button:hover { background: #d0e2ff; }
button.danger { background: #fee; color: #c0392b; }
button.danger:hover { background: #fcc; }
.btn-primary { background: #1a3a5c; color: white; padding: 0.5rem 1.1rem; border-radius: 6px; border: none; cursor: pointer; }
.btn-primary:hover { background: #234e7a; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.form-grid .full { grid-column: 1 / -1; }
label { display: flex; flex-direction: column; font-size: 0.85rem; font-weight: 600; gap: 0.3rem; }
label.check { flex-direction: row; align-items: center; gap: 0.5rem; font-weight: 400; }
label input, label select, label textarea { padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.95rem; font-weight: 400; font-family: inherit; }
label.check input { padding: 0; }
.hint { color: #666; font-size: 0.8rem; margin-top: 0.5rem; }
.error { color: #c0392b; font-size: 0.85rem; margin-top: 0.5rem; }
</style>

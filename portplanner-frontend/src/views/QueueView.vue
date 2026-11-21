<template>
  <AppLayout>
    <div class="toolbar">
      <h2>Kö</h2>
      <div class="right">
        <button class="btn btn-primary btn-sm" @click="openAdd">Lägg till i kö</button>
      </div>
    </div>

    <p v-if="!queue.length" class="empty">Kön är tom</p>

    <!-- The queue is a register: a mono index, the entry, and hairlines between. -->
    <div v-if="queue.length" class="panel queue">
      <div v-for="(entry, idx) in queue" :key="entry.id" class="entry">
        <div class="entry-head">
          <span class="index num">{{ String(idx + 1).padStart(2, '0') }}</span>
          <div class="entry-info">
            <strong>{{ entry.personName }}</strong>
            <span v-if="entry.boatName" class="sub">
              {{ entry.boatName }} –
              <span class="num">{{ entry.boatLengthM }}×{{ entry.boatWidthM }} m</span>
              <span v-if="entry.boatDraftM"> / <span class="num">{{ entry.boatDraftM }} m</span> djup</span>
            </span>
            <span v-else class="sub muted">Ingen båt angiven</span>
            <span class="sub muted">Anmäld <span class="num">{{ formatDate(entry.requestedDate) }}</span></span>
            <span v-if="entry.notes" class="notes">{{ entry.notes }}</span>
          </div>
          <div class="entry-actions">
            <button v-if="entry.boatId" class="btn btn-secondary btn-sm" @click="loadSuggestions(entry)">
              {{ activeSuggestionId === entry.id ? 'Dölj förslag' : 'Visa lediga platser' }}
            </button>
            <button class="btn btn-secondary btn-sm" @click="openEdit(entry)">Redigera</button>
            <button class="btn btn-danger btn-sm" @click="cancel(entry)">Avboka</button>
          </div>
        </div>

        <div v-if="activeSuggestionId === entry.id" class="suggestions">
          <p v-if="suggestions.length === 0" class="empty">
            Inga lediga platser passar denna båt just nu
          </p>
          <table v-else>
            <thead>
              <tr><th>Brygga</th><th>Plats</th><th>Max L</th><th>Max B</th><th>Max djup</th><th></th></tr>
            </thead>
            <tbody>
              <tr v-for="s in suggestions" :key="s.id">
                <td>{{ s.dockName }}</td>
                <td class="num">{{ s.slipNumber }}</td>
                <td class="num muted">{{ s.maxLengthM }} m</td>
                <td class="num muted">{{ s.maxWidthM }} m</td>
                <td class="num muted">{{ s.maxDraftM ? s.maxDraftM + ' m' : '–' }}</td>
                <td><button class="btn btn-primary btn-sm" @click="assign(entry, s)">Tilldela</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Add to queue modal -->
    <BaseModal v-if="modal" title="Lägg till i kö" @close="modal = false" @save="saveAdd">
      <div class="form-col">
        <label>Person *
          <input v-model="personSearch" type="text" placeholder="Sök namn…" class="input search-input" @input="form.personId = ''; form.boatId = ''" />
          <select class="input" v-model.number="form.personId" required size="5" @change="form.boatId = ''; boatSearch = ''">
            <option value="">Välj person…</option>
            <option v-for="p in filteredPersons" :key="p.id" :value="p.id">
              {{ p.firstName }} {{ p.lastName }}
            </option>
          </select>
        </label>
        <label>Båt
          <input v-model="boatSearch" type="text" placeholder="Sök båt…" class="input search-input" />
          <select class="input" v-model.number="form.boatId" size="4">
            <option value="">Ingen båt (valfritt)…</option>
            <option v-for="b in filteredBoatsForPerson" :key="b.id" :value="b.id">
              {{ b.model }} – {{ b.lengthM }}×{{ b.widthM }}m
            </option>
          </select>
        </label>
        <label>Datum i kö
          <input class="input" v-model="form.requestedDate" type="text" placeholder="ÅÅÅÅ-MM-DD" maxlength="10" />
        </label>
        <label>Anteckning
          <textarea class="input" v-model="form.notes" rows="3" placeholder="Ev. önskemål om plats…" />
        </label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>
    <!-- Edit queue entry modal -->
    <BaseModal v-if="editModal && editingEntry" :title="'Redigera – ' + editingEntry.personName" @close="editModal = false" @save="saveEdit">
      <div class="form-col">
        <label>Båt
          <input v-model="editBoatSearch" type="text" placeholder="Sök båt…" class="input search-input" />
          <select class="input" v-model.number="editForm.boatId" size="4">
            <option value="">Ingen båt (valfritt)…</option>
            <option v-for="b in filteredBoatsForEdit" :key="b.id" :value="b.id">
              {{ b.model }} – {{ b.lengthM }}×{{ b.widthM }}m
            </option>
          </select>
        </label>
        <label>Datum i kö
          <input class="input" v-model="editForm.requestedDate" type="text" placeholder="ÅÅÅÅ-MM-DD" maxlength="10" />
        </label>
        <label>Anteckning
          <textarea class="input" v-model="editForm.notes" rows="4" placeholder="Ev. önskemål om plats…" />
        </label>
      </div>
      <p v-if="editErr" class="error">{{ editErr }}</p>
    </BaseModal>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import BaseModal from '../components/BaseModal.vue'
import { getQueue, addToQueue, updateQueueEntry, getSuggestions, assignFromQueue, cancelQueueEntry } from '../api/queue'
import { getPersons } from '../api/persons'
import { getBoats } from '../api/boats'

const queue = ref([])
const persons = ref([])
const allBoats = ref([])
const modal = ref(false)
const editModal = ref(false)
const editingEntry = ref(null)
const err = ref('')
const editErr = ref('')
const form = ref({ personId: '', boatId: '', notes: '', requestedDate: '' })
const editForm = ref({ boatId: '', notes: '', requestedDate: '' })
const personSearch = ref('')
const boatSearch = ref('')
const editBoatSearch = ref('')
const activeSuggestionId = ref(null)
const suggestions = ref([])

function todayStr() { return new Date().toISOString().slice(0, 10) }

const filteredPersons = computed(() => {
  const q = personSearch.value.toLowerCase()
  const list = q
    ? persons.value.filter(p => (p.firstName + ' ' + p.lastName).toLowerCase().includes(q))
    : persons.value
  return list.slice().sort((a, b) =>
    (a.lastName + ' ' + a.firstName).localeCompare(b.lastName + ' ' + b.firstName, 'sv'))
})

const boatsForPerson = computed(() =>
  form.value.personId ? allBoats.value.filter(b => b.ownerId === form.value.personId) : [])

const filteredBoatsForPerson = computed(() => {
  const q = boatSearch.value.toLowerCase()
  const list = q
    ? boatsForPerson.value.filter(b => b.model.toLowerCase().includes(q))
    : boatsForPerson.value
  return list.slice().sort((a, b) => a.model.localeCompare(b.model, 'sv'))
})

const boatsForEditEntry = computed(() =>
  editingEntry.value ? allBoats.value.filter(b => b.ownerId === editingEntry.value.personId) : [])

const filteredBoatsForEdit = computed(() => {
  const q = editBoatSearch.value.toLowerCase()
  const list = q
    ? boatsForEditEntry.value.filter(b => b.model.toLowerCase().includes(q))
    : boatsForEditEntry.value
  return list.slice().sort((a, b) => a.model.localeCompare(b.model, 'sv'))
})

function formatDate(dt) {
  return dt ? new Date(dt).toLocaleDateString('sv-SE') : ''
}

async function load() {
  const [q, p, b] = await Promise.all([getQueue(), getPersons(), getBoats()])
  queue.value = q.data
  persons.value = p.data
  allBoats.value = b.data
}

function openAdd() { form.value = { personId: '', boatId: '', notes: '', requestedDate: todayStr() }; personSearch.value = ''; boatSearch.value = ''; err.value = ''; modal.value = true }

async function saveAdd() {
  err.value = ''
  try {
    await addToQueue(form.value)
    modal.value = false
    await load()
  } catch (e) {
    err.value = e.response?.data?.error || 'Något gick fel'
  }
}

async function loadSuggestions(entry) {
  if (activeSuggestionId.value === entry.id) {
    activeSuggestionId.value = null
    return
  }
  const { data } = await getSuggestions(entry.id)
  suggestions.value = data
  activeSuggestionId.value = entry.id
}

async function assign(entry, slip) {
  if (!confirm(`Tilldela ${entry.personName}s båt (${entry.boatName}) till plats ${slip.slipNumber} på ${slip.dockName}?`)) return
  try {
    await assignFromQueue(entry.id, slip.id)
    activeSuggestionId.value = null
    await load()
  } catch (e) {
    alert(e.response?.data?.error || 'Något gick fel')
  }
}

function openEdit(entry) {
  editingEntry.value = entry
  editBoatSearch.value = ''
  editErr.value = ''
  editForm.value = {
    boatId: entry.boatId ?? '',
    notes: entry.notes ?? '',
    requestedDate: entry.requestedDate ? entry.requestedDate.slice(0, 10) : todayStr(),
  }
  editModal.value = true
}

async function saveEdit() {
  editErr.value = ''
  try {
    await updateQueueEntry(editingEntry.value.id, {
      personId: editingEntry.value.personId,
      boatId: editForm.value.boatId || null,
      notes: editForm.value.notes || null,
      requestedDate: editForm.value.requestedDate || null,
    })
    editModal.value = false
    await load()
  } catch (e) {
    editErr.value = e.response?.data?.error || 'Något gick fel'
  }
}

async function cancel(entry) {
  if (!confirm(`Avboka ${entry.personName} ur kön?`)) return
  await cancelQueueEntry(entry.id)
  await load()
}

onMounted(load)
</script>

<style scoped>
.queue { padding: 0; }
.entry { padding: 18px 22px; border-bottom: 1px solid var(--paper-border); }
.entry:last-child { border-bottom: 0; }
.entry-head { display: flex; align-items: flex-start; gap: 18px; }

/* The index is the register's mark: mono, muted, never a filled circle. */
.index { color: var(--text-muted); padding-top: 2px; }

.entry-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.entry-info strong { color: var(--ink); font-weight: 600; }
.sub { font-size: 0.9rem; color: var(--text); }
.notes { font-size: 0.9rem; color: var(--text-muted); }
.entry-actions { display: flex; gap: 10px; flex-shrink: 0; flex-wrap: wrap; justify-content: flex-end; }

/* A table inside the panel is not a separate card, so it carries no shadow. */
.suggestions { margin-top: 14px; border-top: 1px solid var(--paper-border); padding-top: 14px; }
.suggestions table { width: 100%; border-collapse: collapse; font-size: 0.92rem; }
.suggestions thead th {
  text-align: left;
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  padding: 8px 10px;
  background: var(--paper-deep);
}
.suggestions tbody td { padding: 8px 10px; border-bottom: 1px solid var(--paper-border); }
.suggestions tbody tr:last-child td { border-bottom: 0; }

.form-col { display: flex; flex-direction: column; gap: 14px; }
/* A composite label (search box plus list) still wears the guide's label style. */
.form-col label {
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
.form-col .input { font-family: var(--font); text-transform: none; letter-spacing: 0; }
.form-col select[size] { min-height: 6rem; }
.search-input { font-size: 0.88rem; }
@media (max-width: 760px) {
  .entry-head { flex-wrap: wrap; }
  .entry-actions { width: 100%; justify-content: flex-start; }
}
</style>

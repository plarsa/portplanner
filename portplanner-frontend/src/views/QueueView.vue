<template>
  <AppLayout>
    <div class="page-header">
      <h2>Kö</h2>
      <button class="btn-primary" @click="openAdd">+ Lägg till i kö</button>
    </div>

    <div v-if="!queue.length" class="empty-state">Kön är tom</div>

    <div v-for="(entry, idx) in queue" :key="entry.id" class="queue-card">
      <div class="queue-header">
        <div class="queue-pos">{{ idx + 1 }}</div>
        <div class="queue-info">
          <strong>{{ entry.personName }}</strong>
          <span v-if="entry.boatName" class="sub">{{ entry.boatName }} – {{ entry.boatLengthM }}×{{ entry.boatWidthM }}m
            <span v-if="entry.boatDraftM"> / {{ entry.boatDraftM }}m djup</span>
          </span>
          <span v-else class="sub muted">Ingen båt angiven</span>
          <span class="date">Anmäld: {{ formatDate(entry.requestedDate) }}</span>
          <span v-if="entry.notes" class="notes">{{ entry.notes }}</span>
        </div>
        <div class="queue-actions">
          <button v-if="entry.boatId" @click="loadSuggestions(entry)">
            {{ activeSuggestionId === entry.id ? 'Dölj förslag' : 'Visa lediga platser' }}
          </button>
          <button class="danger" @click="cancel(entry)">Avboka</button>
        </div>
      </div>

      <!-- Platseförslag -->
      <div v-if="activeSuggestionId === entry.id" class="suggestions">
        <div v-if="suggestions.length === 0" class="no-suggestions">
          Inga lediga platser passar denna båt just nu
        </div>
        <table v-else>
          <thead>
            <tr><th>Brygga</th><th>Plats</th><th>Max L</th><th>Max B</th><th>Max Djup</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="s in suggestions" :key="s.id">
              <td>{{ s.dockName }}</td>
              <td>{{ s.slipNumber }}</td>
              <td>{{ s.maxLengthM }}m</td>
              <td>{{ s.maxWidthM }}m</td>
              <td>{{ s.maxDraftM ? s.maxDraftM + 'm' : '–' }}</td>
              <td>
                <button class="assign-btn" @click="assign(entry, s)">Tilldela</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Lägg till i kö modal -->
    <BaseModal v-if="modal" title="Lägg till i kö" @close="modal = false" @save="saveAdd">
      <div class="form-col">
        <label>Person *
          <input v-model="personSearch" type="text" placeholder="Sök namn…" class="search-input" @input="form.personId = ''; form.boatId = ''" />
          <select v-model.number="form.personId" required size="5" @change="form.boatId = ''; boatSearch = ''">
            <option value="">Välj person…</option>
            <option v-for="p in filteredPersons" :key="p.id" :value="p.id">
              {{ p.firstName }} {{ p.lastName }}
            </option>
          </select>
        </label>
        <label>Båt
          <input v-model="boatSearch" type="text" placeholder="Sök båt…" class="search-input" />
          <select v-model.number="form.boatId" size="4">
            <option value="">Ingen båt (valfritt)…</option>
            <option v-for="b in filteredBoatsForPerson" :key="b.id" :value="b.id">
              {{ b.name }} – {{ b.lengthM }}×{{ b.widthM }}m
            </option>
          </select>
        </label>
        <label>Datum i kö
          <input v-model="form.requestedDate" type="date" />
        </label>
        <label>Anteckning
          <textarea v-model="form.notes" rows="3" placeholder="Ev. önskemål om plats…" />
        </label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import BaseModal from '../components/BaseModal.vue'
import { getQueue, addToQueue, getSuggestions, assignFromQueue, cancelQueueEntry } from '../api/queue'
import { getPersons } from '../api/persons'
import { getBoats } from '../api/boats'

const queue = ref([])
const persons = ref([])
const allBoats = ref([])
const modal = ref(false)
const err = ref('')
const form = ref({ personId: '', boatId: '', notes: '', requestedDate: '' })
const personSearch = ref('')
const boatSearch = ref('')
const activeSuggestionId = ref(null)
const suggestions = ref([])

function todayStr() { return new Date().toISOString().slice(0, 10) }

const filteredPersons = computed(() => {
  const q = personSearch.value.toLowerCase()
  if (!q) return persons.value
  return persons.value.filter(p =>
    (p.firstName + ' ' + p.lastName).toLowerCase().includes(q))
})

const boatsForPerson = computed(() =>
  form.value.personId ? allBoats.value.filter(b => b.ownerId === form.value.personId) : [])

const filteredBoatsForPerson = computed(() => {
  const q = boatSearch.value.toLowerCase()
  if (!q) return boatsForPerson.value
  return boatsForPerson.value.filter(b => b.name.toLowerCase().includes(q))
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

async function cancel(entry) {
  if (!confirm(`Avboka ${entry.personName} ur kön?`)) return
  await cancelQueueEntry(entry.id)
  await load()
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; }
h2 { font-size: 1.5rem; }
.empty-state { color: #999; text-align: center; padding: 3rem; background: white; border-radius: 10px; }
.queue-card { background: white; border-radius: 10px; margin-bottom: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.07); overflow: hidden; }
.queue-header { display: flex; align-items: flex-start; gap: 1rem; padding: 1rem 1.25rem; }
.queue-pos { background: #1a3a5c; color: white; border-radius: 50%; width: 2rem; height: 2rem; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 0.9rem; flex-shrink: 0; }
.queue-info { flex: 1; display: flex; flex-direction: column; gap: 0.2rem; }
.queue-info strong { font-size: 1rem; }
.sub { color: #444; font-size: 0.875rem; }
.muted { color: #bbb; }
.date { color: #999; font-size: 0.8rem; }
.notes { color: #555; font-size: 0.85rem; font-style: italic; }
.queue-actions { display: flex; gap: 0.5rem; flex-shrink: 0; }
.suggestions { border-top: 1px solid #eee; padding: 1rem 1.25rem; background: #fafafa; }
.no-suggestions { color: #999; font-size: 0.9rem; }
table { width: 100%; border-collapse: collapse; }
th { background: #f0f0f0; padding: 0.5rem 0.75rem; text-align: left; font-size: 0.82rem; color: #555; }
td { padding: 0.5rem 0.75rem; border-top: 1px solid #eee; font-size: 0.88rem; }
button { padding: 0.35rem 0.8rem; border-radius: 5px; border: none; cursor: pointer; font-size: 0.82rem; background: #e8f0fe; color: #1a3a5c; }
button:hover { background: #d0e2ff; }
button.danger { background: #fee; color: #c0392b; }
button.assign-btn { background: #1a3a5c; color: white; }
button.assign-btn:hover { background: #234e7a; }
.btn-primary { background: #1a3a5c; color: white; padding: 0.5rem 1.1rem; border-radius: 6px; border: none; cursor: pointer; }
.btn-primary:hover { background: #234e7a; }
.form-col { display: flex; flex-direction: column; gap: 0.85rem; }
label { display: flex; flex-direction: column; font-size: 0.85rem; font-weight: 600; gap: 0.3rem; }
label select, label textarea, label input[type="date"] { padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.9rem; font-weight: 400; }
.search-input { padding: 0.4rem 0.6rem; border: 1px solid #ccc; border-radius: 5px; font-size: 0.85rem; font-weight: 400; margin-bottom: 0.2rem; }
label select[size] { min-height: 6rem; }
.error { color: #c0392b; font-size: 0.85rem; margin-top: 0.5rem; }
</style>

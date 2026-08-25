<template>
  <AppLayout>
    <div class="page-header">
      <h2>Båtar</h2>
      <div class="header-right">
        <input v-model="search" class="search-input" placeholder="Sök båt eller ägare…" />
        <button class="btn-primary" @click="openCreate">+ Ny båt</button>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Modell</th>
            <th>Ägare</th>
            <th class="col-dim">L (m)</th>
            <th class="col-dim">B (m)</th>
            <th class="col-draft">Djup (m)</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in filteredBoats" :key="b.id" class="clickable-row" @click="openDetail(b)">
            <td>{{ b.model }}</td>
            <td>{{ b.ownerName }}</td>
            <td class="col-dim">{{ b.lengthM }}</td>
            <td class="col-dim">{{ b.widthM }}</td>
            <td class="col-draft">{{ b.draftM || '–' }}</td>
          </tr>
          <tr v-if="!filteredBoats.length">
            <td colspan="5" class="empty">{{ search ? 'Inga träffar' : 'Inga båtar registrerade' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Detaljvy -->
    <div v-if="detail" class="detail-overlay" @click.self="detail = null">
      <div class="detail-card">
        <div class="detail-header">
          <div>
            <h3>{{ detail.boat.model }}</h3>
            <div class="detail-owner">{{ detail.boat.ownerName }}</div>
          </div>
          <button class="close-btn" @click="detail = null">✕</button>
        </div>
        <dl class="detail-grid">
          <dt>Längd</dt><dd>{{ detail.boat.lengthM }} m</dd>
          <dt>Bredd</dt><dd>{{ detail.boat.widthM }} m</dd>
          <dt>Djupgång</dt><dd>{{ detail.boat.draftM ? detail.boat.draftM + ' m' : '–' }}</dd>
        </dl>
        <div class="assignment-section">
          <div class="section-title">Tilldelning</div>
          <div v-if="detailLoading" class="loading">Laddar…</div>
          <div v-else-if="detail.assignment" class="assignment-block assigned">
            <div class="assign-location">{{ detail.assignment.dockName }} · Plats {{ detail.assignment.slipNumber }}</div>
            <div class="assign-date">Fr.o.m. {{ formatDate(detail.assignment.assignedDate) }}</div>
          </div>
          <div v-else class="assignment-block free">Ingen aktiv plats</div>
        </div>
        <div class="detail-footer">
          <button class="btn-danger" @click="removeFromDetail">Ta bort</button>
          <button class="btn-primary" @click="openEdit(detail.boat); detail = null">Redigera</button>
        </div>
      </div>
    </div>

    <BaseModal v-if="modal" :title="editing ? 'Redigera båt' : 'Ny båt'"
               @close="modal = false" @save="save">
      <div class="form-grid">
        <label class="full">Båtmodell *<input v-model="form.model" required /></label>
        <label>Längd m *<input v-model.number="form.lengthM" type="number" step="0.1" min="0" required /></label>
        <label>Bredd m *<input v-model.number="form.widthM" type="number" step="0.1" min="0" required /></label>
        <label class="full">Djupgång m<input v-model.number="form.draftM" type="number" step="0.1" min="0" /></label>
        <label class="full">Ägare *
          <select v-model.number="form.ownerId" required>
            <option value="">Välj ägare…</option>
            <option v-for="p in persons" :key="p.id" :value="p.id">
              {{ p.firstName }} {{ p.lastName }}
            </option>
          </select>
        </label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAssignments } from '../api/assignments'
import AppLayout from '../components/AppLayout.vue'
import BaseModal from '../components/BaseModal.vue'
import { getBoats, createBoat, updateBoat, deleteBoat } from '../api/boats'
import { getPersons } from '../api/persons'

const boats = ref([])
const persons = ref([])
const search = ref('')
const modal = ref(false)
const detail = ref(null)
const detailLoading = ref(false)

const filteredBoats = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return boats.value
  return boats.value.filter(b =>
    b.model.toLowerCase().includes(q) ||
    (b.ownerName ?? '').toLowerCase().includes(q)
  )
})
const editing = ref(null)
const err = ref('')
const emptyForm = () => ({ model: '', lengthM: '', widthM: '', draftM: '', ownerId: '' })
const form = ref(emptyForm())

async function load() {
  const [b, p] = await Promise.all([getBoats(), getPersons()])
  boats.value = b.data
  persons.value = p.data
}

function formatDate(d) {
  return d ? new Date(d + 'T00:00:00').toLocaleDateString('sv-SE') : '–'
}

async function openDetail(b) {
  detail.value = { boat: b, assignment: null }
  detailLoading.value = true
  try {
    const { data: assignments } = await getAssignments()
    detail.value.assignment = assignments.find(a => a.status === 'ACTIVE' && a.boatId === b.id) ?? null
  } finally {
    detailLoading.value = false
  }
}

function openCreate() { editing.value = null; form.value = emptyForm(); err.value = ''; modal.value = true }
function openEdit(b) {
  editing.value = b
  form.value = { model: b.model, lengthM: b.lengthM, widthM: b.widthM, draftM: b.draftM, ownerId: b.ownerId }
  err.value = ''
  modal.value = true
}

async function save() {
  err.value = ''
  try {
    const payload = { ...form.value, draftM: form.value.draftM || null }
    if (editing.value) await updateBoat(editing.value.id, payload)
    else await createBoat(payload)
    modal.value = false
    await load()
  } catch (e) {
    err.value = e.response?.data?.error || 'Något gick fel'
  }
}

async function remove(b) {
  if (!confirm(`Ta bort ${b.model}?`)) return
  await deleteBoat(b.id)
  await load()
}

async function removeFromDetail() {
  const b = detail.value.boat
  if (!confirm(`Ta bort ${b.model}?`)) return
  try {
    await deleteBoat(b.id)
    detail.value = null
    await load()
  } catch (e) {
    alert(e.response?.data?.error || 'Kunde inte ta bort båten')
  }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; flex-wrap: wrap; gap: 0.75rem; }
h2 { font-size: 1.5rem; }
.header-right { display: flex; gap: 0.75rem; align-items: center; flex-wrap: wrap; }
.search-input { padding: 0.45rem 0.85rem; border: 1px solid #ddd; border-radius: 6px; font-size: 0.9rem; width: min(220px, 100%); }
.search-input:focus { outline: none; border-color: #1a3a5c; }
.table-wrap { background: white; border-radius: 10px; overflow-x: auto; box-shadow: 0 2px 8px rgba(0,0,0,0.07); -webkit-overflow-scrolling: touch; }
table { width: 100%; border-collapse: collapse; }
th { background: #f8f8f8; padding: 0.75rem 1rem; text-align: left; font-size: 0.85rem; color: #555; border-bottom: 1px solid #eee; white-space: nowrap; }
td { padding: 0.75rem 1rem; border-bottom: 1px solid #f0f0f0; font-size: 0.9rem; }
.empty { color: #999; text-align: center; padding: 2rem; }
button { padding: 0.35rem 0.8rem; border-radius: 5px; border: none; cursor: pointer; font-size: 0.85rem; background: #e8f0fe; color: #1a3a5c; }
button:hover { background: #d0e2ff; }
.btn-primary { background: #1a3a5c; color: white; padding: 0.5rem 1.1rem; border-radius: 6px; border: none; cursor: pointer; }
.btn-primary:hover { background: #234e7a; }
.btn-danger { background: #fee2e2; color: #b91c1c; padding: 0.5rem 1.1rem; border-radius: 6px; border: none; cursor: pointer; }
.btn-danger:hover { background: #fecaca; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.form-grid .full { grid-column: 1 / -1; }
label { display: flex; flex-direction: column; font-size: 0.85rem; font-weight: 600; gap: 0.3rem; }
label input, label select { padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.95rem; font-weight: 400; }
.error { color: #c0392b; font-size: 0.85rem; margin-top: 0.5rem; }
.clickable-row { cursor: pointer; transition: background 0.1s; }
.clickable-row:hover { background: #f5f8ff; }
.detail-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); z-index: 100; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.detail-card { background: white; border-radius: 12px; width: 420px; max-width: 100%; box-shadow: 0 8px 32px rgba(0,0,0,0.18); overflow: hidden; }
.detail-header { display: flex; justify-content: space-between; align-items: flex-start; padding: 1.1rem 1.4rem; border-bottom: 1px solid #eee; }
.detail-header h3 { font-size: 1.15rem; margin: 0 0 0.15rem; }
.detail-owner { font-size: 0.85rem; color: #666; }
.close-btn { background: none; border: none; font-size: 1.1rem; color: #888; cursor: pointer; padding: 0.2rem 0.4rem; flex-shrink: 0; }
.close-btn:hover { color: #333; background: #f0f0f0; border-radius: 4px; }
.detail-grid { display: grid; grid-template-columns: 120px 1fr; gap: 0.5rem 1rem; padding: 1.1rem 1.4rem; margin: 0; }
dt { font-size: 0.82rem; font-weight: 600; color: #888; }
dd { font-size: 0.9rem; color: #222; margin: 0; }
.assignment-section { border-top: 1px solid #eee; padding: 1rem 1.4rem; }
.section-title { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: #888; margin-bottom: 0.6rem; }
.loading { color: #aaa; font-size: 0.85rem; }
.assignment-block { border-radius: 8px; padding: 0.7rem 1rem; }
.assignment-block.assigned { background: #f8d7da; }
.assign-location { font-weight: 700; font-size: 0.95rem; color: #721c24; }
.assign-date { font-size: 0.8rem; color: #721c24; margin-top: 0.2rem; opacity: 0.8; }
.assignment-block.free { background: #d4edda; color: #155724; font-weight: 600; font-size: 0.88rem; }
.detail-footer { padding: 0.9rem 1.4rem; border-top: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; gap: 0.75rem; }

/* Responsive */
@media (max-width: 640px) {
  .col-draft { display: none; }
  .page-header { flex-direction: column; align-items: stretch; }
  .header-right { flex-direction: column; align-items: stretch; }
}

@media (max-width: 400px) {
  .col-dim { display: none; }
}
</style>

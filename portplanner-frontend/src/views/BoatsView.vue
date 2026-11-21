<template>
  <AppLayout>
    <div class="toolbar">
      <h2>Båtar</h2>
      <div class="right">
        <input class="input search-input" v-model="search" placeholder="Sök båt eller ägare…" />
        <button class="btn btn-primary btn-sm" @click="openCreate">Ny båt</button>
      </div>
    </div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Modell</th><th>Ägare</th>
            <th class="col-dim">L (m)</th><th class="col-dim">B (m)</th><th class="col-draft">Djup (m)</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in filteredBoats" :key="b.id" class="clickable-row" @click="openDetail(b)">
            <td>{{ b.model }}</td>
            <td>{{ b.ownerName }}</td>
            <td class="col-dim num">{{ b.lengthM }}</td>
            <td class="col-dim num">{{ b.widthM }}</td>
            <td class="col-draft num muted">{{ b.draftM || '–' }}</td>
          </tr>
          <tr v-if="!filteredBoats.length">
            <td colspan="5" class="empty">{{ search ? 'Inga träffar' : 'Inga båtar registrerade' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-if="detail" class="modal-backdrop" @click.self="detail = null">
      <div class="modal detail">
        <div class="detail-head">
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
          <button class="btn btn-danger btn-sm" @click="removeFromDetail">Ta bort</button>
          <button class="btn btn-primary btn-sm" @click="openEdit(detail.boat); detail = null">Redigera</button>
        </div>
      </div>
    </div>
    <BaseModal v-if="modal" :title="editing ? 'Redigera båt' : 'Ny båt'" @close="modal = false" @save="save">
      <div class="form-grid">
        <label class="full">Båtmodell *<input class="input" v-model="form.model" required /></label>
        <label>Längd m *<input class="input" v-model.number="form.lengthM" type="number" step="0.1" min="0" required /></label>
        <label>Bredd m *<input class="input" v-model.number="form.widthM" type="number" step="0.1" min="0" required /></label>
        <label class="full">Djupgång m<input class="input" v-model.number="form.draftM" type="number" step="0.1" min="0" /></label>
        <label class="full">Ägare *
          <select class="input" v-model.number="form.ownerId" required>
            <option value="">Välj ägare…</option>
            <option v-for="p in persons" :key="p.id" :value="p.id">{{ p.firstName }} {{ p.lastName }}</option>
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
  return boats.value.filter(b => b.model.toLowerCase().includes(q) || (b.ownerName ?? '').toLowerCase().includes(q))
})
const editing = ref(null)
const err = ref('')
const emptyForm = () => ({ model: '', lengthM: '', widthM: '', draftM: '', ownerId: '' })
const form = ref(emptyForm())

async function load() {
  const [b, p] = await Promise.all([getBoats(), getPersons()])
  boats.value = b.data; persons.value = p.data
}
function formatDate(d) { return d ? new Date(d + 'T00:00:00').toLocaleDateString('sv-SE') : '–' }
async function openDetail(b) {
  detail.value = { boat: b, assignment: null }
  detailLoading.value = true
  try {
    const { data: assignments } = await getAssignments()
    detail.value.assignment = assignments.find(a => a.status === 'ACTIVE' && a.boatId === b.id) ?? null
  } finally { detailLoading.value = false }
}
function openCreate() { editing.value = null; form.value = emptyForm(); err.value = ''; modal.value = true }
function openEdit(b) {
  editing.value = b
  form.value = { model: b.model, lengthM: b.lengthM, widthM: b.widthM, draftM: b.draftM, ownerId: b.ownerId }
  err.value = ''; modal.value = true
}
async function save() {
  err.value = ''
  try {
    const payload = { ...form.value, draftM: form.value.draftM || null }
    if (editing.value) await updateBoat(editing.value.id, payload)
    else await createBoat(payload)
    modal.value = false; await load()
  } catch (e) { err.value = e.response?.data?.error || 'Något gick fel' }
}
async function remove(b) {
  if (!confirm(`Ta bort ${b.model}?`)) return
  await deleteBoat(b.id); await load()
}
async function removeFromDetail() {
  const b = detail.value.boat
  if (!confirm(`Ta bort ${b.model}?`)) return
  try { await deleteBoat(b.id); detail.value = null; await load() }
  catch (e) { alert(e.response?.data?.error || 'Kunde inte ta bort båten') }
}
onMounted(load)
</script>

<style scoped>
.search-input { width: min(240px, 100%); }
.clickable-row { cursor: pointer; }
.empty { text-align: center; padding: 44px; display: block; }

.detail { max-width: 440px; padding: 0; }
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 22px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--paper-border);
}
.detail-head h3 { margin: 0 0 2px; }
.detail-owner { font-size: 0.9rem; color: var(--text-muted); }
.close-btn { background: none; border: 0; color: var(--text-muted); cursor: pointer; font-size: 1rem; padding: 2px 6px; }
.close-btn:hover { color: var(--ink); }

.detail-grid { display: grid; grid-template-columns: 120px 1fr; gap: 10px 22px; padding: 18px 22px; margin: 0; }
dt { font-family: var(--font-mono); font-size: 0.68rem; letter-spacing: 0.12em; text-transform: uppercase; color: var(--text-muted); }
dd { margin: 0; font-family: var(--font-mono); font-size: 0.88rem; color: var(--text); }

.assignment-section { border-top: 1px solid var(--paper-border); padding: 18px 22px; }
.loading { color: var(--text-muted); font-size: 0.9rem; }

/* Assigned is green and free is neutral, which is the guide's rule for these words. */
.assignment-block { border-radius: var(--radius-sm); padding: 10px 14px; border: 1px solid transparent; }
.assignment-block.assigned { background: var(--ok-bg); border-color: var(--ok-border); color: var(--ok-text); }
.assignment-block.free { background: var(--idle-bg); border-color: var(--idle-border); color: var(--idle-text); font-weight: 500; font-size: 0.9rem; }
.assign-location { font-weight: 600; }
.assign-date { font-size: 0.82rem; margin-top: 2px; }

.detail-footer {
  padding: 14px 22px;
  border-top: 1px solid var(--paper-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

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
.form-grid .input { font-family: var(--font); text-transform: none; letter-spacing: 0; }

@media (max-width: 640px) {
  .col-draft { display: none; }
  .toolbar, .toolbar .right { flex-direction: column; align-items: stretch; }
  .form-grid { grid-template-columns: 1fr; }
}
@media (max-width: 400px) { .col-dim { display: none; } }
</style>

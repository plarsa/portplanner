<template>
  <AppLayout>
    <div class="page-header">
      <h2>Bryggor & Båtplatser</h2>
      <div class="header-actions">
        <button class="btn-secondary" @click="openSlipCreate">+ Ny båtplats</button>
        <button class="btn-primary" @click="openDockCreate">+ Ny brygga</button>
      </div>
    </div>

    <div v-for="dock in docks" :key="dock.id" class="dock-card">
      <div class="dock-header">
        <div>
          <strong>{{ dock.name }}</strong>
          <span class="desc">{{ dock.description }}</span>
        </div>
        <div class="dock-actions">
          <span class="badge">{{ dock.slipCount }} platser</span>
          <button @click="router.push('/docks/' + dock.id + '/layout')">Visa layout</button>
          <button @click="openDockEdit(dock)">Redigera</button>
          <button class="danger" @click="removeDock(dock)">Ta bort</button>
        </div>
      </div>

      <table v-if="slipsByDock[dock.id]?.length">
        <thead>
          <tr><th>Plats</th><th>Kategori</th><th>Max L</th><th>Max B</th><th>Max Djup</th><th>Status</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="s in slipsByDock[dock.id]" :key="s.id" class="clickable-row" @click="openDetail(s, dock.name)">
            <td>{{ s.slipNumber }}</td>
            <td><span v-if="s.category" class="cat-badge">{{ s.category }}</span><span v-else class="muted">–</span></td>
            <td>{{ s.maxLengthM }} m</td>
            <td>{{ s.maxWidthM }} m</td>
            <td>{{ s.maxDraftM ? s.maxDraftM + ' m' : '–' }}</td>
            <td><span :class="['status', s.status.toLowerCase()]">{{ statusLabel(s.status) }}</span></td>
            <td class="actions" @click.stop>
              <button @click="openSlipEdit(s)">Redigera</button>
              <button class="danger" @click="removeSlip(s)">Ta bort</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="no-slips">Inga platser registrerade</p>
    </div>

    <!-- Slip detail overlay -->
    <div v-if="detail" class="detail-overlay" @click.self="detail = null">
      <div class="detail-card">
        <div class="detail-header">
          <div>
            <h3>Plats {{ detail.slip.slipNumber }}</h3>
            <div class="detail-sub">{{ detail.dockName }}</div>
          </div>
          <button class="close-btn" @click="detail = null">✕</button>
        </div>
        <dl class="detail-grid">
          <dt>Kategori</dt>
          <dd>{{ detail.slip.category || '–' }}</dd>
          <dt>Max längd</dt>
          <dd>{{ detail.slip.maxLengthM }} m</dd>
          <dt>Max bredd</dt>
          <dd>{{ detail.slip.maxWidthM }} m</dd>
          <dt>Djup</dt>
          <dd>{{ detail.slip.maxDraftM ? detail.slip.maxDraftM + ' m' : '–' }}</dd>
          <dt>Status</dt>
          <dd><span :class="['status', detail.slip.status.toLowerCase()]">{{ statusLabel(detail.slip.status) }}</span></dd>
          <template v-if="detail.slip.notes">
            <dt>Kommentar</dt><dd class="notes-text">{{ detail.slip.notes }}</dd>
          </template>
        </dl>
        <div class="boat-section">
          <div class="section-title">Tilldelad båt</div>
          <div v-if="detail.boat" class="boat-block assigned">
            <div class="boat-block-header">
              <span class="boat-name">{{ detail.boat.model }}</span>
              <span class="boat-dims">{{ detail.boat.widthM }} m × {{ detail.boat.lengthM }} m</span>
            </div>
            <div class="boat-owner">{{ detail.boat.ownerName }}</div>
            <div v-if="detail.assignment" class="boat-since">Fr.o.m. {{ formatDate(detail.assignment.assignedDate) }}</div>
          </div>
          <div v-else class="boat-block free">Ingen båt tilldelad</div>
        </div>
        <div class="detail-footer">
          <button class="btn-primary" @click="openSlipEdit(detail.slip); detail = null">Redigera plats</button>
        </div>
      </div>
    </div>

    <!-- Dock modal -->
    <BaseModal v-if="dockModal" :title="editingDock ? 'Redigera brygga' : 'Ny brygga'"
               @close="dockModal = false" @save="saveDock">
      <div class="form-col">
        <label>Namn *<input v-model="dockForm.name" required /></label>
        <label>Beskrivning<textarea v-model="dockForm.description" rows="3" /></label>
      </div>
      <p v-if="dockErr" class="error">{{ dockErr }}</p>
    </BaseModal>

    <!-- Slip modal -->
    <BaseModal v-if="slipModal" :title="editingSlip ? 'Redigera båtplats' : 'Ny båtplats'"
               @close="slipModal = false" @save="saveSlip">
      <div class="form-grid">
        <label class="full">Platsnummer *<input v-model="slipForm.slipNumber" required /></label>
        <label>Max längd m *<input v-model.number="slipForm.maxLengthM" type="number" step="0.1" min="0" required /></label>
        <label>Max bredd m *<input v-model.number="slipForm.maxWidthM" type="number" step="0.1" min="0" required /></label>
        <label class="full">Djup m<input v-model.number="slipForm.maxDraftM" type="number" step="0.1" min="0" /></label>
        <label v-if="!editingSlip" class="full">Brygga *
          <select v-model.number="slipForm.dockId" required>
            <option v-for="d in docks" :key="d.id" :value="d.id">{{ d.name }}</option>
          </select>
        </label>
        <label class="full">Tariff
          <select v-model="slipForm.category">
            <option value="">Ingen tariff</option>
            <option v-for="cat in tariffCategories" :key="cat" :value="cat">{{ cat }}</option>
          </select>
        </label>
        <label class="full">Kommentar<textarea v-model="slipForm.notes" rows="3" placeholder="Fritext…" /></label>
      </div>
      <p v-if="slipErr" class="error">{{ slipErr }}</p>
    </BaseModal>

  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import BaseModal from '../components/BaseModal.vue'
import { getDocks, createDock, updateDock, deleteDock, getDockSlips } from '../api/docks'
import { createSlip, updateSlip, deleteSlip } from '../api/slips'
import { getBoats } from '../api/boats'
import { getAssignments } from '../api/assignments'
import { getTariffs } from '../api/tariffs'

const router = useRouter()
const docks = ref([])
const slipsByDock = ref({})
const tariffCategories = ref([])
const allBoats = ref([])
const assignedBoatIds = ref(new Set())

const dockModal = ref(false)
const slipModal = ref(false)
const editingDock = ref(null)
const editingSlip = ref(null)
const dockErr = ref('')
const slipErr = ref('')
const dockForm = ref({ name: '', description: '' })
const emptySlip = () => ({ slipNumber: '', maxLengthM: '', maxWidthM: '', maxDraftM: '', dockId: '', status: 'AVAILABLE', category: '', notes: '' })
const slipForm = ref(emptySlip())

const detail = ref(null)
const assignmentBySlipId = ref({})
const boatById = ref({})

function statusLabel(s) {
  return { AVAILABLE: 'Ledig', OCCUPIED: 'Tilldelad', MAINTENANCE: 'Underhåll' }[s] ?? s
}

async function load() {
  const [{ data: docksData }, { data: tariffs }, { data: boatsData }, { data: assignments }] =
    await Promise.all([getDocks(), getTariffs(), getBoats(), getAssignments()])

  docks.value = docksData
  tariffCategories.value = [...new Set(tariffs.map(t => t.category))].sort()
  allBoats.value = boatsData
  const activeAssignments = assignments.filter(a => a.status === 'ACTIVE')
  assignedBoatIds.value = new Set(activeAssignments.map(a => a.boatId))
  assignmentBySlipId.value = Object.fromEntries(activeAssignments.map(a => [a.slipId, a]))
  boatById.value = Object.fromEntries(boatsData.map(b => [b.id, b]))

  for (const dock of docksData) {
    const { data: slips } = await getDockSlips(dock.id)
    slipsByDock.value[dock.id] = slips
  }
}

function openDockCreate() { editingDock.value = null; dockForm.value = { name: '', description: '' }; dockErr.value = ''; dockModal.value = true }
function openDockEdit(d) { editingDock.value = d; dockForm.value = { name: d.name, description: d.description }; dockErr.value = ''; dockModal.value = true }
function openSlipCreate() { editingSlip.value = null; slipForm.value = emptySlip(); slipErr.value = ''; slipModal.value = true }
function openSlipEdit(s) { editingSlip.value = s; slipForm.value = { slipNumber: s.slipNumber, maxLengthM: s.maxLengthM, maxWidthM: s.maxWidthM, maxDraftM: s.maxDraftM, dockId: s.dockId, status: s.status, category: s.category || '', notes: s.notes || '' }; slipErr.value = ''; slipModal.value = true }

function openDetail(slip, dockName) {
  const assignment = assignmentBySlipId.value[slip.id] ?? null
  const boat = assignment ? (boatById.value[assignment.boatId] ?? null) : null
  detail.value = { slip, dockName, assignment, boat }
}

function formatDate(d) {
  return d ? new Date(d + 'T00:00:00').toLocaleDateString('sv-SE') : '–'
}

async function saveDock() {
  dockErr.value = ''
  try {
    if (editingDock.value) await updateDock(editingDock.value.id, dockForm.value)
    else await createDock(dockForm.value)
    dockModal.value = false; await load()
  } catch (e) { dockErr.value = e.response?.data?.error || 'Något gick fel' }
}

async function saveSlip() {
  slipErr.value = ''
  try {
    const payload = { ...slipForm.value, maxDraftM: slipForm.value.maxDraftM || null, category: slipForm.value.category || null, notes: slipForm.value.notes || null }
    if (editingSlip.value) await updateSlip(editingSlip.value.id, payload)
    else await createSlip(payload)
    slipModal.value = false; await load()
  } catch (e) { slipErr.value = e.response?.data?.error || 'Något gick fel' }
}

async function removeDock(d) {
  if (!confirm(`Ta bort brygga ${d.name} och alla dess platser?`)) return
  try {
    await deleteDock(d.id)
    await load()
  } catch (e) {
    alert(e.response?.data?.error || 'Kunde inte ta bort bryggan')
  }
}

async function removeSlip(s) {
  if (!confirm(`Ta bort plats ${s.slipNumber}?`)) return
  try {
    await deleteSlip(s.id)
    await load()
  } catch (e) {
    alert(e.response?.data?.error || 'Kunde inte ta bort platsen')
  }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; }
h2 { font-size: 1.5rem; }
.header-actions { display: flex; gap: 0.75rem; }
.dock-card { background: white; border-radius: 10px; margin-bottom: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.07); overflow: hidden; }
.dock-header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.25rem; border-bottom: 1px solid #eee; }
.dock-header strong { font-size: 1rem; }
.desc { color: #666; font-size: 0.85rem; margin-left: 0.75rem; }
.dock-actions { display: flex; gap: 0.5rem; align-items: center; }
.badge { background: #e8f0fe; color: #1a3a5c; padding: 0.2rem 0.6rem; border-radius: 20px; font-size: 0.8rem; font-weight: 600; }
.no-slips { color: #999; padding: 1rem 1.25rem; font-size: 0.9rem; }
table { width: 100%; border-collapse: collapse; }
th { background: #f8f8f8; padding: 0.6rem 1rem; text-align: left; font-size: 0.82rem; color: #555; }
td { padding: 0.6rem 1rem; border-top: 1px solid #f0f0f0; font-size: 0.9rem; }
.actions { display: flex; gap: 0.5rem; }
.cat-badge { background: #1a3a5c; color: white; border-radius: 4px; padding: 0.1rem 0.45rem; font-size: 0.78rem; font-weight: 700; }
.muted { color: #bbb; }
.status { padding: 0.2rem 0.6rem; border-radius: 20px; font-size: 0.8rem; font-weight: 600; }
.status.available { background: #d4edda; color: #155724; }
.status.occupied { background: #f8d7da; color: #721c24; }
.status.maintenance { background: #fff3cd; color: #856404; }
button { padding: 0.3rem 0.7rem; border-radius: 5px; border: none; cursor: pointer; font-size: 0.82rem; background: #e8f0fe; color: #1a3a5c; }
button:hover { background: #d0e2ff; }
button.danger { background: #fee; color: #c0392b; }
.btn-primary { background: #1a3a5c; color: white; padding: 0.5rem 1.1rem; border-radius: 6px; }
.btn-primary:hover { background: #234e7a; }
.btn-secondary { background: #eee; color: #333; padding: 0.5rem 1.1rem; border-radius: 6px; }
.form-col { display: flex; flex-direction: column; gap: 0.75rem; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.form-grid .full { grid-column: 1 / -1; }
label { display: flex; flex-direction: column; font-size: 0.85rem; font-weight: 600; gap: 0.3rem; }
label input, label select, label textarea { padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.95rem; font-weight: 400; }
.error { color: #c0392b; font-size: 0.85rem; margin-top: 0.5rem; }

.close-btn { background: none; border: none; font-size: 1.1rem; color: #888; cursor: pointer; padding: 0.2rem 0.4rem; border-radius: 4px; }
.close-btn:hover { background: #f0f0f0; }
.clickable-row { cursor: pointer; transition: background 0.1s; }
.clickable-row:hover { background: #f5f8ff; }

/* Slip detail overlay */
.detail-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); z-index: 100; display: flex; align-items: center; justify-content: center; }
.detail-card { background: white; border-radius: 12px; width: 420px; max-width: 95vw; box-shadow: 0 8px 32px rgba(0,0,0,0.18); overflow: hidden; }
.detail-header { display: flex; justify-content: space-between; align-items: flex-start; padding: 1.1rem 1.4rem; border-bottom: 1px solid #eee; }
.detail-header h3 { font-size: 1.15rem; margin: 0 0 0.15rem; }
.detail-sub { font-size: 0.85rem; color: #666; }
.detail-grid { display: grid; grid-template-columns: 120px 1fr; gap: 0.5rem 1rem; padding: 1.1rem 1.4rem; margin: 0; }
dt { font-size: 0.82rem; font-weight: 600; color: #888; align-self: center; }
dd { font-size: 0.9rem; color: #222; margin: 0; }
.boat-section { border-top: 1px solid #eee; padding: 1rem 1.4rem; }
.section-title { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: #888; margin-bottom: 0.6rem; }
.boat-block { border-radius: 8px; padding: 0.75rem 1rem; }
.boat-block.assigned { background: #f8d7da; }
.boat-block-header { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 0.3rem; flex-wrap: wrap; }
.boat-block .boat-name { font-weight: 700; font-size: 0.95rem; color: #721c24; }
.boat-block .boat-dims { font-size: 0.78rem; color: #9a3a3a; }
.boat-block .boat-reg { font-size: 0.78rem; color: #b05050; margin-left: auto; }
.boat-owner { font-size: 0.85rem; color: #721c24; }
.boat-since { font-size: 0.78rem; color: #9a3a3a; margin-top: 0.2rem; opacity: 0.85; }
.boat-block.free { background: #d4edda; color: #155724; font-weight: 600; font-size: 0.88rem; }
.notes-text { white-space: pre-wrap; color: #555; font-style: italic; }
.detail-footer { padding: 0.9rem 1.4rem; border-top: 1px solid #eee; display: flex; justify-content: flex-end; }
</style>

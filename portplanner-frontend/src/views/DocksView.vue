<template>
  <AppLayout>
    <div class="toolbar">
      <h2>Bryggor & Båtplatser</h2>
      <div class="right">
        <button class="btn btn-secondary btn-sm" @click="openSlipCreate">Ny båtplats</button>
        <button class="btn btn-primary btn-sm" @click="openDockCreate">Ny brygga</button>
      </div>
    </div>
    <div class="sub-nav">
      <button v-for="dock in docks" :key="dock.id" :class="['navtab', { active: activeTab === dock.id }]" @click="activeTab = dock.id">{{ dock.name }}</button>
    </div>
    <template v-if="activeDock">
      <div class="dock-card">
        <div class="dock-header">
          <div class="dock-info">
            <strong>{{ activeDock.name }}</strong>
            <span v-if="activeDock.description" class="desc">{{ activeDock.description }}</span>
          </div>
          <div class="dock-actions">
            <span class="count"><span class="num">{{ activeDock.slipCount }}</span> platser</span>
            <button class="btn btn-secondary btn-sm" @click="router.push('/docks/' + activeDock.id + '/layout')">Visa layout</button>
            <button class="btn btn-secondary btn-sm" @click="openDockEdit(activeDock)">Redigera</button>
            <button class="btn btn-danger btn-sm" @click="removeDock(activeDock)">Ta bort</button>
          </div>
        </div>
        <div class="table-wrap">
          <table v-if="slipsByDock[activeDock.id]?.length">
            <thead>
              <tr><th>Plats</th><th class="col-cat">Kategori</th><th class="col-dim">Max L</th><th class="col-dim">Max B</th><th class="col-draft">Max Djup</th><th>Status</th></tr>
            </thead>
            <tbody>
              <tr v-for="s in slipsByDock[activeDock.id]" :key="s.id" class="clickable-row" @click="openDetail(s, activeDock.name)">
                <td>{{ s.slipNumber }}</td>
                <td class="col-cat"><span v-if="s.category" class="num">{{ s.category }}</span><span v-else class="muted">–</span></td>
                <td class="col-dim">{{ s.maxLengthM }} m</td>
                <td class="col-dim">{{ s.maxWidthM }} m</td>
                <td class="col-draft">{{ s.maxDraftM ? s.maxDraftM + ' m' : '–' }}</td>
                <td><span class="pill" :class="pillClass(s.status)">{{ statusLabel(s.status) }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="!slipsByDock[activeDock.id]?.length" class="no-slips">Inga platser registrerade</p>
      </div>
    </template>
    <div v-else-if="!docks.length" class="empty-state">Inga bryggor registrerade</div>

    <div v-if="detail" class="modal-backdrop" @click.self="detail = null">
      <div class="modal detail">
        <div class="detail-head">
          <div>
            <h3>Plats {{ detail.slip.slipNumber }}</h3>
            <div class="detail-sub">{{ detail.dockName }}</div>
          </div>
          <button class="close-btn" @click="detail = null">✕</button>
        </div>
        <dl class="detail-grid">
          <dt>Kategori</dt><dd>{{ detail.slip.category || '–' }}</dd>
          <dt>Max längd</dt><dd>{{ detail.slip.maxLengthM }} m</dd>
          <dt>Max bredd</dt><dd>{{ detail.slip.maxWidthM }} m</dd>
          <dt>Djup</dt><dd>{{ detail.slip.maxDraftM ? detail.slip.maxDraftM + ' m' : '–' }}</dd>
          <dt>Status</dt><dd><span class="pill" :class="pillClass(detail.slip.status)">{{ statusLabel(detail.slip.status) }}</span></dd>
          <template v-if="detail.slip.notes"><dt>Kommentar</dt><dd class="notes-text">{{ detail.slip.notes }}</dd></template>
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
          <button class="btn btn-danger btn-sm" @click="removeSlipFromDetail">Ta bort</button>
          <button class="btn btn-primary btn-sm" @click="openSlipEdit(detail.slip); detail = null">Redigera plats</button>
        </div>
      </div>
    </div>

    <BaseModal v-if="dockModal" :title="editingDock ? 'Redigera brygga' : 'Ny brygga'" @close="dockModal = false" @save="saveDock">
      <div class="form-col">
        <label>Namn *<input class="input" v-model="dockForm.name" required /></label>
        <label>Beskrivning<textarea class="input" v-model="dockForm.description" rows="3" /></label>
      </div>
      <p v-if="dockErr" class="error">{{ dockErr }}</p>
    </BaseModal>
    <BaseModal v-if="slipModal" :title="editingSlip ? 'Redigera båtplats' : 'Ny båtplats'" @close="slipModal = false" @save="saveSlip">
      <div class="form-grid">
        <label class="full">Platsnummer *<input class="input" v-model="slipForm.slipNumber" required /></label>
        <label>Max längd m *<input class="input" v-model.number="slipForm.maxLengthM" type="number" step="0.1" min="0" required /></label>
        <label>Max bredd m *<input class="input" v-model.number="slipForm.maxWidthM" type="number" step="0.1" min="0" required /></label>
        <label class="full">Djup m<input class="input" v-model.number="slipForm.maxDraftM" type="number" step="0.1" min="0" /></label>
        <label v-if="!editingSlip" class="full">Brygga *
          <select class="input" v-model.number="slipForm.dockId" required>
            <option v-for="d in docks" :key="d.id" :value="d.id">{{ d.name }}</option>
          </select>
        </label>
        <label class="full">Tariff
          <select class="input" v-model="slipForm.category">
            <option value="">Ingen tariff</option>
            <option v-for="cat in tariffCategories" :key="cat" :value="cat">{{ cat }}</option>
          </select>
        </label>
        <label class="full">Kommentar<textarea class="input" v-model="slipForm.notes" rows="3" placeholder="Fritext…" /></label>
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
const activeTab = ref(null)
const activeDock = computed(() => docks.value.find(d => d.id === activeTab.value) ?? null)
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

function statusLabel(s) { return { AVAILABLE: 'Ledig', OCCUPIED: 'Tilldelad', MAINTENANCE: 'Underhåll' }[s] ?? s }

/* The guide puts "tilldelad" under green and "ledig" under neutral grey. */
function pillClass(s) {
  return { AVAILABLE: 'pill-idle', OCCUPIED: 'pill-ok', MAINTENANCE: 'pill-warn' }[s] ?? 'pill-idle'
}

async function load() {
  const [{ data: docksData }, { data: tariffs }, { data: boatsData }, { data: assignments }] =
    await Promise.all([getDocks(), getTariffs(), getBoats(), getAssignments()])
  docks.value = docksData
  if (!activeTab.value && docksData.length) activeTab.value = docksData[0].id
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
function openSlipCreate() { editingSlip.value = null; slipForm.value = { ...emptySlip(), dockId: activeTab.value ?? '' }; slipErr.value = ''; slipModal.value = true }
function openSlipEdit(s) { editingSlip.value = s; slipForm.value = { slipNumber: s.slipNumber, maxLengthM: s.maxLengthM, maxWidthM: s.maxWidthM, maxDraftM: s.maxDraftM, dockId: s.dockId, status: s.status, category: s.category || '', notes: s.notes || '' }; slipErr.value = ''; slipModal.value = true }
function openDetail(slip, dockName) {
  const assignment = assignmentBySlipId.value[slip.id] ?? null
  const boat = assignment ? (boatById.value[assignment.boatId] ?? null) : null
  detail.value = { slip, dockName, assignment, boat }
}
function formatDate(d) { return d ? new Date(d + 'T00:00:00').toLocaleDateString('sv-SE') : '–' }

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
  try { await deleteDock(d.id); await load() }
  catch (e) { alert(e.response?.data?.error || 'Kunde inte ta bort bryggan') }
}
async function removeSlip(s) {
  if (!confirm(`Ta bort plats ${s.slipNumber}?`)) return
  try { await deleteSlip(s.id); await load() }
  catch (e) { alert(e.response?.data?.error || 'Kunde inte ta bort platsen') }
}
async function removeSlipFromDetail() {
  const s = detail.value.slip
  if (!confirm(`Ta bort plats ${s.slipNumber}?`)) return
  try { await deleteSlip(s.id); detail.value = null; await load() }
  catch (e) { alert(e.response?.data?.error || 'Kunde inte ta bort platsen') }
}
onMounted(load)
</script>

<style scoped>
/* Dock tabs follow the top row's rule: active on --sea-pale, hover on --paper-deep. */
.sub-nav { display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 22px; }
.navtab {
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
.navtab:hover { background: var(--paper-deep); color: var(--ink); }
.navtab.active { background: var(--sea-pale); color: var(--ink); }

.dock-card { margin-bottom: 22px; }
.dock-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 22px; margin-bottom: 14px; }
.dock-info { display: flex; flex-direction: column; gap: 2px; }
.dock-info strong { font-size: 1.1rem; color: var(--ink); }
.desc { font-size: 0.9rem; color: var(--text-muted); }
.dock-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.count { font-size: 0.88rem; color: var(--text-muted); }
.no-slips { color: var(--text-muted); font-size: 0.9rem; padding: 14px 0; }
.clickable-row { cursor: pointer; }
.muted { color: var(--text-muted); }

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
.detail-sub { font-size: 0.9rem; color: var(--text-muted); }
.close-btn { background: none; border: 0; color: var(--text-muted); cursor: pointer; font-size: 1rem; padding: 2px 6px; }
.close-btn:hover { color: var(--ink); }

.detail-grid { display: grid; grid-template-columns: 120px 1fr; gap: 10px 22px; padding: 18px 22px; margin: 0; }
dt { font-family: var(--font-mono); font-size: 0.68rem; letter-spacing: 0.12em; text-transform: uppercase; color: var(--text-muted); align-self: center; }
dd { margin: 0; font-size: 0.9rem; color: var(--text); }

.boat-section { border-top: 1px solid var(--paper-border); padding: 18px 22px; }
.boat-block { border-radius: var(--radius-sm); padding: 10px 14px; border: 1px solid transparent; }
.boat-block.assigned { background: var(--ok-bg); border-color: var(--ok-border); color: var(--ok-text); }
.boat-block.free { background: var(--idle-bg); border-color: var(--idle-border); color: var(--idle-text); font-weight: 500; font-size: 0.9rem; }
.boat-block-header { display: flex; align-items: center; gap: 10px; margin-bottom: 2px; flex-wrap: wrap; }
.boat-block .boat-name { font-weight: 600; }
.boat-block .boat-dims, .boat-since { font-family: var(--font-mono); font-size: 0.8rem; }
.boat-owner { font-size: 0.9rem; }
.notes-text { white-space: pre-wrap; color: var(--text-muted); }

.detail-footer {
  padding: 14px 22px;
  border-top: 1px solid var(--paper-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 14px; }
.form-grid .full { grid-column: 1 / -1; }
.form-grid label, .form-col label {
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
.form-col { display: flex; flex-direction: column; gap: 14px; }
.input { font-family: var(--font); text-transform: none; letter-spacing: 0; }

@media (max-width: 640px) {
  .col-cat, .col-draft { display: none; }
  .dock-header { flex-direction: column; align-items: stretch; }
  .form-grid { grid-template-columns: 1fr; }
}
@media (max-width: 400px) { .col-dim { display: none; } }
</style>

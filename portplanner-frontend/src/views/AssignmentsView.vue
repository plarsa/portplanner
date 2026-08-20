<template>
  <AppLayout>
    <div class="page-header">
      <h2>Tilldelningar</h2>
      <div v-if="isDockTab" class="zoom-controls">
        <button @click="zoomOut" :disabled="zoomLevel <= 0.25">−</button>
        <span class="zoom-label">{{ Math.round(zoomLevel * 100) }}%</span>
        <button @click="zoomIn" :disabled="zoomLevel >= 2.0">+</button>
        <button @click="zoomLevel = 1.0" title="Återställ zoom">↺</button>
      </div>
    </div>

    <!-- Sub-navigation -->
    <nav class="sub-nav">
      <button v-for="dock in docks" :key="dock.id"
              :class="['nav-btn', activeTab === 'dock-' + dock.id ? 'active' : '']"
              @click="activeTab = 'dock-' + dock.id">{{ dock.name }}</button>
      <div class="nav-sep"></div>
      <button :class="['nav-btn', activeTab === 'boats' ? 'active' : '']" @click="activeTab = 'boats'">
        Båtar
        <span v-if="unassignedBoats.length" class="nav-badge">{{ unassignedBoats.length }}</span>
      </button>
      <button :class="['nav-btn', activeTab === 'list' ? 'active' : '']" @click="activeTab = 'list'">Tilldelning</button>
    </nav>

    <!-- Dock layout tabs -->
    <template v-for="dock in docks" :key="dock.id">
      <div v-if="activeTab === 'dock-' + dock.id">
        <div class="legend">
          <span class="leg-item"><span class="leg-swatch available"></span> Ledig (klicka för att tilldela)</span>
          <span class="leg-item"><span class="leg-swatch occupied"></span> Tilldelad (klicka för info)</span>
          <span class="leg-item"><span class="leg-swatch maintenance"></span> Underhåll</span>
        </div>
        <div v-if="dockLayouts[dock.id]" class="grid-scroll">
          <div class="grid-wrapper"
               :style="{ width: dockLayouts[dock.id].gridWidth * 32 * zoomLevel + 'px',
                         height: dockLayouts[dock.id].gridHeight * 32 * zoomLevel + 'px' }">
            <div class="grid"
                 :style="{ transform: `scale(${zoomLevel})`, transformOrigin: '0 0',
                           gridTemplateColumns: `repeat(${dockLayouts[dock.id].gridWidth}, 32px)` }">
              <template v-for="r in dockLayouts[dock.id].gridHeight" :key="r">
                <div v-for="c in dockLayouts[dock.id].gridWidth" :key="`${r-1},${c-1}`"
                     :class="cellClass(dock.id, r-1, c-1)"
                     :title="cellTitle(dock.id, r-1, c-1)"
                     @click="onCellClick(dock.id, r-1, c-1)">
                  <span v-if="slipForCell(dock.id, r-1, c-1)" class="slip-label">
                    {{ slipForCell(dock.id, r-1, c-1).slipNumber }}
                  </span>
                </div>
              </template>
            </div>
          </div>
        </div>
        <p v-else class="no-layout">Ingen layout sparad – gå till Bryggor & Båtplatser för att rita en.</p>
      </div>
    </template>

    <!-- Båtar tab: oplacerade båtar -->
    <div v-if="activeTab === 'boats'">
      <div v-if="!unassignedBoats.length" class="empty-state">Alla registrerade båtar har en plats.</div>
      <div v-else class="tab-table-wrap">
        <table class="tab-table">
          <thead>
            <tr><th>Båt</th><th>Ägare</th><th>L (m)</th><th>B (m)</th></tr>
          </thead>
          <tbody>
            <tr v-for="b in unassignedBoats" :key="b.id" class="clickable-row" @click="openBoatModal(b)">
              <td>{{ b.model }}</td>
              <td>{{ b.ownerName }}</td>
              <td>{{ b.lengthM }}</td>
              <td>{{ b.widthM }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Tilldelning tab: lista per brygga -->
    <div v-if="activeTab === 'list'">
      <div v-if="!assignedSlipsByDock.length" class="empty-state">Inga aktiva tilldelningar.</div>
      <div v-for="group in assignedSlipsByDock" :key="group.dock.id" class="list-dock-section">
        <div class="list-dock-heading">{{ group.dock.name }}</div>
        <div class="tab-table-wrap">
          <table class="tab-table">
            <thead>
              <tr><th>Plats</th><th>Ägare</th><th>Båt</th><th>Fr.o.m.</th><th></th></tr>
            </thead>
            <tbody>
              <tr v-for="e in group.entries" :key="e.slip.id">
                <td class="slip-num">{{ e.slip.slipNumber }}</td>
                <td>{{ e.assignment?.ownerName ?? '–' }}</td>
                <td>{{ e.assignment?.boatName ?? '–' }}</td>
                <td class="date-col">{{ formatDate(e.assignment?.assignedDate) }}</td>
                <td class="actions">
                  <button class="btn-unassign-sm" @click="doUnassign(e.assignment)" v-if="e.assignment">Avsluta</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Info-popup för tilldelade/underhåll-platser -->
    <div v-if="popup" class="popup-overlay" @click.self="popup = null">
      <div class="popup-card">
        <div class="popup-header">
          <div>
            <h3>Plats {{ popup.slip.slipNumber }}</h3>
            <div class="popup-dock">{{ popup.dockName }}</div>
          </div>
          <button class="close-btn" @click="popup = null">✕</button>
        </div>
        <dl class="popup-dl">
          <dt>Kategori</dt><dd>{{ popup.slip.category || '–' }}</dd>
          <dt>Max bredd</dt><dd>{{ popup.slip.maxWidthM }} m</dd>
          <dt>Max längd</dt><dd>{{ popup.slip.maxLengthM ?? '–' }} m</dd>
          <dt>Max djup</dt><dd>{{ popup.slip.maxDraftM ? popup.slip.maxDraftM + ' m' : '–' }}</dd>
          <dt>Status</dt>
          <dd><span :class="['status-badge', popup.slip.status.toLowerCase()]">{{ statusLabel(popup.slip.status) }}</span></dd>
        </dl>
        <div v-if="popup.assignment" class="popup-assignment">
          <div class="assign-label">Tilldelad båt</div>
          <div class="assign-boat">{{ popup.assignment.boatName }}</div>
          <div class="assign-owner">{{ popup.assignment.ownerName }}</div>
          <div class="assign-date">Fr.o.m. {{ formatDate(popup.assignment.assignedDate) }}</div>
        </div>
        <div class="popup-footer">
          <button v-if="popup.assignment" class="btn-unassign"
                  @click="doUnassign(popup.assignment)">Avsluta tilldelning</button>
        </div>
      </div>
    </div>

    <!-- Tilldela-modal (plats → båt) -->
    <div v-if="assignModal" class="overlay" @click.self="assignModal = false">
      <div class="assign-card">
        <div class="assign-header">
          <div>
            <h3>Tilldela plats {{ assignSlip.slipNumber }}</h3>
            <div class="assign-dims">{{ assignDockName }} · Max {{ assignSlip.maxWidthM }} m bredd · {{ assignSlip.maxLengthM ?? '?' }} m längd</div>
          </div>
          <button class="close-btn" @click="assignModal = false">✕</button>
        </div>
        <div class="assign-scroll">
          <div v-if="!unassignedBoats.length" class="no-boats">Inga lediga båtar att tilldela.</div>
          <template v-else>
            <div class="section-label">Passar platsen</div>
            <div v-if="!fittingBoats.length" class="no-boats-section">Inga båtar passar måtten</div>
            <div v-for="b in fittingBoats" :key="b.id" class="boat-row fits" @click="doAssign(b)">
              <div class="boat-name">{{ b.model }}</div>
              <div class="boat-meta">{{ b.ownerName }} · {{ b.widthM }} m × {{ b.lengthM }} m</div>
            </div>
            <div class="section-label warn-label">Passar ej (för stor)</div>
            <div v-if="!nonFittingBoats.length" class="no-boats-section muted">Inga överstora båtar</div>
            <div v-for="b in nonFittingBoats" :key="b.id" class="boat-row no-fit" @click="doAssign(b)">
              <div class="boat-name">{{ b.model }}</div>
              <div class="boat-meta">
                {{ b.ownerName }} ·
                <span :class="b.widthM > assignSlip.maxWidthM ? 'over' : ''">{{ b.widthM }} m bredd</span>
                ·
                <span :class="assignSlip.maxLengthM && b.lengthM > assignSlip.maxLengthM ? 'over' : ''">{{ b.lengthM }} m längd</span>
              </div>
              <span class="warn-tag">⚠ Passar ej</span>
            </div>
          </template>
        </div>
        <p v-if="assignErr" class="error assign-err">{{ assignErr }}</p>
      </div>
    </div>

    <!-- Tilldela-modal (båt → plats) -->
    <div v-if="boatModal" class="overlay" @click.self="boatModal = false">
      <div class="assign-card">
        <div class="assign-header">
          <div>
            <h3>{{ selectedBoat.model }}</h3>
            <div class="assign-dims">{{ selectedBoat.ownerName }} · {{ selectedBoat.widthM }} m bredd · {{ selectedBoat.lengthM }} m längd</div>
          </div>
          <button class="close-btn" @click="boatModal = false">✕</button>
        </div>
        <div class="assign-scroll">
          <div v-if="!availableSlips.length" class="no-boats">Inga lediga platser.</div>
          <template v-else>
            <div class="section-label">Passar platsen</div>
            <div v-if="!fittingSlips.length" class="no-boats-section">Inga platser passar båtens mått</div>
            <div v-for="s in fittingSlips" :key="s.id" class="boat-row fits" @click="doAssignBoatToSlip(s)">
              <div class="boat-name">{{ s.dockName }} · Plats {{ s.slipNumber }}</div>
              <div class="boat-meta">Max {{ s.maxWidthM }} m bredd · {{ s.maxLengthM ?? '?' }} m längd</div>
            </div>
            <div class="section-label warn-label">Passar ej (för stor)</div>
            <div v-if="!nonFittingSlips.length" class="no-boats-section muted">Inga överstora platser</div>
            <div v-for="s in nonFittingSlips" :key="s.id" class="boat-row no-fit" @click="doAssignBoatToSlip(s)">
              <div class="boat-name">{{ s.dockName }} · Plats {{ s.slipNumber }}</div>
              <div class="boat-meta">
                Max <span :class="selectedBoat.widthM > s.maxWidthM ? 'over' : ''">{{ s.maxWidthM }} m bredd</span>
                · <span :class="s.maxLengthM && selectedBoat.lengthM > s.maxLengthM ? 'over' : ''">{{ s.maxLengthM ?? '?' }} m längd</span>
              </div>
              <span class="warn-tag">⚠ Passar ej</span>
            </div>
          </template>
        </div>
        <p v-if="boatAssignErr" class="error assign-err">{{ boatAssignErr }}</p>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { getDocks, getDockSlips } from '../api/docks'
import { getBoats } from '../api/boats'
import { getAssignments, createAssignment, endAssignment } from '../api/assignments'

const docks = ref([])
const slipsByDock = ref({})
const dockLayouts = ref({})
const allBoats = ref([])
const allSlips = ref([])
const assignmentsBySlipId = ref({})
const assignedBoatIds = ref(new Set())

const activeTab = ref('boats')
const popup = ref(null)
const assignModal = ref(false)
const assignSlip = ref(null)
const assignDockName = ref('')
const assignErr = ref('')
const boatModal = ref(false)
const selectedBoat = ref(null)
const boatAssignErr = ref('')
const zoomLevel = ref(1.0)

const isDockTab = computed(() => activeTab.value.startsWith('dock-'))

const unassignedBoats = computed(() =>
  allBoats.value.filter(b => !assignedBoatIds.value.has(b.id)).sort((a, b) => a.model.localeCompare(b.model))
)
const fittingBoats = computed(() => {
  if (!assignSlip.value) return []
  return unassignedBoats.value.filter(b => boatFits(b, assignSlip.value))
})
const nonFittingBoats = computed(() => {
  if (!assignSlip.value) return []
  return unassignedBoats.value.filter(b => !boatFits(b, assignSlip.value))
})
const availableSlips = computed(() =>
  allSlips.value.filter(s => s.status === 'AVAILABLE').sort((a, b) =>
    a.dockName.localeCompare(b.dockName) || String(a.slipNumber).localeCompare(String(b.slipNumber), undefined, { numeric: true })
  )
)
const fittingSlips = computed(() => {
  if (!selectedBoat.value) return []
  return availableSlips.value.filter(s => boatFits(selectedBoat.value, s))
})
const nonFittingSlips = computed(() => {
  if (!selectedBoat.value) return []
  return availableSlips.value.filter(s => !boatFits(selectedBoat.value, s))
})
const assignedSlipsByDock = computed(() =>
  docks.value.map(dock => ({
    dock,
    entries: (slipsByDock.value[dock.id] ?? [])
      .filter(s => s.status === 'OCCUPIED')
      .map(s => ({ slip: s, assignment: assignmentsBySlipId.value[s.id] ?? null }))
      .sort((a, b) => String(a.slip.slipNumber).localeCompare(String(b.slip.slipNumber), undefined, { numeric: true }))
  })).filter(g => g.entries.length > 0)
)

function boatFits(boat, slip) {
  if (boat.widthM > slip.maxWidthM) return false
  if (slip.maxLengthM && boat.lengthM > slip.maxLengthM) return false
  return true
}
function key(r, c) { return `${r},${c}` }

function slipForCell(dockId, r, c) {
  const layout = dockLayouts.value[dockId]
  if (!layout) return null
  const cell = layout.cells[key(r, c)]
  if (!cell || cell.type !== 'SLIP') return null
  return (slipsByDock.value[dockId] ?? []).find(s => s.id === cell.slipId) ?? null
}

function cellClass(dockId, r, c) {
  const layout = dockLayouts.value[dockId]
  if (!layout) return 'cell water'
  const cell = layout.cells[key(r, c)]
  if (!cell) return 'cell water'
  if (cell.type === 'LAND') return 'cell land'
  if (cell.type === 'DOCK') return 'cell dock-cell'
  if (cell.type === 'SLIP') {
    const slip = (slipsByDock.value[dockId] ?? []).find(s => s.id === cell.slipId)
    const st = slip?.status?.toLowerCase() ?? 'available'
    return `cell slip ${st} clickable`
  }
  return 'cell water'
}

function cellTitle(dockId, r, c) {
  const slip = slipForCell(dockId, r, c)
  return slip ? `Plats ${slip.slipNumber} – ${statusLabel(slip.status)}` : ''
}

function statusLabel(s) {
  return { AVAILABLE: 'Ledig', OCCUPIED: 'Tilldelad', MAINTENANCE: 'Underhåll' }[s] ?? s
}

function formatDate(d) {
  return d ? new Date(d + 'T00:00:00').toLocaleDateString('sv-SE') : '–'
}

function onCellClick(dockId, r, c) {
  const slip = slipForCell(dockId, r, c)
  if (!slip) return
  const dock = docks.value.find(d => d.id === dockId)
  if (slip.status === 'AVAILABLE') {
    assignSlip.value = slip
    assignDockName.value = dock?.name ?? ''
    assignErr.value = ''
    assignModal.value = true
  } else {
    popup.value = { slip, dockName: dock?.name ?? '', assignment: assignmentsBySlipId.value[slip.id] ?? null }
  }
}

function zoomIn()  { zoomLevel.value = Math.min(2.0,  Math.round((zoomLevel.value + 0.25) * 4) / 4) }
function zoomOut() { zoomLevel.value = Math.max(0.25, Math.round((zoomLevel.value - 0.25) * 4) / 4) }

function openBoatModal(boat) { selectedBoat.value = boat; boatAssignErr.value = ''; boatModal.value = true }

async function doAssignBoatToSlip(slip) {
  boatAssignErr.value = ''
  try {
    await createAssignment({ boatId: selectedBoat.value.id, slipId: slip.id })
    boatModal.value = false; await load()
  } catch (e) { boatAssignErr.value = e.response?.data?.error || 'Kunde inte tilldela platsen' }
}

async function doAssign(boat) {
  assignErr.value = ''
  try {
    await createAssignment({ boatId: boat.id, slipId: assignSlip.value.id })
    assignModal.value = false; await load()
  } catch (e) { assignErr.value = e.response?.data?.error || 'Kunde inte tilldela platsen' }
}

async function doUnassign(assignment) {
  if (!confirm(`Avsluta tilldelningen för ${assignment.boatName} på plats ${assignment.slipNumber}?`)) return
  popup.value = null
  try {
    await endAssignment(assignment.id); await load()
  } catch (e) { alert(e.response?.data?.error || 'Något gick fel') }
}

function parseLayout(json) {
  if (!json) return null
  try {
    const data = JSON.parse(json)
    const cells = {}
    for (const cell of data.cells ?? []) {
      cells[key(cell.row, cell.col)] = cell.type === 'SLIP'
        ? { type: 'SLIP', slipId: cell.slipId }
        : { type: cell.type }
    }
    return { cells, gridWidth: data.gridWidth ?? 30, gridHeight: data.gridHeight ?? 20 }
  } catch { return null }
}

async function load() {
  const [{ data: docksData }, { data: boatsData }, { data: assignments }] =
    await Promise.all([getDocks(), getBoats(), getAssignments()])

  docks.value = docksData
  allBoats.value = boatsData

  const active = assignments.filter(a => a.status === 'ACTIVE')
  assignedBoatIds.value = new Set(active.map(a => a.boatId))
  assignmentsBySlipId.value = Object.fromEntries(active.map(a => [a.slipId, a]))

  const layouts = {}
  const slipMap = {}
  const flatSlips = []
  for (const dock of docksData) {
    const { data: slips } = await getDockSlips(dock.id)
    slipMap[dock.id] = slips
    for (const s of slips) flatSlips.push({ ...s, dockName: dock.name })
    const parsed = parseLayout(dock.layoutJson)
    if (parsed) layouts[dock.id] = parsed
  }
  slipsByDock.value = slipMap
  allSlips.value = flatSlips
  dockLayouts.value = layouts

  if (docksData.length && activeTab.value === 'boats') {
    activeTab.value = 'dock-' + docksData[0].id
  }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
h2 { font-size: 1.5rem; }

.zoom-controls { display: flex; align-items: center; gap: 0.3rem; }
.zoom-label { font-size: 0.82rem; font-weight: 600; color: #555; min-width: 3rem; text-align: center; }

/* Sub-navigation */
.sub-nav { display: flex; align-items: center; gap: 0.25rem; margin-bottom: 1.25rem; background: white; border-radius: 10px; padding: 0.35rem 0.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.07); flex-wrap: wrap; }
.nav-btn { background: none; border: none; padding: 0.45rem 0.9rem; border-radius: 6px; font-size: 0.88rem; font-weight: 600; color: #555; cursor: pointer; display: flex; align-items: center; gap: 0.4rem; transition: background 0.1s, color 0.1s; }
.nav-btn:hover { background: #f0f4ff; color: #1a3a5c; }
.nav-btn.active { background: #1a3a5c; color: white; }
.nav-badge { background: #ef5350; color: white; font-size: 0.7rem; font-weight: 700; padding: 0.05rem 0.45rem; border-radius: 20px; }
.nav-btn.active .nav-badge { background: rgba(255,255,255,0.3); }
.nav-sep { flex: 1; }

/* Legend */
.legend { display: flex; gap: 1.5rem; margin-bottom: 1rem; flex-wrap: wrap; }
.leg-item { display: flex; align-items: center; gap: 0.4rem; font-size: 0.82rem; color: #555; }
.leg-swatch { width: 18px; height: 18px; border-radius: 3px; border: 1px solid rgba(0,0,0,0.12); }
.leg-swatch.land { background: #c8a97c; }
.leg-swatch.available { background: #66bb6a; }
.leg-swatch.occupied { background: #ef5350; }
.leg-swatch.maintenance { background: #ffa726; }

/* Grid */
.grid-scroll { overflow: auto; background: white; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); padding: 1rem; }
.grid-wrapper { position: relative; overflow: visible; }
.grid { display: grid; gap: 1px; background: rgba(0,0,0,0.06); width: fit-content; user-select: none; }
.cell { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; cursor: default; transition: filter 0.05s; }
.cell:hover { filter: brightness(0.9); }
.cell.water { background: #e3f2fd; }
.cell.land { background: #c8a97c; }
.cell.dock-cell { background: #757575; }
.cell.slip { background: #66bb6a; }
.cell.slip.occupied { background: #ef5350; }
.cell.slip.maintenance { background: #ffa726; }
.cell.slip.clickable { cursor: pointer; }
.slip-label { font-size: 0.6rem; font-weight: 700; color: white; text-shadow: 0 1px 2px rgba(0,0,0,0.4); line-height: 1; text-align: center; }
.no-layout { color: #bbb; font-size: 0.88rem; padding: 0.5rem 0; }

/* Shared table */
.tab-table-wrap { background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.07); }
.tab-table { width: 100%; border-collapse: collapse; }
.tab-table th { background: #f8f8f8; padding: 0.6rem 1rem; text-align: left; font-size: 0.82rem; color: #555; border-bottom: 1px solid #eee; }
.tab-table td { padding: 0.65rem 1rem; border-bottom: 1px solid #f0f0f0; font-size: 0.9rem; }
.tab-table td.slip-num { font-weight: 700; color: #1a3a5c; }
.tab-table td.date-col { color: #888; font-size: 0.82rem; }
.tab-table td.actions { width: 1%; white-space: nowrap; }
.empty-state { color: #bbb; font-size: 0.88rem; padding: 1rem 0; }
.clickable-row { cursor: pointer; transition: background 0.1s; }
.clickable-row:hover { background: #f5f8ff; }

/* Tilldelning list */
.list-dock-section { margin-bottom: 1.75rem; }
.list-dock-heading { font-size: 1rem; font-weight: 700; color: #1a3a5c; margin-bottom: 0.5rem; }
.btn-unassign-sm { background: #fff3e0; color: #e65100; font-weight: 600; padding: 0.25rem 0.65rem; border-radius: 5px; border: none; cursor: pointer; font-size: 0.8rem; }
.btn-unassign-sm:hover { background: #ffe0b2; }

/* Buttons */
button { padding: 0.4rem 0.85rem; border: none; border-radius: 6px; cursor: pointer; background: #e8f0fe; color: #1a3a5c; font-size: 0.85rem; }
button:hover:not(:disabled) { background: #d0e2ff; }
button:disabled { opacity: 0.45; cursor: default; }
.close-btn { background: none; border: none; font-size: 1.1rem; color: #888; cursor: pointer; padding: 0.2rem 0.4rem; border-radius: 4px; }
.close-btn:hover { background: #f0f0f0; color: #333; }

/* Info popup */
.popup-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); z-index: 100; display: flex; align-items: center; justify-content: center; }
.popup-card { background: white; border-radius: 12px; width: 360px; max-width: 95vw; box-shadow: 0 8px 32px rgba(0,0,0,0.18); overflow: hidden; }
.popup-header { display: flex; justify-content: space-between; align-items: flex-start; padding: 1rem 1.25rem; border-bottom: 1px solid #eee; }
.popup-header h3 { margin: 0 0 0.15rem; font-size: 1.1rem; }
.popup-dock { font-size: 0.82rem; color: #888; }
.popup-dl { display: grid; grid-template-columns: 110px 1fr; gap: 0.4rem 0.75rem; padding: 1rem 1.25rem; margin: 0; }
dt { font-size: 0.8rem; font-weight: 600; color: #888; align-self: center; }
dd { font-size: 0.88rem; margin: 0; }
.status-badge { padding: 0.15rem 0.5rem; border-radius: 12px; font-size: 0.78rem; font-weight: 600; }
.status-badge.available { background: #c8e6c9; color: #1b5e20; }
.status-badge.occupied { background: #ffcdd2; color: #b71c1c; }
.status-badge.maintenance { background: #ffe0b2; color: #e65100; }
.popup-assignment { padding: 0.85rem 1.25rem; border-top: 1px solid #eee; }
.assign-label { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: #888; margin-bottom: 0.4rem; }
.assign-boat { font-size: 1rem; font-weight: 700; color: #1a3a5c; margin-bottom: 0.15rem; }
.assign-owner { font-size: 0.88rem; color: #444; margin-bottom: 0.2rem; }
.assign-date { font-size: 0.8rem; color: #888; }
.popup-footer { padding: 0.75rem 1.25rem; border-top: 1px solid #eee; display: flex; justify-content: flex-end; }
.btn-unassign { background: #fff3e0; color: #e65100; font-weight: 600; padding: 0.45rem 1rem; border-radius: 6px; border: none; cursor: pointer; }
.btn-unassign:hover { background: #ffe0b2; }

/* Assign modal */
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 200; display: flex; align-items: center; justify-content: center; }
.assign-card { background: white; border-radius: 12px; width: 460px; max-width: 95vw; height: min(80vh, 700px); display: flex; flex-direction: column; box-shadow: 0 8px 32px rgba(0,0,0,0.18); overflow: hidden; }
.assign-header { flex-shrink: 0; display: flex; justify-content: space-between; align-items: flex-start; padding: 1.1rem 1.25rem; border-bottom: 1px solid #eee; }
.assign-header h3 { margin: 0 0 0.2rem; font-size: 1.05rem; }
.assign-dims { font-size: 0.8rem; color: #888; }
.assign-scroll { flex: 1; overflow-y: auto; min-height: 0; }
.section-label { font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: #555; padding: 0.6rem 1.25rem 0.3rem; background: #fafafa; border-top: 1px solid #f0f0f0; }
.warn-label { color: #9a5000; background: #fffbf0; border-top-color: #ffe082; }
.no-boats { padding: 1.25rem; color: #999; font-size: 0.9rem; }
.no-boats-section { padding: 0.4rem 1.25rem 0.6rem; color: #bbb; font-size: 0.82rem; }
.boat-row { display: flex; align-items: center; gap: 0.75rem; padding: 0.65rem 1.25rem; cursor: pointer; border-top: 1px solid #f4f4f4; }
.boat-row:hover { background: #f0f4ff; }
.boat-row.no-fit { background: #fffbf0; }
.boat-row.no-fit:hover { background: #fff3cd; }
.boat-name { font-weight: 600; font-size: 0.9rem; min-width: 120px; }
.boat-meta { font-size: 0.8rem; color: #666; flex: 1; }
.over { color: #c0392b; font-weight: 700; }
.warn-tag { font-size: 0.72rem; font-weight: 700; color: #9a5000; background: #ffe082; border-radius: 4px; padding: 0.1rem 0.4rem; white-space: nowrap; }
.error { color: #c0392b; font-size: 0.85rem; }
.assign-err { padding: 0 1.25rem 1rem; margin: 0; }
.muted { color: #bbb; }
</style>

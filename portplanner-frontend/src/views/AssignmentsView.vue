<template>
  <AppLayout>
    <div class="toolbar">
      <h2>Tilldelningar</h2>
      <div v-if="isDockTab" class="zoom-controls">
        <button class="btn btn-secondary btn-sm" @click="zoomOut" :disabled="zoomLevel <= 0.25">−</button>
        <span class="zoom-label">{{ Math.round(zoomLevel * 100) }}%</span>
        <button class="btn btn-secondary btn-sm" @click="zoomIn" :disabled="zoomLevel >= 2.0">+</button>
        <button class="btn btn-secondary btn-sm" @click="zoomLevel = 1.0" title="Återställ zoom">↺</button>
      </div>
    </div>

    <!-- Sub-navigation -->
    <nav class="sub-nav">
      <button v-for="dock in docks" :key="dock.id"
              :class="['navtab', activeTab === 'dock-' + dock.id ? 'active' : '']"
              @click="activeTab = 'dock-' + dock.id">{{ dock.name }}</button>
      <div class="nav-sep"></div>
      <button :class="['navtab', activeTab === 'boats' ? 'active' : '']" @click="activeTab = 'boats'">
        Båtar
        <span v-if="unassignedBoats.length" class="nav-badge">{{ unassignedBoats.length }}</span>
      </button>
      <button :class="['navtab', activeTab === 'list' ? 'active' : '']" @click="activeTab = 'list'">Tilldelning</button>
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

    <!-- Boats tab: boats without a slip -->
    <div v-if="activeTab === 'boats'">
      <div v-if="!unassignedBoats.length" class="empty-state">Alla registrerade båtar har en plats.</div>
      <div v-else class="table-wrap">
        <table>
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
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Plats</th><th>Ägare</th><th>Båt</th><th>Fr.o.m.</th><th></th></tr>
            </thead>
            <tbody>
              <tr v-for="e in group.entries" :key="e.slip.id">
                <td class="slip-num num">{{ e.slip.slipNumber }}</td>
                <td>{{ e.assignment?.ownerName ?? '–' }}</td>
                <td>{{ e.assignment?.boatName ?? '–' }}</td>
                <td class="date-col num muted">{{ formatDate(e.assignment?.assignedDate) }}</td>
                <td class="actions">
                  <button class="btn btn-secondary btn-sm" @click="doUnassign(e.assignment)" v-if="e.assignment">Avsluta</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Info popup for assigned and maintenance slips -->
    <div v-if="popup" class="modal-backdrop" @click.self="popup = null">
      <div class="modal popup">
        <div class="popup-head">
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
          <dd><span :class="['pill', pillClass(popup.slip.status)]">{{ statusLabel(popup.slip.status) }}</span></dd>
        </dl>
        <div v-if="popup.assignment" class="popup-assignment">
          <div class="assign-label">Tilldelad båt</div>
          <div class="assign-boat">{{ popup.assignment.boatName }}</div>
          <div class="assign-owner">{{ popup.assignment.ownerName }}</div>
          <div class="assign-date">Fr.o.m. {{ formatDate(popup.assignment.assignedDate) }}</div>
        </div>
        <div class="popup-footer">
          <button v-if="popup.assignment" class="btn btn-secondary btn-sm"
                  @click="doUnassign(popup.assignment)">Avsluta tilldelning</button>
        </div>
      </div>
    </div>

    <!-- Assign modal (slip → boat) -->
    <div v-if="assignModal" class="modal-backdrop" @click.self="assignModal = false">
      <div class="modal assign">
        <div class="assign-head">
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

    <!-- Assign modal (boat → slip) -->
    <div v-if="boatModal" class="modal-backdrop" @click.self="boatModal = false">
      <div class="modal assign">
        <div class="assign-head">
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
/* Slip status in the guide's roles: assigned is green, free is neutral. */
function pillClass(s) {
  return { AVAILABLE: 'pill-idle', OCCUPIED: 'pill-ok', MAINTENANCE: 'pill-warn' }[s] ?? 'pill-idle'
}

import { ref, computed, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { getDocks, getDockSlips, getDockLayout } from '../api/docks'
import { cellKey, parseLayout } from '../lib/dockLayout'
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

function slipForCell(dockId, r, c) {
  const layout = dockLayouts.value[dockId]
  if (!layout) return null
  const cell = layout.cells[cellKey(r, c)]
  if (!cell || cell.type !== 'SLIP') return null
  return (slipsByDock.value[dockId] ?? []).find(s => s.id === cell.slipId) ?? null
}

function cellClass(dockId, r, c) {
  const layout = dockLayouts.value[dockId]
  if (!layout) return 'cell water'
  const cell = layout.cells[cellKey(r, c)]
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
  // Slips and layout per dock, in parallel: the layout is no longer part of the list.
  const perDock = await Promise.all(docksData.map(async (dock) => ({
    dock,
    slips: (await getDockSlips(dock.id)).data,
    layout: parseLayout((await getDockLayout(dock.id)).data.layoutJson),
  })))
  for (const { dock, slips, layout } of perDock) {
    slipMap[dock.id] = slips
    for (const s of slips) flatSlips.push({ ...s, dockName: dock.name })
    if (layout) layouts[dock.id] = layout
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
.zoom-controls { display: flex; align-items: center; gap: 6px; }
.zoom-label { font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-muted); min-width: 3rem; text-align: center; }

/* Sub-navigation follows the top row's rule rather than a filled pill. */
.sub-nav { display: flex; align-items: center; gap: 4px; margin-bottom: 22px; flex-wrap: wrap; }
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
  display: flex;
  align-items: center;
  gap: 8px;
}
.navtab:hover { background: var(--paper-deep); color: var(--ink); }
.navtab.active { background: var(--sea-pale); color: var(--ink); }
.nav-badge {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  font-weight: 500;
  color: var(--warn-text);
  background: var(--warn-bg);
  border: 1px solid var(--warn-border);
  border-radius: var(--radius-sm);
  padding: 0 5px;
}
.nav-sep { flex: 1; }

/* The map. Fills come from the map palette in the style layer. */
.grid-scroll {
  overflow: auto;
  background: var(--surface);
  border: 1px solid var(--paper-border);
  border-radius: var(--radius);
  padding: 14px;
}
.grid-wrapper { position: relative; overflow: visible; }
.grid { display: grid; gap: 1px; background: var(--paper-border); width: fit-content; user-select: none; }
.cell { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; cursor: default; }
.cell:hover { filter: brightness(0.96); }
.cell.water { background: var(--map-water); }
.cell.land { background: var(--map-land); }
.cell.dock-cell { background: var(--map-dock); }
.cell.slip { background: var(--map-free-bg); }
.cell.slip .slip-label { color: var(--map-free-text); }
.cell.slip.occupied { background: var(--map-taken-bg); }
.cell.slip.occupied .slip-label { color: var(--map-taken-text); }
.cell.slip.maintenance { background: var(--map-service-bg); }
.cell.slip.maintenance .slip-label { color: var(--map-service-text); }
.cell.slip.clickable { cursor: pointer; }
.slip-label { font-family: var(--font-mono); font-size: 0.6rem; font-weight: 500; line-height: 1; text-align: center; }
.no-layout, .empty-state { color: var(--text-muted); font-size: 0.9rem; padding: 10px 0; }

.slip-num { font-weight: 500; color: var(--ink); }
.clickable-row { cursor: pointer; }
.muted { color: var(--text-muted); }

.list-dock-section { margin-bottom: 28px; }
.list-dock-heading { font-size: 1.05rem; font-weight: 600; color: var(--ink); margin-bottom: 10px; }

.close-btn { background: none; border: 0; color: var(--text-muted); cursor: pointer; font-size: 1rem; padding: 2px 6px; }
.close-btn:hover { color: var(--ink); }

/* Slip popup */
.popup { max-width: 360px; padding: 0; }
.popup-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 22px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--paper-border);
}
.popup-head h3 { margin: 0 0 2px; }
.popup-dock { font-size: 0.85rem; color: var(--text-muted); }
.popup-dl { display: grid; grid-template-columns: 110px 1fr; gap: 8px 14px; padding: 18px 22px; margin: 0; }
dt {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  align-self: center;
}
dd { font-size: 0.9rem; margin: 0; }
.popup-assignment { padding: 14px 22px; border-top: 1px solid var(--paper-border); }
.assign-label, .section-label {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-bottom: 6px;
}
.assign-boat { font-size: 1rem; font-weight: 600; color: var(--ink); }
.assign-owner { font-size: 0.9rem; color: var(--text); }
.assign-date { font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-muted); }
.popup-footer { padding: 14px 22px; border-top: 1px solid var(--paper-border); display: flex; justify-content: flex-end; }

/* Assign dialog */
.assign { max-width: 460px; height: min(80vh, 700px); display: flex; flex-direction: column; padding: 0; }
.assign-head {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 22px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--paper-border);
}
.assign-head h3 { margin: 0 0 2px; }
.assign-dims { font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-muted); }
.assign-scroll { flex: 1; overflow-y: auto; min-height: 0; }
.section-label { padding: 14px 22px 6px; background: var(--paper-deep); border-top: 1px solid var(--paper-border); margin: 0; }
.warn-label { color: var(--warn-text); background: var(--warn-bg); border-top-color: var(--warn-border); }
.no-boats, .no-boats-section { padding: 14px 22px; color: var(--text-muted); font-size: 0.9rem; }
.boat-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 22px;
  cursor: pointer;
  border-top: 1px solid var(--paper-border);
}
.boat-row:hover { background: var(--paper); }
.boat-row.no-fit { background: var(--warn-bg); }
.boat-row.no-fit:hover { background: var(--warn-border); }
.boat-name { font-weight: 500; font-size: 0.92rem; min-width: 120px; color: var(--ink); }
.boat-meta { font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-muted); flex: 1; }
.over { color: var(--bad-text); font-weight: 500; }
.warn-tag {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  font-weight: 500;
  color: var(--warn-text);
  background: var(--surface);
  border: 1px solid var(--warn-border);
  border-radius: var(--radius-sm);
  padding: 1px 6px;
  white-space: nowrap;
}
.assign-err { padding: 0 22px 18px; margin: 0; }
.actions { width: 1%; white-space: nowrap; }
</style>

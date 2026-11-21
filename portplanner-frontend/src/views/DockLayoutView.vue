<template>
  <AppLayout>
    <div class="toolbar">
      <div class="left">
        <button class="btn btn-secondary btn-sm" @click="$router.back()">← Tillbaka</button>
        <h2>{{ dock?.name ?? '…' }} – Layout</h2>
      </div>
      <div class="right">
        <button class="btn btn-secondary btn-sm" @click="toggleMode">{{ isEditing ? '👁 Visningsläge' : '✏️ Redigera layout' }}</button>
        <button v-if="isEditing" class="btn btn-primary btn-sm" @click="save" :disabled="saving">
          {{ saving ? 'Sparar…' : 'Spara' }}
        </button>
      </div>
    </div>

    <!-- Edit toolbar -->
    <div v-if="isEditing" class="paintbar">
      <span class="tl">Rita:</span>
      <button v-for="m in paintModes" :key="m.value"
              :class="['mode-btn', m.value.toLowerCase(), { active: paintMode === m.value }]"
              @click="setPaintMode(m.value)">
        {{ m.label }}
      </button>
      <span class="tl-sep"></span>
      <span class="tl">Storlek:</span>
      <label>Bredd <input class="input" type="number" v-model.number="gridWidth" min="5" max="60" /></label>
      <label>Höjd <input class="input" type="number" v-model.number="gridHeight" min="5" max="40" /></label>
      <span class="tl-sep"></span>
      <div class="zoom-controls">
        <span class="tl">Zoom:</span>
        <button class="btn btn-secondary btn-sm" @click="zoomOut" :disabled="zoomLevel <= 0.25">−</button>
        <span class="zoom-label">{{ Math.round(zoomLevel * 100) }}%</span>
        <button class="btn btn-secondary btn-sm" @click="zoomIn" :disabled="zoomLevel >= 2.0">+</button>
        <button class="btn btn-secondary btn-sm" @click="zoomLevel = 1.0" title="Återställ zoom">↺</button>
      </div>
    </div>

    <!-- Legend (view mode) -->
    <div v-else class="legend">
      <span class="leg-item"><span class="leg-swatch land"></span> Land</span>
      <span class="leg-item"><span class="leg-swatch dock"></span> Brygga</span>
      <span class="leg-item"><span class="leg-swatch available"></span> Ledig plats</span>
      <span class="leg-item"><span class="leg-swatch occupied"></span> Tilldelad plats</span>
      <span class="leg-item"><span class="leg-swatch maintenance"></span> Underhåll</span>
      <div class="zoom-controls">
        <button class="btn btn-secondary btn-sm" @click="zoomOut" :disabled="zoomLevel <= 0.25">−</button>
        <span class="zoom-label">{{ Math.round(zoomLevel * 100) }}%</span>
        <button class="btn btn-secondary btn-sm" @click="zoomIn" :disabled="zoomLevel >= 2.0">+</button>
        <button class="btn btn-secondary btn-sm" @click="zoomLevel = 1.0" title="Återställ zoom">↺</button>
      </div>
    </div>

    <div class="workspace">
      <!-- Grid -->
      <div class="grid-scroll"
           @wheel="onWheel"
           @mouseup="stopPainting"
           @mouseleave="stopPainting">
        <div class="grid-wrapper"
             :style="{ width: gridWidth * 32 * zoomLevel + 'px', height: gridHeight * 32 * zoomLevel + 'px' }">
        <div class="grid"
             :style="{ transform: `scale(${zoomLevel})`, transformOrigin: '0 0', gridTemplateColumns: `repeat(${gridWidth}, 32px)` }">
          <template v-for="r in gridHeight" :key="r">
            <div v-for="c in gridWidth" :key="`${r-1},${c-1}`"
                 :class="cellClass(r-1, c-1)"
                 :title="cellTitle(r-1, c-1)"
                 @mousedown.prevent="onMouseDown(r-1, c-1)"
                 @mouseenter="onMouseEnter(r-1, c-1)"
                 @click="onCellClick(r-1, c-1)">
              <span v-if="slipForCell(r-1, c-1)" class="slip-label">
                {{ slipForCell(r-1, c-1).slipNumber }}
              </span>
            </div>
          </template>
        </div>
        </div>
      </div>

      <!-- Slip panel (edit, PLATS mode) -->
      <div v-if="isEditing && paintMode === 'PLATS'" class="slip-panel">
        <div class="panel-title">Välj plats:</div>
        <div v-if="!slips.length" class="no-slips">Inga platser på denna brygga</div>
        <div v-for="slip in slips" :key="slip.id"
             :class="['slip-item', { placed: isPlaced(slip.id), selected: selectedSlipId === slip.id }]"
             @click="selectedSlipId = (selectedSlipId === slip.id ? null : slip.id)">
          <span class="sn">{{ slip.slipNumber }}</span>
          <span class="sd">{{ slip.maxWidthM }}m</span>
          <span v-if="isPlaced(slip.id)" class="check">✓</span>
        </div>
      </div>
    </div>

    <!-- Viewer popup -->
    <div v-if="popup" class="modal-backdrop" @click.self="popup = null">
      <div class="modal popup">
        <div class="popup-head">
          <h3>Plats {{ popup.slip.slipNumber }}</h3>
          <button class="close-btn" @click="popup = null">✕</button>
        </div>
        <dl class="popup-dl">
          <dt>Bredd</dt><dd>{{ popup.slip.maxWidthM }} m</dd>
          <dt>Längd</dt><dd>{{ popup.slip.maxLengthM ?? '–' }} m</dd>
          <dt>Max djup</dt><dd>{{ popup.slip.maxDraftM ? popup.slip.maxDraftM + ' m' : '–' }}</dd>
          <dt>Kategori</dt><dd>{{ popup.slip.category ?? '–' }}</dd>
          <dt>Status</dt>
          <dd><span :class="['pill', pillClass(popup.slip.status)]">{{ statusLabel(popup.slip.status) }}</span></dd>
        </dl>
        <div class="popup-assignment">
          <template v-if="popup.assignment">
            <div class="assign-header">Tilldelad</div>
            <div class="assign-boat">{{ popup.assignment.boatName }}</div>
            <div class="assign-owner">{{ popup.assignment.ownerName }}</div>
            <div class="assign-row date">Från: {{ formatDate(popup.assignment.assignedDate) }}</div>
          </template>
          <div v-else class="free-tag">Platsen är ledig</div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
/* Slip status in the guide's roles: assigned is green, free is neutral. */
function pillClass(s) {
  return { AVAILABLE: 'pill-idle', OCCUPIED: 'pill-ok', MAINTENANCE: 'pill-warn' }[s] ?? 'pill-idle'
}

import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import { getDock, getDockSlips, getDockLayout, saveDockLayout } from '../api/docks'
import { cellKey, parseLayout, serializeLayout } from '../lib/dockLayout'
import { getAssignments } from '../api/assignments'

const route = useRoute()
const dockId = Number(route.params.id)

const dock = ref(null)
const slips = ref([])
const isEditing = ref(false)
const saving = ref(false)

const paintModes = [
  { value: 'LAND',   label: 'Land' },
  { value: 'DOCK',   label: 'Brygga' },
  { value: 'PLATS',  label: 'Plats' },
  { value: 'RADERA', label: 'Radera' },
]
const paintMode = ref('BRYGGA')
const selectedSlipId = ref(null)
const isPainting = ref(false)

const cells = ref({})        // key: "r,c" → { type: 'LAND'|'DOCK'|'SLIP', slipId? }
const gridWidth = ref(30)
const gridHeight = ref(20)

const popup = ref(null)
const assignmentsBySlipId = ref({})

const zoomLevel = ref(1.0)

// ── Helpers ──────────────────────────────────────────────

function isPlaced(slipId) {
  return Object.values(cells.value).some(v => v.type === 'SLIP' && v.slipId === slipId)
}

function slipForCell(r, c) {
  const cell = cells.value[cellKey(r, c)]
  if (!cell || cell.type !== 'SLIP') return null
  return slips.value.find(s => s.id === cell.slipId) ?? null
}

function cellClass(r, c) {
  const cell = cells.value[cellKey(r, c)]
  if (!cell) return 'cell water'
  if (cell.type === 'LAND') return 'cell land'
  if (cell.type === 'DOCK') return 'cell dock-cell'
  if (cell.type === 'SLIP') {
    const slip = slips.value.find(s => s.id === cell.slipId)
    const st = slip?.status?.toLowerCase() ?? 'available'
    return `cell slip ${st}${!isEditing.value ? ' clickable' : ''}`
  }
  return 'cell water'
}

function cellTitle(r, c) {
  const slip = slipForCell(r, c)
  return slip ? `Plats ${slip.slipNumber}` : ''
}

function statusLabel(s) {
  return { AVAILABLE: 'Ledig', OCCUPIED: 'Tilldelad', MAINTENANCE: 'Underhåll' }[s] ?? s
}

function formatDate(d) {
  return d ? new Date(d + 'T00:00:00').toLocaleDateString('sv-SE') : '–'
}

// ── Painting ──────────────────────────────────────────────

function setPaintMode(m) {
  paintMode.value = m
  if (m !== 'PLATS') selectedSlipId.value = null
}

function paintCell(r, c) {
  const k = cellKey(r, c)
  if (paintMode.value === 'RADERA') {
    const current = cells.value[k]
    // For reactive deletion we need to reassign
    const next = { ...cells.value }
    delete next[k]
    cells.value = next
    return
  }
  if (paintMode.value === 'PLATS') {
    if (!selectedSlipId.value) return
    // Remove old placement of this slip
    const next = Object.fromEntries(
      Object.entries(cells.value).filter(([, v]) => !(v.type === 'SLIP' && v.slipId === selectedSlipId.value))
    )
    next[k] = { type: 'SLIP', slipId: selectedSlipId.value }
    cells.value = next
    return
  }
  cells.value = { ...cells.value, [k]: { type: paintMode.value } }
}

function onMouseDown(r, c) {
  if (!isEditing.value) return
  isPainting.value = true
  paintCell(r, c)
}

function onMouseEnter(r, c) {
  if (!isEditing.value || !isPainting.value) return
  if (paintMode.value === 'PLATS') return
  paintCell(r, c)
}

function stopPainting() { isPainting.value = false }

// ── Zoom ──────────────────────────────────────────────────

function zoomIn()  { zoomLevel.value = Math.min(2.0,  Math.round((zoomLevel.value + 0.25) * 4) / 4) }
function zoomOut() { zoomLevel.value = Math.max(0.25, Math.round((zoomLevel.value - 0.25) * 4) / 4) }

function onWheel(e) {
  if (!e.ctrlKey && !e.metaKey) return
  e.preventDefault()
  e.deltaY > 0 ? zoomOut() : zoomIn()
}

// ── Viewer click ──────────────────────────────────────────

function onCellClick(r, c) {
  if (isEditing.value) return
  const slip = slipForCell(r, c)
  if (!slip) return
  popup.value = { slip, assignment: assignmentsBySlipId.value[slip.id] ?? null }
}

// ── Mode toggle ───────────────────────────────────────────

function toggleMode() {
  isEditing.value = !isEditing.value
  popup.value = null
}

// ── Load / Save ───────────────────────────────────────────

function applyLayout(json) {
  const layout = parseLayout(json)
  if (!layout) return
  gridWidth.value = layout.gridWidth
  gridHeight.value = layout.gridHeight
  cells.value = layout.cells
}

async function save() {
  saving.value = true
  try {
    const layoutJson = serializeLayout({
      cells: cells.value, gridWidth: gridWidth.value, gridHeight: gridHeight.value,
    })
    await saveDockLayout(dockId, layoutJson)
    isEditing.value = false
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const [{ data: d }, { data: s }, { data: allAssignments }, { data: layout }] = await Promise.all([
    getDock(dockId), getDockSlips(dockId), getAssignments(), getDockLayout(dockId)
  ])
  dock.value = d
  slips.value = s
  const bySlip = {}
  for (const a of allAssignments) {
    if (a.status === 'ACTIVE') bySlip[a.slipId] = a
  }
  assignmentsBySlipId.value = bySlip
  applyLayout(layout.layoutJson)
})
</script>

<style scoped>
.toolbar .left { display: flex; align-items: center; gap: 14px; }
h2 { font-size: 1.45rem; margin: 0; }

/* The paint toolbar. Each mode wears the fill it paints, so the control shows
   its own result instead of an invented colour. */
.paintbar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--surface);
  border: 1px solid var(--paper-border);
  border-radius: var(--radius);
  padding: 10px 14px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.tl {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.tl-sep { flex: 1; }
.mode-btn {
  border: 1px solid var(--paper-border);
  border-radius: var(--radius-sm);
  padding: 6px 12px;
  font-family: var(--font);
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  background: var(--surface);
  color: var(--text-muted);
}
.mode-btn.land { background: var(--map-land); color: var(--idle-text); }
.mode-btn.dock { background: var(--map-dock); color: #fff; border-color: var(--map-dock); }
.mode-btn.plats { background: var(--sea-pale); color: var(--sea-hover); border-color: var(--info-border); }
.mode-btn.radera { background: var(--bad-bg); color: var(--bad-text); border-color: var(--bad-border); }
.mode-btn.active { outline: 2px solid var(--sea); outline-offset: 1px; }
.paintbar label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.paintbar label .input { width: 64px; padding: 5px 8px; font-size: 0.85rem; }

.workspace { display: flex; gap: 14px; align-items: flex-start; }

.grid-scroll {
  overflow: auto;
  background: var(--surface);
  border: 1px solid var(--paper-border);
  border-radius: var(--radius);
  padding: 14px;
  flex: 1;
  min-width: 0;
}
.grid-wrapper { position: relative; overflow: visible; }
.grid { display: grid; gap: 1px; background: var(--paper-border); width: fit-content; user-select: none; }

.zoom-controls { display: flex; align-items: center; gap: 6px; margin-left: auto; }
.zoom-label { font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-muted); min-width: 3rem; text-align: center; }

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

.slip-panel {
  background: var(--surface);
  border: 1px solid var(--paper-border);
  border-radius: var(--radius);
  padding: 14px;
  min-width: 170px;
  max-width: 190px;
}
.panel-title {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-bottom: 10px;
}
.no-slips { color: var(--text-muted); font-size: 0.85rem; }
.slip-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.88rem;
  margin-bottom: 2px;
}
.slip-item:hover { background: var(--paper-deep); }
.slip-item.selected { background: var(--sea-pale); color: var(--ink); }
.sn { font-family: var(--font-mono); font-weight: 500; min-width: 2.2rem; }
.sd { font-family: var(--font-mono); font-size: 0.78rem; color: var(--text-muted); }
.slip-item.placed .sn { color: var(--ok-text); }
.check { margin-left: auto; color: var(--ok-text); }

.popup { max-width: 340px; padding: 0; }
.popup-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 22px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--paper-border);
}
.popup-head h3 { margin: 0; }
.close-btn { background: none; border: 0; color: var(--text-muted); cursor: pointer; font-size: 1rem; padding: 2px 6px; }
.close-btn:hover { color: var(--ink); }
.popup-dl { display: grid; grid-template-columns: 110px 1fr; gap: 8px 14px; padding: 18px 22px; margin: 0; }
dt { font-family: var(--font-mono); font-size: 0.68rem; letter-spacing: 0.12em; text-transform: uppercase; color: var(--text-muted); }
dd { font-size: 0.9rem; margin: 0; }
.popup-assignment { padding: 14px 22px 18px; border-top: 1px solid var(--paper-border); }
.assign-header {
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
.assign-row { font-size: 0.9rem; }
.assign-row.date { font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-muted); }
.free-tag {
  background: var(--idle-bg);
  border: 1px solid var(--idle-border);
  color: var(--idle-text);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  font-size: 0.88rem;
  font-weight: 500;
}
@media (max-width: 980px) {
  .workspace { flex-direction: column; }
  .slip-panel { max-width: none; width: 100%; }
}
</style>

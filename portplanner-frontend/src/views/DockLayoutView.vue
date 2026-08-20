<template>
  <AppLayout>
    <div class="page-header">
      <div class="left">
        <button class="back-btn" @click="$router.back()">← Tillbaka</button>
        <h2>{{ dock?.name ?? '…' }} – Layout</h2>
      </div>
      <div class="right">
        <button @click="toggleMode">{{ isEditing ? '👁 Visningsläge' : '✏️ Redigera layout' }}</button>
        <button v-if="isEditing" class="btn-primary" @click="save" :disabled="saving">
          {{ saving ? 'Sparar…' : 'Spara' }}
        </button>
      </div>
    </div>

    <!-- Edit toolbar -->
    <div v-if="isEditing" class="toolbar">
      <span class="tl">Rita:</span>
      <button v-for="m in paintModes" :key="m.value"
              :class="['mode-btn', m.value.toLowerCase(), { active: paintMode === m.value }]"
              @click="setPaintMode(m.value)">
        {{ m.label }}
      </button>
      <span class="tl-sep"></span>
      <span class="tl">Storlek:</span>
      <label>Bredd <input type="number" v-model.number="gridWidth" min="5" max="60" /></label>
      <label>Höjd <input type="number" v-model.number="gridHeight" min="5" max="40" /></label>
      <span class="tl-sep"></span>
      <div class="zoom-controls">
        <span class="tl">Zoom:</span>
        <button @click="zoomOut" :disabled="zoomLevel <= 0.25">−</button>
        <span class="zoom-label">{{ Math.round(zoomLevel * 100) }}%</span>
        <button @click="zoomIn" :disabled="zoomLevel >= 2.0">+</button>
        <button @click="zoomLevel = 1.0" title="Återställ zoom">↺</button>
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
        <button @click="zoomOut" :disabled="zoomLevel <= 0.25">−</button>
        <span class="zoom-label">{{ Math.round(zoomLevel * 100) }}%</span>
        <button @click="zoomIn" :disabled="zoomLevel >= 2.0">+</button>
        <button @click="zoomLevel = 1.0" title="Återställ zoom">↺</button>
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
    <div v-if="popup" class="popup-overlay" @click.self="popup = null">
      <div class="popup-card">
        <div class="popup-header">
          <h3>Plats {{ popup.slip.slipNumber }}</h3>
          <button class="close-btn" @click="popup = null">✕</button>
        </div>
        <dl class="popup-dl">
          <dt>Bredd</dt><dd>{{ popup.slip.maxWidthM }} m</dd>
          <dt>Längd</dt><dd>{{ popup.slip.maxLengthM ?? '–' }} m</dd>
          <dt>Max djup</dt><dd>{{ popup.slip.maxDraftM ? popup.slip.maxDraftM + ' m' : '–' }}</dd>
          <dt>Kategori</dt><dd>{{ popup.slip.category ?? '–' }}</dd>
          <dt>Status</dt>
          <dd><span :class="['status-badge', popup.slip.status.toLowerCase()]">{{ statusLabel(popup.slip.status) }}</span></dd>
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
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import { getDock, getDockSlips, updateDock } from '../api/docks'
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

function key(r, c) { return `${r},${c}` }

function isPlaced(slipId) {
  return Object.values(cells.value).some(v => v.type === 'SLIP' && v.slipId === slipId)
}

function slipForCell(r, c) {
  const cell = cells.value[key(r, c)]
  if (!cell || cell.type !== 'SLIP') return null
  return slips.value.find(s => s.id === cell.slipId) ?? null
}

function cellClass(r, c) {
  const cell = cells.value[key(r, c)]
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
  const k = key(r, c)
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

function parseLayout(json) {
  if (!json) return
  try {
    const data = JSON.parse(json)
    if (data.gridWidth) gridWidth.value = data.gridWidth
    if (data.gridHeight) gridHeight.value = data.gridHeight
    const next = {}
    for (const cell of data.cells ?? []) {
      next[key(cell.row, cell.col)] = cell.type === 'SLIP'
        ? { type: 'SLIP', slipId: cell.slipId }
        : { type: cell.type }
    }
    cells.value = next
  } catch (e) { /* corrupt json – start fresh */ }
}

async function save() {
  saving.value = true
  try {
    const layoutJson = JSON.stringify({
      gridWidth: gridWidth.value,
      gridHeight: gridHeight.value,
      cells: Object.entries(cells.value).map(([k, v]) => {
        const [row, col] = k.split(',').map(Number)
        return v.type === 'SLIP'
          ? { row, col, type: 'SLIP', slipId: v.slipId }
          : { row, col, type: v.type }
      })
    })
    await updateDock(dockId, { name: dock.value.name, description: dock.value.description, layoutJson })
    dock.value.layoutJson = layoutJson
    isEditing.value = false
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const [{ data: d }, { data: s }, { data: allAssignments }] = await Promise.all([
    getDock(dockId), getDockSlips(dockId), getAssignments()
  ])
  dock.value = d
  slips.value = s
  const bySlip = {}
  for (const a of allAssignments) {
    if (a.status === 'ACTIVE') bySlip[a.slipId] = a
  }
  assignmentsBySlipId.value = bySlip
  parseLayout(d.layoutJson)
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
.left { display: flex; align-items: center; gap: 1rem; }
.right { display: flex; gap: 0.75rem; }
h2 { font-size: 1.4rem; margin: 0; }
.back-btn { background: #eee; border: none; border-radius: 6px; padding: 0.4rem 0.8rem; cursor: pointer; font-size: 0.85rem; }
.back-btn:hover { background: #ddd; }
.btn-primary { background: #1a3a5c; color: white; border: none; border-radius: 6px; padding: 0.45rem 1rem; cursor: pointer; }
.btn-primary:hover:not(:disabled) { background: #234e7a; }
.btn-primary:disabled { opacity: 0.6; cursor: default; }
button { padding: 0.4rem 0.85rem; border: none; border-radius: 6px; cursor: pointer; background: #e8f0fe; color: #1a3a5c; font-size: 0.85rem; }
button:hover { background: #d0e2ff; }

/* Toolbar */
.toolbar { display: flex; align-items: center; gap: 0.6rem; background: white; border-radius: 8px; padding: 0.6rem 1rem; margin-bottom: 0.75rem; box-shadow: 0 1px 4px rgba(0,0,0,0.08); flex-wrap: wrap; }
.tl { font-size: 0.82rem; font-weight: 600; color: #555; }
.tl-sep { flex: 1; }
.mode-btn { font-weight: 600; }
.mode-btn.land { background: #f0e0c8; color: #7a5230; }
.mode-btn.land:hover, .mode-btn.land.active { background: #c8a97c; color: white; }
.mode-btn.dock { background: #e0e0e0; color: #333; }
.mode-btn.dock:hover, .mode-btn.dock.active { background: #757575; color: white; }
.mode-btn.plats { background: #bbdefb; color: #0d47a1; }
.mode-btn.plats:hover, .mode-btn.plats.active { background: #1565c0; color: white; }
.mode-btn.radera { background: #ffcdd2; color: #b71c1c; }
.mode-btn.radera:hover, .mode-btn.radera.active { background: #e53935; color: white; }
label { display: flex; align-items: center; gap: 0.3rem; font-size: 0.82rem; font-weight: 600; color: #555; }
label input { width: 52px; padding: 0.25rem 0.4rem; border: 1px solid #ddd; border-radius: 4px; font-size: 0.85rem; }

/* Legend */
.legend { display: flex; gap: 1.25rem; margin-bottom: 0.75rem; flex-wrap: wrap; }
.leg-item { display: flex; align-items: center; gap: 0.4rem; font-size: 0.82rem; color: #555; }
.leg-swatch { width: 18px; height: 18px; border-radius: 3px; border: 1px solid rgba(0,0,0,0.12); }
.leg-swatch.land { background: #c8a97c; }
.leg-swatch.dock { background: #757575; }
.leg-swatch.available { background: #66bb6a; }
.leg-swatch.occupied { background: #ef5350; }
.leg-swatch.maintenance { background: #ffa726; }

/* Workspace */
.workspace { display: flex; gap: 1rem; align-items: flex-start; }

/* Grid */
.grid-scroll { overflow: auto; background: white; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); padding: 1rem; flex: 1; min-width: 0; }
.grid-wrapper { position: relative; overflow: visible; }
.grid { display: grid; gap: 1px; background: rgba(0,0,0,0.06); width: fit-content; user-select: none; }

/* Zoom controls */
.zoom-controls { display: flex; align-items: center; gap: 0.3rem; margin-left: auto; }
.zoom-label { font-size: 0.82rem; font-weight: 600; color: #555; min-width: 3rem; text-align: center; }

.cell { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; cursor: default; transition: filter 0.05s; }
.cell:hover { filter: brightness(0.9); }
.cell.water { background: #e3f2fd; }
.cell.land { background: #c8a97c; }
.cell.dock-cell { background: #757575; }
.cell.slip { background: #66bb6a; cursor: default; }
.cell.slip.occupied { background: #ef5350; }
.cell.slip.maintenance { background: #ffa726; }
.cell.slip.clickable { cursor: pointer; }
.slip-label { font-size: 0.6rem; font-weight: 700; color: white; text-shadow: 0 1px 2px rgba(0,0,0,0.4); line-height: 1; text-align: center; }

/* Slip panel */
.slip-panel { background: white; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); padding: 0.75rem; min-width: 160px; max-width: 180px; }
.panel-title { font-size: 0.82rem; font-weight: 700; color: #555; margin-bottom: 0.5rem; }
.no-slips { color: #bbb; font-size: 0.82rem; }
.slip-item { display: flex; align-items: center; gap: 0.4rem; padding: 0.4rem 0.5rem; border-radius: 5px; cursor: pointer; font-size: 0.85rem; margin-bottom: 2px; }
.slip-item:hover { background: #f0f4ff; }
.slip-item.selected { background: #1a3a5c; color: white; }
.slip-item.placed .sn { color: #4caf50; font-weight: 700; }
.slip-item.selected.placed .sn { color: #a5d6a7; }
.sn { font-weight: 600; min-width: 2rem; }
.sd { color: #888; font-size: 0.78rem; }
.slip-item.selected .sd { color: rgba(255,255,255,0.7); }
.check { margin-left: auto; color: #4caf50; font-weight: 700; }
.slip-item.selected .check { color: #a5d6a7; }

/* Popup */
.popup-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); z-index: 100; display: flex; align-items: center; justify-content: center; }
.popup-card { background: white; border-radius: 12px; width: 340px; max-width: 95vw; box-shadow: 0 8px 32px rgba(0,0,0,0.18); overflow: hidden; }
.popup-header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.25rem; border-bottom: 1px solid #eee; }
.popup-header h3 { margin: 0; font-size: 1.1rem; }
.close-btn { background: none; border: none; font-size: 1.1rem; color: #888; cursor: pointer; padding: 0.2rem 0.4rem; border-radius: 4px; }
.close-btn:hover { background: #f0f0f0; color: #333; }
.popup-dl { display: grid; grid-template-columns: 110px 1fr; gap: 0.4rem 0.75rem; padding: 1rem 1.25rem; margin: 0; }
dt { font-size: 0.8rem; font-weight: 600; color: #888; }
dd { font-size: 0.88rem; margin: 0; }
.status-badge { padding: 0.15rem 0.5rem; border-radius: 12px; font-size: 0.78rem; font-weight: 600; }
.status-badge.available { background: #c8e6c9; color: #1b5e20; }
.status-badge.occupied { background: #ffcdd2; color: #b71c1c; }
.status-badge.maintenance { background: #ffe0b2; color: #e65100; }
.popup-assignment { padding: 0.75rem 1.25rem 1.1rem; border-top: 1px solid #eee; }
.assign-header { font-size: 0.78rem; font-weight: 600; color: #888; margin-bottom: 0.35rem; text-transform: uppercase; letter-spacing: 0.04em; }
.assign-boat { font-size: 1rem; font-weight: 700; color: #1a3a5c; margin-bottom: 0.2rem; }
.assign-owner { font-size: 0.88rem; color: #444; margin-bottom: 0.25rem; }
.assign-row { font-size: 0.9rem; margin-bottom: 0.15rem; }
.assign-row.date { font-size: 0.8rem; color: #888; }
.free-tag { background: #e8f5e9; color: #2e7d32; border-radius: 6px; padding: 0.5rem 0.75rem; font-size: 0.85rem; font-weight: 600; }
</style>

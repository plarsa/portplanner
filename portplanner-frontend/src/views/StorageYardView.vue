<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <router-link to="/winter" class="back-link">← Vinterupptagning</router-link>
        <h2>Gårdsplan — {{ season?.name }}</h2>
      </div>
      <div class="header-actions">
        <button class="btn-secondary" @click="openCreateYard">+ Ny gårdsplan</button>
      </div>
    </div>

    <div v-if="!yards.length" class="empty-state">Inga gårdsplaner för denna säsong.</div>

    <!-- Yard tabs -->
    <div v-if="yards.length" class="yard-tabs">
      <button v-for="y in yards" :key="y.id"
              :class="['yard-tab', activeYardId === y.id ? 'active' : '']"
              @click="selectYard(y.id)">
        {{ y.name }}
      </button>
    </div>

    <div v-if="activeYard" class="yard-panel">
      <!-- Toolbar -->
      <div class="yard-toolbar">
        <button class="btn-sm" @click="suggestPlacements" :disabled="busy">Föreslå placering</button>
        <button class="btn-sm" @click="validateYardPlan">Validera</button>
        <button class="btn-secondary" @click="openCalibration">Kalibrera</button>
        <button class="btn-secondary" @click="openEditYard(activeYard)">Redigera</button>
        <button class="btn-danger-sm" @click="removeYard(activeYard)">Ta bort</button>
      </div>

      <!-- Validation issues -->
      <div v-if="validationIssues.length" class="issues-panel">
        <div v-for="issue in validationIssues" :key="issue.placementId" class="issue-row">
          ⚠ {{ issue.boatModel }}: {{ issue.message }}
        </div>
      </div>

      <!-- Canvas -->
      <div class="canvas-container" @dragover.prevent @drop="onDrop">
        <svg :width="canvasW" :height="canvasH" class="yard-svg" @click="deselectPlacement">
          <!-- Background image -->
          <image v-if="activeYard.backgroundImageUrl"
                 :href="activeYard.backgroundImageUrl"
                 x="0" y="0" :width="canvasW" :height="canvasH"
                 preserveAspectRatio="xMidYMid meet" />

          <!-- Boundary polygon -->
          <polygon v-if="boundaryPoints.length >= 3"
                   :points="boundaryPoints.map(p => `${mToP(p.xMeters)},${mToP(p.yMeters)}`).join(' ')"
                   fill="rgba(26,58,92,0.05)" stroke="#1a3a5c" stroke-width="1.5" stroke-dasharray="6 4" />

          <!-- Placements -->
          <g v-for="p in placements" :key="p.id"
             :transform="`translate(${mToP(p.xMeters)},${mToP(p.yMeters)})`"
             class="placement-group"
             :class="{ selected: selectedId === p.id, launched: p.status === 'LAUNCHED' }"
             @click.stop="selectPlacement(p)"
             @mousedown="startDrag($event, p)">
            <!-- Boat silhouette -->
            <polygon :points="boatSilhouette(p)" :fill="placementColor(p)" stroke="#1a3a5c" stroke-width="1" />
            <!-- Label -->
            <text :x="mToP(p.widthMeters) / 2" :y="mToP(p.lengthMeters) / 2 + 4"
                  text-anchor="middle" font-size="10" fill="#1a3a5c" font-weight="600"
                  style="pointer-events:none; user-select:none">
              {{ p.boatModel }}
            </text>
          </g>
        </svg>
      </div>

      <!-- Side panel: selected placement details -->
      <div v-if="selectedPlacement" class="side-panel">
        <div class="side-title">{{ selectedPlacement.boatModel }}</div>
        <div class="side-owner">{{ selectedPlacement.personName }}</div>
        <dl class="side-grid">
          <dt>Bredd</dt><dd>{{ selectedPlacement.widthMeters }} m</dd>
          <dt>Längd</dt><dd>{{ selectedPlacement.lengthMeters }} m</dd>
          <dt>Position X</dt><dd>{{ selectedPlacement.xMeters }} m</dd>
          <dt>Position Y</dt><dd>{{ selectedPlacement.yMeters }} m</dd>
          <dt>Status</dt><dd><span :class="['status-chip', selectedPlacement.status.toLowerCase()]">{{ selectedPlacement.status }}</span></dd>
          <dt>Grupp</dt>
          <dd>
            <select :value="selectedPlacement.packingGroupId ?? ''" @change="assignGroup($event.target.value)">
              <option value="">Ingen grupp</option>
              <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
            </select>
          </dd>
          <dt v-if="selectedPlacement.packingGroupId">Ordning</dt>
          <dd v-if="selectedPlacement.packingGroupId">
            <input type="number" :value="selectedPlacement.orderInGroup ?? ''" min="1"
                   @change="setGroupOrder(+$event.target.value)" style="width:60px" />
          </dd>
        </dl>
        <div class="side-actions">
          <button class="btn-sm" @click="markPlaced">Satt som PLACED</button>
          <button v-if="selectedPlacement.status !== 'LAUNCHED'" class="btn-sm" @click="launchPlacement">Sjösatt</button>
        </div>
      </div>

      <!-- Packing groups panel -->
      <div class="groups-section">
        <div class="groups-header">
          <span class="section-label">Packningsgrupper</span>
          <button class="btn-sm" @click="createGroup">+ Ny grupp</button>
        </div>
        <div v-for="g in groups" :key="g.id" class="group-row">
          <span>{{ g.name }}</span>
          <span v-if="g.retrievalNote" class="group-note">{{ g.retrievalNote }}</span>
          <button class="btn-danger-sm" @click="removeGroup(g)">Ta bort</button>
        </div>
        <div v-if="!groups.length" class="slots-empty">Inga grupper</div>
      </div>
    </div>

    <!-- Yard modal -->
    <BaseModal v-if="yardModal" :title="editingYard ? 'Redigera gårdsplan' : 'Ny gårdsplan'"
               @close="yardModal = false" @save="saveYard">
      <div class="form-grid">
        <label class="full">Namn *<input v-model="yardForm.name" /></label>
        <label class="full">Bakgrundsbild URL<input v-model="yardForm.backgroundImageUrl" placeholder="https://..." /></label>
        <label class="full">Marginal mellan båtar (m)<input v-model.number="yardForm.laneMarginM" type="number" step="0.1" min="0" /></label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>

    <!-- Calibration modal -->
    <BaseModal v-if="calibModal" title="Kalibrering" @close="calibModal = false" @save="saveCalibration">
      <p class="calib-help">Ange koordinatsystemets ursprung och skala. Pixelkoordinater läses från bakgrundsbild; meter-koordinater används för placering.</p>
      <div class="form-grid">
        <label>Ursprung pixel X<input v-model.number="calibForm.originPixelX" type="number" /></label>
        <label>Ursprung pixel Y<input v-model.number="calibForm.originPixelY" type="number" /></label>
        <label class="full">Pixlar per meter<input v-model.number="calibForm.pixelsPerMeter" type="number" step="0.1" /></label>
      </div>
      <div class="boundary-section">
        <div class="section-label">Gränspolygon (meter-koordinater)</div>
        <div v-for="(pt, i) in calibForm.boundary" :key="i" class="boundary-row">
          <label>X<input v-model.number="pt.xMeters" type="number" step="0.5" style="width:80px" /></label>
          <label>Y<input v-model.number="pt.yMeters" type="number" step="0.5" style="width:80px" /></label>
          <button class="btn-danger-sm" @click="calibForm.boundary.splice(i, 1)">✕</button>
        </div>
        <button class="btn-sm" @click="calibForm.boundary.push({ xMeters: 0, yMeters: 0 })">+ Punkt</button>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import BaseModal from '../components/BaseModal.vue'
import {
  getStorageYards, createStorageYard, updateStorageYard, deleteStorageYard, calibrateStorageYard,
  suggestPlacements as apiSuggest, validateYard as apiValidate,
  getStoragePlacements, updateStoragePlacement, launchStoragePlacement,
  getPackingGroups, createPackingGroup, deletePackingGroup
} from '../api/winterHaulOut'
import { getWinterSeason } from '../api/winterHaulOut'

const route = useRoute()
const seasonId = computed(() => +route.params.seasonId)
const season = ref(null)
const yards = ref([])
const activeYardId = ref(null)
const activeYard = computed(() => yards.value.find(y => y.id === activeYardId.value) ?? null)
const placements = ref([])
const groups = ref([])
const validationIssues = ref([])
const selectedId = ref(null)
const selectedPlacement = computed(() => placements.value.find(p => p.id === selectedId.value) ?? null)
const busy = ref(false)
const err = ref('')

// Canvas geometry
const canvasW = 900
const canvasH = 600
const ppm = computed(() => activeYard.value?.pixelsPerMeter ?? 20) // pixels per meter
function mToP(meters) { return (parseFloat(meters) || 0) * ppm.value }
const boundaryPoints = computed(() => activeYard.value?.boundary ?? [])

// Modals
const yardModal = ref(false)
const editingYard = ref(null)
const yardForm = ref({ name: '', backgroundImageUrl: '', laneMarginM: 0.5 })
const calibModal = ref(false)
const calibForm = ref({ originPixelX: 0, originPixelY: 0, pixelsPerMeter: 20, boundary: [] })

// Drag state
let dragging = null
let dragStart = null

async function load() {
  const [sRes, yRes] = await Promise.all([getWinterSeason(seasonId.value), getStorageYards(seasonId.value)])
  season.value = sRes.data
  yards.value = yRes.data
  if (!activeYardId.value && yRes.data.length) selectYard(yRes.data[0].id)
}

async function selectYard(id) {
  activeYardId.value = id
  selectedId.value = null
  validationIssues.value = []
  const [pRes, gRes] = await Promise.all([getStoragePlacements(id), getPackingGroups(id)])
  placements.value = pRes.data
  groups.value = gRes.data
}

async function suggestPlacements() {
  busy.value = true
  try {
    const { data } = await apiSuggest(activeYardId.value)
    placements.value = data
  } catch (e) { alert(e.response?.data?.error || 'Fel vid placeringsförslag') }
  finally { busy.value = false }
}

async function validateYardPlan() {
  const { data } = await apiValidate(activeYardId.value)
  validationIssues.value = data
  if (!data.length) alert('Inga konflikter hittades!')
}

function selectPlacement(p) { selectedId.value = p.id }
function deselectPlacement() { selectedId.value = null }

function startDrag(e, p) {
  e.preventDefault()
  dragging = p
  dragStart = { clientX: e.clientX, clientY: e.clientY, x: parseFloat(p.xMeters), y: parseFloat(p.yMeters) }
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}
function onDragMove(e) {
  if (!dragging) return
  const dx = (e.clientX - dragStart.clientX) / ppm.value
  const dy = (e.clientY - dragStart.clientY) / ppm.value
  const placement = placements.value.find(p => p.id === dragging.id)
  if (placement) {
    placement.xMeters = +(dragStart.x + dx).toFixed(3)
    placement.yMeters = +(dragStart.y + dy).toFixed(3)
  }
}
async function onDragEnd() {
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
  if (!dragging) return
  const p = placements.value.find(pp => pp.id === dragging.id)
  dragging = null
  if (p) await updateStoragePlacement(p.id, { xMeters: p.xMeters, yMeters: p.yMeters })
}
function onDrop() {}

async function markPlaced() {
  if (!selectedPlacement.value) return
  await updateStoragePlacement(selectedPlacement.value.id, { status: 'PLACED' })
  await selectYard(activeYardId.value)
}
async function launchPlacement() {
  if (!selectedPlacement.value) return
  try {
    await launchStoragePlacement(selectedPlacement.value.id)
    await selectYard(activeYardId.value)
  } catch (e) { alert(e.response?.data?.error || 'Fel vid sjösättning') }
}
async function assignGroup(groupId) {
  if (!selectedPlacement.value) return
  await updateStoragePlacement(selectedPlacement.value.id, { packingGroupId: groupId ? +groupId : null })
  await selectYard(activeYardId.value)
}
async function setGroupOrder(order) {
  if (!selectedPlacement.value) return
  await updateStoragePlacement(selectedPlacement.value.id, { orderInGroup: order })
  await selectYard(activeYardId.value)
}

async function createGroup() {
  const name = prompt('Gruppnamn:')
  if (!name) return
  const note = prompt('Hämtningsnotering (valfri):') ?? ''
  await createPackingGroup({ yardId: activeYardId.value, name, retrievalNote: note || null })
  const { data } = await getPackingGroups(activeYardId.value)
  groups.value = data
}
async function removeGroup(g) {
  if (!confirm(`Ta bort grupp ${g.name}?`)) return
  await deletePackingGroup(g.id)
  const { data } = await getPackingGroups(activeYardId.value)
  groups.value = data
}

function openCreateYard() { editingYard.value = null; yardForm.value = { name: '', backgroundImageUrl: '', laneMarginM: 0.5 }; err.value = ''; yardModal.value = true }
function openEditYard(y) {
  editingYard.value = y
  yardForm.value = { name: y.name, backgroundImageUrl: y.backgroundImageUrl ?? '', laneMarginM: y.laneMarginM ?? 0.5 }
  err.value = ''; yardModal.value = true
}
async function saveYard() {
  err.value = ''
  try {
    const payload = { ...yardForm.value, seasonId: seasonId.value, backgroundImageUrl: yardForm.value.backgroundImageUrl || null }
    if (editingYard.value) await updateStorageYard(editingYard.value.id, payload)
    else await createStorageYard(payload)
    yardModal.value = false; await load()
  } catch (e) { err.value = e.response?.data?.error || 'Något gick fel' }
}
async function removeYard(y) {
  if (!confirm(`Ta bort gårdsplan ${y.name}?`)) return
  await deleteStorageYard(y.id)
  activeYardId.value = null
  await load()
}

function openCalibration() {
  const y = activeYard.value
  calibForm.value = {
    originPixelX: y.originPixelX ?? 0,
    originPixelY: y.originPixelY ?? 0,
    pixelsPerMeter: y.pixelsPerMeter ?? 20,
    boundary: (y.boundary ?? []).map(p => ({ xMeters: parseFloat(p.xMeters), yMeters: parseFloat(p.yMeters) }))
  }
  err.value = ''; calibModal.value = true
}
async function saveCalibration() {
  err.value = ''
  try {
    await calibrateStorageYard(activeYardId.value, calibForm.value)
    calibModal.value = false; await load()
  } catch (e) { err.value = e.response?.data?.error || 'Något gick fel' }
}

// Visual helpers
function placementColor(p) {
  if (p.status === 'LAUNCHED') return '#d4edda'
  if (p.status === 'PLACED') return '#cce5ff'
  return '#f8d7da'
}

function boatSilhouette(p) {
  const w = mToP(p.widthMeters)
  const l = mToP(p.lengthMeters)
  const hull = p.hullType ?? 'OTHER'
  const taperPct = hull === 'SAIL' ? 0.30 : hull === 'RIB' ? 0.15 : 0.20
  const tipWidthPct = hull === 'SAIL' ? 0.15 : 0.35
  const taperLen = l * taperPct
  const tipW = w * tipWidthPct
  const midW = w
  // hexagon: bow taper at top (y=0), full width in middle, slight taper at stern
  const sternWidthPct = hull === 'SAIL' ? 0.70 : 0.88
  const sternW = w * sternWidthPct
  const cx = w / 2
  return [
    `${cx},0`,                          // bow tip
    `${cx - tipW / 2},${taperLen}`,     // bow port
    `${0},${l * 0.4}`,                  // mid port
    `${cx - sternW / 2},${l}`,          // stern port
    `${cx + sternW / 2},${l}`,          // stern starboard
    `${midW},${l * 0.4}`,               // mid starboard
    `${cx + tipW / 2},${taperLen}`,     // bow starboard
  ].join(' ')
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1rem; flex-wrap: wrap; gap: 0.5rem; }
h2 { font-size: 1.4rem; margin: 0.2rem 0 0; }
.back-link { font-size: 0.85rem; color: #1a3a5c; text-decoration: none; }
.back-link:hover { text-decoration: underline; }
.header-actions { display: flex; gap: 0.5rem; }
.empty-state { color: #999; text-align: center; padding: 3rem; }

.yard-tabs { display: flex; gap: 0.5rem; margin-bottom: 1rem; flex-wrap: wrap; }
.yard-tab { padding: 0.45rem 1rem; border-radius: 6px; border: 1px solid #ddd; background: white; cursor: pointer; font-size: 0.88rem; }
.yard-tab.active { background: #1a3a5c; color: white; border-color: #1a3a5c; }

.yard-panel { display: grid; grid-template-columns: 1fr 220px; gap: 1rem; }
.yard-toolbar { grid-column: 1 / -1; display: flex; gap: 0.5rem; flex-wrap: wrap; background: white; padding: 0.75rem 1rem; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.07); }

.issues-panel { grid-column: 1 / -1; background: #fff3cd; border-radius: 8px; padding: 0.75rem 1rem; }
.issue-row { font-size: 0.85rem; color: #856404; padding: 0.2rem 0; }

.canvas-container { background: white; border-radius: 8px; overflow: auto; box-shadow: 0 2px 8px rgba(0,0,0,0.07); cursor: crosshair; }
.yard-svg { display: block; }
.placement-group { cursor: grab; }
.placement-group:active { cursor: grabbing; }
.placement-group.selected polygon { stroke: #c0392b; stroke-width: 2; }
.placement-group.launched { opacity: 0.55; }

.side-panel { background: white; border-radius: 8px; padding: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.07); height: fit-content; }
.side-title { font-weight: 700; font-size: 1rem; color: #1a3a5c; }
.side-owner { font-size: 0.82rem; color: #888; margin-bottom: 0.75rem; }
.side-grid { display: grid; grid-template-columns: 60px 1fr; gap: 0.4rem 0.75rem; margin: 0 0 0.75rem; }
dt { font-size: 0.78rem; font-weight: 600; color: #888; }
dd { font-size: 0.85rem; color: #222; margin: 0; }
.side-grid select { font-size: 0.82rem; padding: 0.2rem; border: 1px solid #ddd; border-radius: 4px; width: 100%; }
.side-actions { display: flex; gap: 0.5rem; flex-wrap: wrap; }
.status-chip { font-size: 0.7rem; font-weight: 700; padding: 0.15rem 0.45rem; border-radius: 8px; }
.status-chip.planned { background: #f8d7da; color: #721c24; }
.status-chip.placed { background: #cce5ff; color: #004085; }
.status-chip.launched { background: #d4edda; color: #155724; }

.groups-section { grid-column: 1 / -1; background: white; border-radius: 8px; padding: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.07); }
.groups-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
.section-label { font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: #888; }
.group-row { display: flex; align-items: center; gap: 0.75rem; padding: 0.35rem 0; border-bottom: 1px solid #f0f0f0; font-size: 0.88rem; }
.group-row:last-child { border-bottom: none; }
.group-note { color: #888; font-size: 0.78rem; font-style: italic; flex: 1; }
.slots-empty { color: #bbb; font-size: 0.85rem; }

.btn-primary { background: #1a3a5c; color: white; padding: 0.5rem 1.1rem; border-radius: 6px; border: none; cursor: pointer; font-size: 0.85rem; }
.btn-primary:hover { background: #234e7a; }
.btn-secondary { background: #e8f0fe; color: #1a3a5c; padding: 0.4rem 0.8rem; border-radius: 5px; border: none; cursor: pointer; font-size: 0.82rem; }
.btn-secondary:hover { background: #d0e2ff; }
.btn-sm { background: #e8f0fe; color: #1a3a5c; padding: 0.3rem 0.65rem; border-radius: 4px; border: none; cursor: pointer; font-size: 0.8rem; }
.btn-sm:hover { background: #d0e2ff; }
.btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-danger-sm { background: #fee; color: #c0392b; padding: 0.25rem 0.6rem; border-radius: 4px; border: none; cursor: pointer; font-size: 0.78rem; }
.btn-danger-sm:hover { background: #fcc; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.form-grid .full { grid-column: 1 / -1; }
label { display: flex; flex-direction: column; font-size: 0.85rem; font-weight: 600; gap: 0.3rem; }
label input, label select { padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.95rem; font-weight: 400; }
.error { color: #c0392b; font-size: 0.85rem; margin-top: 0.5rem; }
.calib-help { font-size: 0.83rem; color: #666; margin-bottom: 0.75rem; }

.boundary-section { margin-top: 1rem; }
.boundary-row { display: flex; align-items: flex-end; gap: 0.5rem; margin-bottom: 0.4rem; }
</style>

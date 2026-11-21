<template>
  <AppLayout>
    <div class="toolbar">
      <div>
        <router-link to="/winter" class="back-link">← Vinterupptagning</router-link>
        <h2>Gårdsplan — {{ season?.name }}</h2>
      </div>
      <div class="right">
        <button class="btn btn-secondary btn-sm" @click="openCreateYard">+ Ny gårdsplan</button>
      </div>
    </div>

    <div v-if="!yards.length" class="empty-state">Inga gårdsplaner för denna säsong.</div>

    <!-- Yard tabs -->
    <div v-if="yards.length" class="yard-tabs">
      <button v-for="y in yards" :key="y.id"
              :class="['yardtab', activeYardId === y.id ? 'active' : '']"
              @click="selectYard(y.id)">
        {{ y.name }}
      </button>
    </div>

    <div v-if="activeYard" class="yard-panel">
      <!-- Toolbar -->
      <div class="yard-toolbar">
        <button class="btn btn-secondary btn-sm" @click="suggestPlacements" :disabled="busy">Föreslå placering</button>
        <button class="btn btn-secondary btn-sm" @click="validateYardPlan">Validera</button>
        <button class="btn btn-secondary btn-sm" @click="openCalibration">Kalibrera</button>
        <button class="btn btn-secondary btn-sm" @click="openEditYard(activeYard)">Redigera</button>
        <button class="btn btn-danger btn-sm" @click="removeYard(activeYard)">Ta bort</button>
      </div>

      <!-- Validation issues -->
      <div v-if="validationIssues.length" class="issues-panel">
        <div v-for="issue in validationIssues" :key="issue.placementId" class="issue-row">
          ⚠ {{ issue.boatModel }}: {{ issue.message }}
        </div>
      </div>

      <!-- Canvas -->
      <div class="canvas-container" @dragover.prevent @drop="onDrop">
        <svg :width="canvasW" :height="canvasH" class="yard-svg" data-graphic @click="deselectPlacement">
          <!-- Background image -->
          <image v-if="activeYard.backgroundImageUrl"
                 :href="activeYard.backgroundImageUrl"
                 x="0" y="0" :width="canvasW" :height="canvasH"
                 preserveAspectRatio="xMidYMid meet" />

          <!-- Boundary polygon -->
          <polygon v-if="boundaryPoints.length >= 3"
                   :points="boundaryPoints.map(p => `${mToP(p.xMeters)},${mToP(p.yMeters)}`).join(' ')"
                   class="yard-bounds" stroke-width="1.5" stroke-dasharray="6 4" />

          <!-- Placements -->
          <g v-for="p in placements" :key="p.id"
             :transform="`translate(${mToP(p.xMeters)},${mToP(p.yMeters)})`"
             class="placement-group"
             :class="['status-' + p.status.toLowerCase(), { selected: selectedId === p.id, launched: p.status === 'LAUNCHED' }]"
             @click.stop="selectPlacement(p)"
             @mousedown="startDrag($event, p)">
            <!-- Boat silhouette -->
            <polygon :points="boatSilhouette(p)" class="hull" stroke-width="1" />
            <!-- Label -->
            <text :x="mToP(p.widthMeters) / 2" :y="mToP(p.lengthMeters) / 2 + 4"
                  text-anchor="middle" font-size="10" class="hull-label"
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
          <dt>Status</dt><dd><span :class="['pill', placementPill(selectedPlacement.status)]">{{ selectedPlacement.status }}</span></dd>
          <dt>Grupp</dt>
          <dd>
            <select class="input" :value="selectedPlacement.packingGroupId ?? ''" @change="assignGroup($event.target.value)">
              <option value="">Ingen grupp</option>
              <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
            </select>
          </dd>
          <dt v-if="selectedPlacement.packingGroupId">Ordning</dt>
          <dd v-if="selectedPlacement.packingGroupId">
            <input class="input" type="number" :value="selectedPlacement.orderInGroup ?? ''" min="1"
                   @change="setGroupOrder(+$event.target.value)" style="width:60px" />
          </dd>
        </dl>
        <div class="side-actions">
          <button class="btn btn-secondary btn-sm" @click="markPlaced">Satt som PLACED</button>
          <button v-if="selectedPlacement.status !== 'LAUNCHED'" class="btn btn-secondary btn-sm" @click="launchPlacement">Sjösatt</button>
        </div>
      </div>

      <!-- Packing groups panel -->
      <div class="groups-section">
        <div class="groups-header">
          <span class="section-label">Packningsgrupper</span>
          <button class="btn btn-secondary btn-sm" @click="createGroup">+ Ny grupp</button>
        </div>
        <div v-for="g in groups" :key="g.id" class="group-row">
          <span>{{ g.name }}</span>
          <span v-if="g.retrievalNote" class="group-note">{{ g.retrievalNote }}</span>
          <button class="btn btn-danger btn-sm" @click="removeGroup(g)">Ta bort</button>
        </div>
        <div v-if="!groups.length" class="slots-empty">Inga grupper</div>
      </div>
    </div>

    <!-- Yard modal -->
    <BaseModal v-if="yardModal" :title="editingYard ? 'Redigera gårdsplan' : 'Ny gårdsplan'"
               @close="yardModal = false" @save="saveYard">
      <div class="form-grid">
        <label class="full">Namn *<input class="input" v-model="yardForm.name" /></label>
        <label class="full">Bakgrundsbild URL<input class="input" v-model="yardForm.backgroundImageUrl" placeholder="https://..." /></label>
        <label class="full">Marginal mellan båtar (m)<input class="input" v-model.number="yardForm.laneMarginM" type="number" step="0.1" min="0" /></label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>

    <!-- Calibration modal -->
    <BaseModal v-if="calibModal" title="Kalibrering" @close="calibModal = false" @save="saveCalibration">
      <p class="calib-help">Ange koordinatsystemets ursprung och skala. Pixelkoordinater läses från bakgrundsbild; meter-koordinater används för placering.</p>
      <div class="form-grid">
        <label>Ursprung pixel X<input class="input" v-model.number="calibForm.originPixelX" type="number" /></label>
        <label>Ursprung pixel Y<input class="input" v-model.number="calibForm.originPixelY" type="number" /></label>
        <label class="full">Pixlar per meter<input class="input" v-model.number="calibForm.pixelsPerMeter" type="number" step="0.1" /></label>
      </div>
      <div class="boundary-section">
        <div class="section-label">Gränspolygon (meter-koordinater)</div>
        <div v-for="(pt, i) in calibForm.boundary" :key="i" class="boundary-row">
          <label>X<input class="input" v-model.number="pt.xMeters" type="number" step="0.5" style="width:80px" /></label>
          <label>Y<input class="input" v-model.number="pt.yMeters" type="number" step="0.5" style="width:80px" /></label>
          <button class="btn btn-danger btn-sm" @click="calibForm.boundary.splice(i, 1)">✕</button>
        </div>
        <button class="btn btn-secondary btn-sm" @click="calibForm.boundary.push({ xMeters: 0, yMeters: 0 })">+ Punkt</button>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>
  </AppLayout>
</template>

<script setup>
/* Placement state in the guide's roles: waiting is yellow, in place is green,
   launched is done and closed. */
const placementPill = (s) =>
  ({ PLANNED: 'pill-warn', PLACED: 'pill-ok', LAUNCHED: 'pill-info' })[s] ?? 'pill-idle'

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
h2 { font-size: 1.45rem; margin: 2px 0 0; }
.back-link { font-size: 0.88rem; }
.empty-state, .slots-empty { color: var(--text-muted); }
.empty-state { text-align: center; padding: 44px; }

.yard-tabs { display: flex; gap: 4px; margin-bottom: 14px; flex-wrap: wrap; }
.yardtab {
  padding: 8px 14px;
  border: 0;
  border-radius: var(--radius-sm);
  background: none;
  color: var(--text-muted);
  font-family: var(--font);
  font-size: 0.92rem;
  font-weight: 500;
  cursor: pointer;
}
.yardtab:hover { background: var(--paper-deep); color: var(--ink); }
.yardtab.active { background: var(--sea-pale); color: var(--ink); }

.yard-panel { display: grid; grid-template-columns: 1fr 230px; gap: 14px; }
.yard-toolbar {
  grid-column: 1 / -1;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  background: var(--surface);
  border: 1px solid var(--paper-border);
  border-radius: var(--radius);
  padding: 10px 14px;
}

.issues-panel {
  grid-column: 1 / -1;
  background: var(--warn-bg);
  border: 1px solid var(--warn-border);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
}
.issue-row { font-size: 0.88rem; color: var(--warn-text); padding: 2px 0; }

.canvas-container {
  background: var(--surface);
  border: 1px solid var(--paper-border);
  border-radius: var(--radius);
  overflow: auto;
  cursor: crosshair;
}
.yard-svg { display: block; }

/* The yard is a data graphic: the hull wears its status fill from the map palette. */
.yard-bounds { fill: rgba(16, 32, 52, 0.04); stroke: var(--ink); }
.placement-group { cursor: grab; }
.placement-group:active { cursor: grabbing; }
.placement-group .hull { stroke: var(--ink); fill: var(--map-free-bg); }
.placement-group.status-planned .hull { fill: var(--map-service-bg); }
.placement-group.status-placed .hull { fill: var(--map-taken-bg); }
.placement-group.status-launched .hull { fill: var(--info-bg); }
.placement-group.selected .hull { stroke: var(--sea); stroke-width: 2; }
.placement-group.launched { opacity: 0.55; }
.hull-label { fill: var(--ink); font-family: var(--font-mono); font-weight: 500; }

.side-panel {
  background: var(--surface);
  border: 1px solid var(--paper-border);
  border-radius: var(--radius);
  padding: 14px;
  height: fit-content;
}
.side-title { font-weight: 600; font-size: 1rem; color: var(--ink); }
.side-owner { font-size: 0.85rem; color: var(--text-muted); margin-bottom: 10px; }
.side-grid { display: grid; grid-template-columns: 70px 1fr; gap: 8px 14px; margin: 0 0 14px; }
dt { font-family: var(--font-mono); font-size: 0.68rem; letter-spacing: 0.12em; text-transform: uppercase; color: var(--text-muted); }
dd { font-size: 0.88rem; margin: 0; }
.side-grid .input { font-size: 0.85rem; padding: 5px 8px; width: 100%; }
.side-actions { display: flex; gap: 10px; flex-wrap: wrap; }

.groups-section {
  grid-column: 1 / -1;
  background: var(--surface);
  border: 1px solid var(--paper-border);
  border-radius: var(--radius);
  padding: 14px 18px;
}
.groups-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.section-label {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.group-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 0;
  border-bottom: 1px solid var(--paper-border);
  font-size: 0.9rem;
}
.group-row:last-child { border-bottom: 0; }
.group-note { color: var(--text-muted); font-size: 0.82rem; flex: 1; }

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
.calib-help { font-size: 0.88rem; color: var(--text-muted); margin-bottom: 14px; }
.boundary-section { margin-top: 14px; }
.boundary-row { display: flex; align-items: flex-end; gap: 10px; margin-bottom: 8px; }

@media (max-width: 980px) {
  .yard-panel { grid-template-columns: 1fr; }
}
</style>

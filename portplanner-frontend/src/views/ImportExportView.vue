<template>
  <AppLayout>
    <h2>Import / Export</h2>

    <!-- ═══ BRYGGOR – EXPORT ════════════════════════════════════════════ -->
    <section class="card">
      <h3>Exportera bryggor &amp; platser</h3>
      <p class="desc">Laddar ner alla bryggor och båtplatser som en JSON-fil du kan spara lokalt.</p>
      <button class="btn-primary" :disabled="exporting" @click="doExport">
        {{ exporting ? 'Exporterar…' : '⬇ Ladda ner bryggor.json' }}
      </button>
    </section>

    <!-- ═══ BRYGGOR – IMPORT ════════════════════════════════════════════ -->
    <section class="card">
      <h3>Importera bryggor &amp; platser</h3>
      <p class="desc">
        Klistra in eller ladda upp en JSON-fil. Befintliga bryggor uppdateras inte –
        bara nya bryggor och platser läggs till.
      </p>

      <div class="input-row">
        <label class="file-btn">
          📂 Välj fil
          <input type="file" accept=".json,application/json" @change="onFileSelected($event, 'docks')" hidden />
        </label>
        <span class="or">eller klistra in JSON nedan</span>
      </div>

      <textarea
        v-model="dockJsonText"
        rows="10"
        placeholder='[{ "name": "Bryggans namn", "slips": [{ "slipNumber": "1", "maxWidthM": 3.2 }] }]'
      />

      <div class="btn-row">
        <button class="btn-secondary" :disabled="!dockJsonText.trim() || dockPreviewing" @click="doPreview">
          {{ dockPreviewing ? 'Kontrollerar…' : '🔍 Förhandsgranska' }}
        </button>
        <button class="btn-primary" :disabled="!dockPreview || dockImporting" @click="doImport">
          {{ dockImporting ? 'Importerar…' : '⬆ Importera' }}
        </button>
      </div>

      <p v-if="dockParseError" class="error">{{ dockParseError }}</p>

      <div v-if="dockPreview" class="preview-box">
        <h4>Förhandsgranskning</h4>
        <div class="stats">
          <div class="stat"><span class="stat-val green">{{ dockPreview.docksNew }}</span><span class="stat-label">Nya bryggor</span></div>
          <div class="stat"><span class="stat-val blue">{{ dockPreview.docksExisting }}</span><span class="stat-label">Befintliga</span></div>
          <div class="stat"><span class="stat-val green">{{ dockPreview.slipsNew }}</span><span class="stat-label">Nya platser</span></div>
          <div class="stat"><span class="stat-val gray">{{ dockPreview.slipsSkipped }}</span><span class="stat-label">Hoppar över</span></div>
        </div>
        <ul v-if="dockPreview.details.length" class="details">
          <li v-for="(d, i) in dockPreview.details" :key="i">{{ d }}</li>
        </ul>
      </div>

      <div v-if="dockResult" class="result-box" :class="dockResult.warnings.length ? 'warn' : 'ok'">
        <h4>Import klar</h4>
        <div class="stats">
          <div class="stat"><span class="stat-val green">{{ dockResult.docksCreated }}</span><span class="stat-label">Bryggor skapade</span></div>
          <div class="stat"><span class="stat-val green">{{ dockResult.slipsCreated }}</span><span class="stat-label">Platser skapade</span></div>
          <div class="stat"><span class="stat-val gray">{{ dockResult.slipsSkipped }}</span><span class="stat-label">Hoppade över</span></div>
        </div>
        <ul v-if="dockResult.warnings.length" class="details warn-list">
          <li v-for="(w, i) in dockResult.warnings" :key="i">⚠ {{ w }}</li>
        </ul>
      </div>
    </section>

    <!-- ═══ BÅTAR – EXPORT ═══════════════════════════════════════════════ -->
    <section class="card">
      <h3>Exportera båtar</h3>
      <p class="desc">Laddar ner alla båtar (modell, mått, ägare via e-post) som en JSON-fil.</p>
      <button class="btn-primary" :disabled="boatExporting" @click="doBoatExport">
        {{ boatExporting ? 'Exporterar…' : '⬇ Ladda ner båtar.json' }}
      </button>
    </section>

    <!-- ═══ BÅTAR – IMPORT ════════════════════════════════════════════════ -->
    <section class="card">
      <h3>Importera båtar</h3>
      <p class="desc">
        Klistra in eller ladda upp en JSON-fil med båtar. Matchning sker på båtmodell + ägarens e-postadress –
        befintliga kombinationer hoppas över. Ägaren måste finnas som person i systemet.
      </p>

      <div class="input-row">
        <label class="file-btn">
          📂 Välj fil
          <input type="file" accept=".json,application/json" @change="onFileSelected($event, 'boats')" hidden />
        </label>
        <span class="or">eller klistra in JSON nedan</span>
      </div>

      <textarea
        v-model="boatJsonText"
        rows="10"
        placeholder='[
  {
    "model": "Mästerkatten",
    "lengthM": 8.5,
    "widthM": 2.8,
    "draftM": 1.2,
    "ownerEmail": "anna@example.com"
  }
]'
      />

      <div class="btn-row">
        <button class="btn-secondary" :disabled="!boatJsonText.trim() || boatPreviewing" @click="doBoatPreview">
          {{ boatPreviewing ? 'Kontrollerar…' : '🔍 Förhandsgranska' }}
        </button>
        <button class="btn-primary" :disabled="!boatPreview || boatImporting" @click="doBoatImport">
          {{ boatImporting ? 'Importerar…' : '⬆ Importera' }}
        </button>
      </div>

      <p v-if="boatParseError" class="error">{{ boatParseError }}</p>

      <div v-if="boatPreview" class="preview-box">
        <h4>Förhandsgranskning</h4>
        <div class="stats">
          <div class="stat"><span class="stat-val green">{{ boatPreview.boatsNew }}</span><span class="stat-label">Nya båtar</span></div>
          <div class="stat"><span class="stat-val blue">{{ boatPreview.boatsExisting }}</span><span class="stat-label">Befintliga</span></div>
          <div class="stat"><span class="stat-val gray">{{ boatPreview.ownersNotFound }}</span><span class="stat-label">Ägare ej hittad</span></div>
        </div>
        <ul v-if="boatPreview.details.length" class="details">
          <li v-for="(d, i) in boatPreview.details" :key="i">{{ d }}</li>
        </ul>
      </div>

      <div v-if="boatResult" class="result-box" :class="boatResult.warnings.length ? 'warn' : 'ok'">
        <h4>Import klar</h4>
        <div class="stats">
          <div class="stat"><span class="stat-val green">{{ boatResult.boatsCreated }}</span><span class="stat-label">Skapade</span></div>
          <div class="stat"><span class="stat-val gray">{{ boatResult.boatsSkipped }}</span><span class="stat-label">Hoppade över</span></div>
        </div>
        <ul v-if="boatResult.warnings.length" class="details warn-list">
          <li v-for="(w, i) in boatResult.warnings" :key="i">⚠ {{ w }}</li>
        </ul>
      </div>
    </section>

    <!-- ═══ PERSONER – EXPORT ════════════════════════════════════════════ -->
    <section class="card">
      <h3>Exportera personer</h3>
      <p class="desc">Laddar ner alla personer (namn, e-post, telefon) som en JSON-fil.</p>
      <button class="btn-primary" :disabled="personExporting" @click="doPersonExport">
        {{ personExporting ? 'Exporterar…' : '⬇ Ladda ner personer.json' }}
      </button>
    </section>

    <!-- ═══ PERSONER – IMPORT ════════════════════════════════════════════ -->
    <section class="card">
      <h3>Importera personer</h3>
      <p class="desc">
        Klistra in eller ladda upp en JSON-fil med personer. Matchning sker på e-postadress –
        befintliga personer hoppas över, bara nya läggs till.
      </p>

      <div class="input-row">
        <label class="file-btn">
          📂 Välj fil
          <input type="file" accept=".json,application/json" @change="onFileSelected($event, 'persons')" hidden />
        </label>
        <span class="or">eller klistra in JSON nedan</span>
      </div>

      <textarea
        v-model="personJsonText"
        rows="10"
        placeholder='[
  {
    "firstName": "Anna",
    "lastName": "Svensson",
    "email": "anna@example.com",
    "phone": "070-123 45 67"
  }
]'
      />

      <div class="btn-row">
        <button class="btn-secondary" :disabled="!personJsonText.trim() || personPreviewing" @click="doPersonPreview">
          {{ personPreviewing ? 'Kontrollerar…' : '🔍 Förhandsgranska' }}
        </button>
        <button class="btn-primary" :disabled="!personPreview || personImporting" @click="doPersonImport">
          {{ personImporting ? 'Importerar…' : '⬆ Importera' }}
        </button>
      </div>

      <p v-if="personParseError" class="error">{{ personParseError }}</p>

      <div v-if="personPreview" class="preview-box">
        <h4>Förhandsgranskning</h4>
        <div class="stats">
          <div class="stat"><span class="stat-val green">{{ personPreview.personsNew }}</span><span class="stat-label">Nya personer</span></div>
          <div class="stat"><span class="stat-val blue">{{ personPreview.personsExisting }}</span><span class="stat-label">Befintliga</span></div>
        </div>
        <ul v-if="personPreview.details.length" class="details">
          <li v-for="(d, i) in personPreview.details" :key="i">{{ d }}</li>
        </ul>
      </div>

      <div v-if="personResult" class="result-box" :class="personResult.warnings.length ? 'warn' : 'ok'">
        <h4>Import klar</h4>
        <div class="stats">
          <div class="stat"><span class="stat-val green">{{ personResult.personsCreated }}</span><span class="stat-label">Skapade</span></div>
          <div class="stat"><span class="stat-val gray">{{ personResult.personsSkipped }}</span><span class="stat-label">Hoppade över</span></div>
        </div>
        <ul v-if="personResult.warnings.length" class="details warn-list">
          <li v-for="(w, i) in personResult.warnings" :key="i">⚠ {{ w }}</li>
        </ul>
      </div>
    </section>
  </AppLayout>
</template>

<script setup>
import { ref } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import {
  exportDocks, previewImport, importDocks,
  exportPersons, previewPersonImport, importPersons,
  exportBoats, previewBoatImport, importBoats,
} from '../api/importExport'

// ── Docks ────────────────────────────────────────────────────────────────
const dockJsonText   = ref('')
const dockParseError = ref('')
const dockPreview    = ref(null)
const dockResult     = ref(null)
const exporting      = ref(false)
const dockPreviewing = ref(false)
const dockImporting  = ref(false)

function parseDockJson() {
  dockParseError.value = ''
  try {
    const data = JSON.parse(dockJsonText.value)
    if (!Array.isArray(data)) throw new Error('JSON måste vara en array')
    return data
  } catch (e) {
    dockParseError.value = 'Ogiltig JSON: ' + e.message
    return null
  }
}

async function doExport() {
  exporting.value = true
  try {
    const { data } = await exportDocks()
    downloadJson(data, 'bryggor.json')
  } finally { exporting.value = false }
}

async function doPreview() {
  const data = parseDockJson()
  if (!data) return
  dockPreview.value = null; dockResult.value = null; dockPreviewing.value = true
  try {
    const { data: p } = await previewImport(data)
    dockPreview.value = p
  } catch (e) { dockParseError.value = e.response?.data?.error || 'Fel vid förhandsgranskning' }
  finally { dockPreviewing.value = false }
}

async function doImport() {
  const data = parseDockJson()
  if (!data) return
  dockResult.value = null; dockImporting.value = true
  try {
    const { data: r } = await importDocks(data)
    dockResult.value = r; dockPreview.value = null
  } catch (e) { dockParseError.value = e.response?.data?.error || 'Fel vid import' }
  finally { dockImporting.value = false }
}

// ── Persons ───────────────────────────────────────────────────────────────
const personJsonText   = ref('')
const personParseError = ref('')
const personPreview    = ref(null)
const personResult     = ref(null)
const personExporting  = ref(false)
const personPreviewing = ref(false)
const personImporting  = ref(false)

function parsePersonJson() {
  personParseError.value = ''
  try {
    const data = JSON.parse(personJsonText.value)
    if (!Array.isArray(data)) throw new Error('JSON måste vara en array')
    return data
  } catch (e) {
    personParseError.value = 'Ogiltig JSON: ' + e.message
    return null
  }
}

async function doPersonExport() {
  personExporting.value = true
  try {
    const { data } = await exportPersons()
    downloadJson(data, 'personer.json')
  } finally { personExporting.value = false }
}

async function doPersonPreview() {
  const data = parsePersonJson()
  if (!data) return
  personPreview.value = null; personResult.value = null; personPreviewing.value = true
  try {
    const { data: p } = await previewPersonImport(data)
    personPreview.value = p
  } catch (e) { personParseError.value = e.response?.data?.error || 'Fel vid förhandsgranskning' }
  finally { personPreviewing.value = false }
}

async function doPersonImport() {
  const data = parsePersonJson()
  if (!data) return
  personResult.value = null; personImporting.value = true
  try {
    const { data: r } = await importPersons(data)
    personResult.value = r; personPreview.value = null
  } catch (e) { personParseError.value = e.response?.data?.error || 'Fel vid import' }
  finally { personImporting.value = false }
}

// ── Boats ─────────────────────────────────────────────────────────────────
const boatJsonText   = ref('')
const boatParseError = ref('')
const boatPreview    = ref(null)
const boatResult     = ref(null)
const boatExporting  = ref(false)
const boatPreviewing = ref(false)
const boatImporting  = ref(false)

function parseBoatJson() {
  boatParseError.value = ''
  try {
    const data = JSON.parse(boatJsonText.value)
    if (!Array.isArray(data)) throw new Error('JSON måste vara en array')
    return data
  } catch (e) {
    boatParseError.value = 'Ogiltig JSON: ' + e.message
    return null
  }
}

async function doBoatExport() {
  boatExporting.value = true
  try {
    const { data } = await exportBoats()
    downloadJson(data, 'båtar.json')
  } finally { boatExporting.value = false }
}

async function doBoatPreview() {
  const data = parseBoatJson()
  if (!data) return
  boatPreview.value = null; boatResult.value = null; boatPreviewing.value = true
  try {
    const { data: p } = await previewBoatImport(data)
    boatPreview.value = p
  } catch (e) { boatParseError.value = e.response?.data?.error || 'Fel vid förhandsgranskning' }
  finally { boatPreviewing.value = false }
}

async function doBoatImport() {
  const data = parseBoatJson()
  if (!data) return
  boatResult.value = null; boatImporting.value = true
  try {
    const { data: r } = await importBoats(data)
    boatResult.value = r; boatPreview.value = null
  } catch (e) { boatParseError.value = e.response?.data?.error || 'Fel vid import' }
  finally { boatImporting.value = false }
}

// ── Shared ────────────────────────────────────────────────────────────────
function onFileSelected(e, target) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => {
    if (target === 'docks') {
      dockJsonText.value = ev.target.result
      dockPreview.value = null; dockResult.value = null; dockParseError.value = ''
    } else if (target === 'boats') {
      boatJsonText.value = ev.target.result
      boatPreview.value = null; boatResult.value = null; boatParseError.value = ''
    } else {
      personJsonText.value = ev.target.result
      personPreview.value = null; personResult.value = null; personParseError.value = ''
    }
  }
  reader.readAsText(file)
}

function downloadJson(data, filename) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = filename; a.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
h2 { font-size: 1.5rem; margin-bottom: 1.5rem; }
h3 { font-size: 1.1rem; margin-bottom: 0.4rem; }
h4 { font-size: 0.95rem; margin-bottom: 0.75rem; color: #333; }
.card { background: white; border-radius: 10px; padding: 1.5rem; margin-bottom: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.07); }
.desc { color: #666; font-size: 0.9rem; margin-bottom: 1rem; }
.input-row { display: flex; align-items: center; gap: 1rem; margin-bottom: 0.75rem; }
.file-btn { background: #eee; padding: 0.45rem 1rem; border-radius: 6px; cursor: pointer; font-size: 0.9rem; }
.file-btn:hover { background: #ddd; }
.or { color: #999; font-size: 0.85rem; }
textarea { width: 100%; padding: 0.75rem; border: 1px solid #ddd; border-radius: 6px; font-family: monospace; font-size: 0.82rem; resize: vertical; box-sizing: border-box; }
.btn-row { display: flex; gap: 0.75rem; margin-top: 0.75rem; }
.btn-primary { background: #1a3a5c; color: white; border: none; padding: 0.5rem 1.25rem; border-radius: 6px; cursor: pointer; font-size: 0.9rem; }
.btn-primary:hover:not(:disabled) { background: #234e7a; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { background: #eee; color: #333; border: none; padding: 0.5rem 1.25rem; border-radius: 6px; cursor: pointer; font-size: 0.9rem; }
.btn-secondary:hover:not(:disabled) { background: #ddd; }
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }
.error { color: #c0392b; font-size: 0.875rem; margin-top: 0.5rem; }
.preview-box { margin-top: 1.25rem; background: #f0f4ff; border: 1px solid #c5d4f0; border-radius: 8px; padding: 1rem; }
.result-box { margin-top: 1.25rem; border-radius: 8px; padding: 1rem; }
.result-box.ok   { background: #f0fff4; border: 1px solid #b2dfdb; }
.result-box.warn { background: #fffbf0; border: 1px solid #ffe082; }
.stats { display: flex; gap: 1.5rem; margin-bottom: 0.75rem; flex-wrap: wrap; }
.stat { display: flex; flex-direction: column; align-items: center; }
.stat-val { font-size: 1.75rem; font-weight: 700; line-height: 1; }
.stat-val.green { color: #2e7d32; }
.stat-val.blue  { color: #1565c0; }
.stat-val.gray  { color: #757575; }
.stat-label { font-size: 0.75rem; color: #555; margin-top: 0.2rem; }
.details { list-style: none; padding: 0; font-size: 0.85rem; color: #444; }
.details li { padding: 0.2rem 0; border-bottom: 1px solid rgba(0,0,0,0.05); }
.warn-list li { color: #7c5700; }
</style>

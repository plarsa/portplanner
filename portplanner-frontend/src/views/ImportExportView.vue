<template>
  <AppLayout>
    <h2>Import / Export</h2>

    <!-- ═══ DOCKS – EXPORT ══════════════════════════════════════════════ -->
    <section class="panel section">
      <h3>Exportera bryggor &amp; platser</h3>
      <p class="desc">Laddar ner alla bryggor och båtplatser som en JSON-fil du kan spara lokalt.</p>
      <button class="btn btn-primary btn-sm" :disabled="exporting" @click="doExport">
        {{ exporting ? 'Exporterar…' : '⬇ Ladda ner bryggor.json' }}
      </button>
    </section>

    <!-- ═══ DOCKS – IMPORT ══════════════════════════════════════════════ -->
    <section class="panel section">
      <h3>Importera bryggor &amp; platser</h3>
      <p class="desc">
        Klistra in eller ladda upp en JSON-fil. Befintliga bryggor uppdateras inte –
        bara nya bryggor och platser läggs till.
      </p>

      <div class="input-row">
        <label class="file-btn">
          📂 Välj fil
          <input class="input" type="file" accept=".json,application/json" @change="onFileSelected($event, 'docks')" hidden />
        </label>
        <span class="or">eller klistra in JSON nedan</span>
      </div>

      <textarea class="input"
        v-model="dockJsonText"
        rows="10"
        placeholder='[{ "name": "Bryggans namn", "slips": [{ "slipNumber": "1", "maxWidthM": 3.2 }] }]'
      />

      <div class="btn-row">
        <button class="btn btn-secondary btn-sm" :disabled="!dockJsonText.trim() || dockPreviewing" @click="doPreview">
          {{ dockPreviewing ? 'Kontrollerar…' : '🔍 Förhandsgranska' }}
        </button>
        <button class="btn btn-primary btn-sm" :disabled="!dockPreview || dockImporting" @click="doImport">
          {{ dockImporting ? 'Importerar…' : '⬆ Importera' }}
        </button>
      </div>

      <p v-if="dockParseError" class="error">{{ dockParseError }}</p>

      <div v-if="dockPreview" class="preview-box">
        <h4>Förhandsgranskning</h4>
        <div class="stats">
          <div class="stat"><span class="stat-val num ok">{{ dockPreview.docksNew }}</span><span class="stat-label">Nya bryggor</span></div>
          <div class="stat"><span class="stat-val num info">{{ dockPreview.docksExisting }}</span><span class="stat-label">Befintliga</span></div>
          <div class="stat"><span class="stat-val num ok">{{ dockPreview.slipsNew }}</span><span class="stat-label">Nya platser</span></div>
          <div class="stat"><span class="stat-val num idle">{{ dockPreview.slipsSkipped }}</span><span class="stat-label">Hoppar över</span></div>
        </div>
        <ul v-if="dockPreview.details.length" class="details">
          <li v-for="(d, i) in dockPreview.details" :key="i">{{ d }}</li>
        </ul>
      </div>

      <div v-if="dockResult" class="result-box" :class="dockResult.warnings.length ? 'warn' : 'ok'">
        <h4>Import klar</h4>
        <div class="stats">
          <div class="stat"><span class="stat-val num ok">{{ dockResult.docksCreated }}</span><span class="stat-label">Bryggor skapade</span></div>
          <div class="stat"><span class="stat-val num ok">{{ dockResult.slipsCreated }}</span><span class="stat-label">Platser skapade</span></div>
          <div class="stat"><span class="stat-val num idle">{{ dockResult.slipsSkipped }}</span><span class="stat-label">Hoppade över</span></div>
        </div>
        <ul v-if="dockResult.warnings.length" class="details warn-list">
          <li v-for="(w, i) in dockResult.warnings" :key="i">⚠ {{ w }}</li>
        </ul>
      </div>
    </section>

    <!-- ═══ BOATS – EXPORT ══════════════════════════════════════════════ -->
    <section class="panel section">
      <h3>Exportera båtar</h3>
      <p class="desc">Laddar ner alla båtar (modell, mått, ägare via e-post) som en JSON-fil.</p>
      <button class="btn btn-primary btn-sm" :disabled="boatExporting" @click="doBoatExport">
        {{ boatExporting ? 'Exporterar…' : '⬇ Ladda ner båtar.json' }}
      </button>
    </section>

    <!-- ═══ BOATS – IMPORT ══════════════════════════════════════════════ -->
    <section class="panel section">
      <h3>Importera båtar</h3>
      <p class="desc">
        Klistra in eller ladda upp en JSON-fil med båtar. Matchning sker på båtmodell + ägarens e-postadress –
        befintliga kombinationer hoppas över. Ägaren måste finnas som person i systemet.
      </p>

      <div class="input-row">
        <label class="file-btn">
          📂 Välj fil
          <input class="input" type="file" accept=".json,application/json" @change="onFileSelected($event, 'boats')" hidden />
        </label>
        <span class="or">eller klistra in JSON nedan</span>
      </div>

      <textarea class="input"
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
        <button class="btn btn-secondary btn-sm" :disabled="!boatJsonText.trim() || boatPreviewing" @click="doBoatPreview">
          {{ boatPreviewing ? 'Kontrollerar…' : '🔍 Förhandsgranska' }}
        </button>
        <button class="btn btn-primary btn-sm" :disabled="!boatPreview || boatImporting" @click="doBoatImport">
          {{ boatImporting ? 'Importerar…' : '⬆ Importera' }}
        </button>
      </div>

      <p v-if="boatParseError" class="error">{{ boatParseError }}</p>

      <div v-if="boatPreview" class="preview-box">
        <h4>Förhandsgranskning</h4>
        <div class="stats">
          <div class="stat"><span class="stat-val num ok">{{ boatPreview.boatsNew }}</span><span class="stat-label">Nya båtar</span></div>
          <div class="stat"><span class="stat-val num info">{{ boatPreview.boatsExisting }}</span><span class="stat-label">Befintliga</span></div>
          <div class="stat"><span class="stat-val num idle">{{ boatPreview.ownersNotFound }}</span><span class="stat-label">Ägare ej hittad</span></div>
        </div>
        <ul v-if="boatPreview.details.length" class="details">
          <li v-for="(d, i) in boatPreview.details" :key="i">{{ d }}</li>
        </ul>
      </div>

      <div v-if="boatResult" class="result-box" :class="boatResult.warnings.length ? 'warn' : 'ok'">
        <h4>Import klar</h4>
        <div class="stats">
          <div class="stat"><span class="stat-val num ok">{{ boatResult.boatsCreated }}</span><span class="stat-label">Skapade</span></div>
          <div class="stat"><span class="stat-val num idle">{{ boatResult.boatsSkipped }}</span><span class="stat-label">Hoppade över</span></div>
        </div>
        <ul v-if="boatResult.warnings.length" class="details warn-list">
          <li v-for="(w, i) in boatResult.warnings" :key="i">⚠ {{ w }}</li>
        </ul>
      </div>
    </section>

    <!-- ═══ PERSONS – EXPORT ════════════════════════════════════════════ -->
    <section class="panel section">
      <h3>Exportera personer</h3>
      <p class="desc">Laddar ner alla personer (namn, e-post, telefon) som en JSON-fil.</p>
      <button class="btn btn-primary btn-sm" :disabled="personExporting" @click="doPersonExport">
        {{ personExporting ? 'Exporterar…' : '⬇ Ladda ner personer.json' }}
      </button>
    </section>

    <!-- ═══ PERSONS – IMPORT ════════════════════════════════════════════ -->
    <section class="panel section">
      <h3>Importera personer</h3>
      <p class="desc">
        Klistra in eller ladda upp en JSON-fil med personer. Matchning sker på e-postadress –
        befintliga personer hoppas över, bara nya läggs till.
      </p>

      <div class="input-row">
        <label class="file-btn">
          📂 Välj fil
          <input class="input" type="file" accept=".json,application/json" @change="onFileSelected($event, 'persons')" hidden />
        </label>
        <span class="or">eller klistra in JSON nedan</span>
      </div>

      <textarea class="input"
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
        <button class="btn btn-secondary btn-sm" :disabled="!personJsonText.trim() || personPreviewing" @click="doPersonPreview">
          {{ personPreviewing ? 'Kontrollerar…' : '🔍 Förhandsgranska' }}
        </button>
        <button class="btn btn-primary btn-sm" :disabled="!personPreview || personImporting" @click="doPersonImport">
          {{ personImporting ? 'Importerar…' : '⬆ Importera' }}
        </button>
      </div>

      <p v-if="personParseError" class="error">{{ personParseError }}</p>

      <div v-if="personPreview" class="preview-box">
        <h4>Förhandsgranskning</h4>
        <div class="stats">
          <div class="stat"><span class="stat-val num ok">{{ personPreview.personsNew }}</span><span class="stat-label">Nya personer</span></div>
          <div class="stat"><span class="stat-val num info">{{ personPreview.personsExisting }}</span><span class="stat-label">Befintliga</span></div>
        </div>
        <ul v-if="personPreview.details.length" class="details">
          <li v-for="(d, i) in personPreview.details" :key="i">{{ d }}</li>
        </ul>
      </div>

      <div v-if="personResult" class="result-box" :class="personResult.warnings.length ? 'warn' : 'ok'">
        <h4>Import klar</h4>
        <div class="stats">
          <div class="stat"><span class="stat-val num ok">{{ personResult.personsCreated }}</span><span class="stat-label">Skapade</span></div>
          <div class="stat"><span class="stat-val num idle">{{ personResult.personsSkipped }}</span><span class="stat-label">Hoppade över</span></div>
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
h2 { font-size: 1.45rem; margin-bottom: 24px; }
h3 { margin-bottom: 6px; }
h4 { font-size: 0.95rem; margin-bottom: 10px; color: var(--ink); }
.desc { color: var(--text-muted); font-size: 0.9rem; margin-bottom: 14px; }

.input-row { display: flex; align-items: center; gap: 14px; margin-bottom: 10px; flex-wrap: wrap; }
.file-btn {
  display: inline-flex;
  align-items: center;
  padding: 9px 15px;
  border: 1px solid var(--paper-border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--sea);
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
}
.file-btn:hover { background: var(--sea-pale); border-color: var(--info-border); }
.or { color: var(--text-muted); font-size: 0.88rem; }

/* Pasted JSON is code, so it stays in mono. */
textarea.input { width: 100%; font-family: var(--font-mono); font-size: 0.82rem; resize: vertical; }
.btn-row { display: flex; gap: 10px; margin-top: 10px; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }

.preview-box, .result-box {
  margin-top: 22px;
  border-radius: var(--radius-sm);
  padding: 14px;
  border: 1px solid var(--paper-border);
  background: var(--paper);
}
.result-box.ok { background: var(--ok-bg); border-color: var(--ok-border); color: var(--ok-text); }
.result-box.warn { background: var(--warn-bg); border-color: var(--warn-border); color: var(--warn-text); }

.stats { display: flex; gap: 22px; margin-bottom: 14px; flex-wrap: wrap; }
.stat { display: flex; flex-direction: column; align-items: center; }
.stat-val { font-size: 1.6rem; font-weight: 600; line-height: 1; }
.stat-val.ok { color: var(--ok-text); }
.stat-val.info { color: var(--info-text); }
.stat-val.idle { color: var(--idle-text); }
.stat-label {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-top: 6px;
}
.details { list-style: none; padding: 0; font-size: 0.88rem; }
.details li { padding: 4px 0; border-bottom: 1px solid var(--paper-border); }
.details li:last-child { border-bottom: 0; }
.warn-list li { color: var(--warn-text); }
</style>

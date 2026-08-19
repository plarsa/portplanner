<template>
  <AppLayout>
    <h2>Import / Export</h2>

    <!-- EXPORT -->
    <section class="card">
      <h3>Exportera bryggor &amp; platser</h3>
      <p class="desc">Laddar ner alla bryggor och båtplatser som en JSON-fil du kan spara lokalt.</p>
      <button class="btn-primary" :disabled="exporting" @click="doExport">
        {{ exporting ? 'Exporterar…' : '⬇ Ladda ner bryggor.json' }}
      </button>
    </section>

    <!-- IMPORT -->
    <section class="card">
      <h3>Importera bryggor &amp; platser</h3>
      <p class="desc">
        Klistra in eller ladda upp en JSON-fil. Befintliga bryggor uppdateras inte –
        bara nya bryggor och platser läggs till.
      </p>

      <div class="input-row">
        <label class="file-btn">
          📂 Välj fil
          <input type="file" accept=".json,application/json" @change="onFileSelected" hidden />
        </label>
        <span class="or">eller klistra in JSON nedan</span>
      </div>

      <textarea
        v-model="jsonText"
        rows="12"
        placeholder='[
  {
    "name": "Bryggans namn",
    "description": "Valfri beskrivning",
    "slips": [
      {
        "slipNumber": "1",
        "maxWidthM": 3.2,
        "maxLengthM": 7.4,
        "maxDraftM": 1.0,
        "mooringType": "Stolpe",
        "side": "Vänster"
      }
    ]
  }
]'
      />

      <div class="btn-row">
        <button class="btn-secondary" :disabled="!jsonText.trim() || previewing" @click="doPreview">
          {{ previewing ? 'Kontrollerar…' : '🔍 Förhandsgranska' }}
        </button>
        <button class="btn-primary" :disabled="!preview || importing" @click="doImport">
          {{ importing ? 'Importerar…' : '⬆ Importera' }}
        </button>
      </div>

      <p v-if="parseError" class="error">{{ parseError }}</p>

      <!-- Förhandsgranskning -->
      <div v-if="preview" class="preview-box">
        <h4>Förhandsgranskning</h4>
        <div class="stats">
          <div class="stat">
            <span class="stat-val green">{{ preview.docksNew }}</span>
            <span class="stat-label">Nya bryggor</span>
          </div>
          <div class="stat">
            <span class="stat-val blue">{{ preview.docksExisting }}</span>
            <span class="stat-label">Befintliga bryggor</span>
          </div>
          <div class="stat">
            <span class="stat-val green">{{ preview.slipsNew }}</span>
            <span class="stat-label">Nya platser</span>
          </div>
          <div class="stat">
            <span class="stat-val gray">{{ preview.slipsSkipped }}</span>
            <span class="stat-label">Hoppar över</span>
          </div>
        </div>
        <ul v-if="preview.details.length" class="details">
          <li v-for="(d, i) in preview.details" :key="i">{{ d }}</li>
        </ul>
      </div>

      <!-- Importresultat -->
      <div v-if="result" class="result-box" :class="result.warnings.length ? 'warn' : 'ok'">
        <h4>Import klar</h4>
        <div class="stats">
          <div class="stat">
            <span class="stat-val green">{{ result.docksCreated }}</span>
            <span class="stat-label">Bryggor skapade</span>
          </div>
          <div class="stat">
            <span class="stat-val green">{{ result.slipsCreated }}</span>
            <span class="stat-label">Platser skapade</span>
          </div>
          <div class="stat">
            <span class="stat-val gray">{{ result.slipsSkipped }}</span>
            <span class="stat-label">Hoppade över</span>
          </div>
        </div>
        <ul v-if="result.warnings.length" class="details warn-list">
          <li v-for="(w, i) in result.warnings" :key="i">⚠ {{ w }}</li>
        </ul>
      </div>
    </section>
  </AppLayout>
</template>

<script setup>
import { ref } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { exportDocks, previewImport, importDocks } from '../api/importExport'

const jsonText = ref('')
const parseError = ref('')
const preview = ref(null)
const result = ref(null)
const exporting = ref(false)
const previewing = ref(false)
const importing = ref(false)

function parseJson() {
  parseError.value = ''
  try {
    const data = JSON.parse(jsonText.value)
    if (!Array.isArray(data)) throw new Error('JSON måste vara en array av bryggor')
    return data
  } catch (e) {
    parseError.value = 'Ogiltig JSON: ' + e.message
    return null
  }
}

async function doExport() {
  exporting.value = true
  try {
    const { data } = await exportDocks()
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'bryggor.json'
    a.click()
    URL.revokeObjectURL(url)
  } finally {
    exporting.value = false
  }
}

async function doPreview() {
  const data = parseJson()
  if (!data) return
  preview.value = null
  result.value = null
  previewing.value = true
  try {
    const { data: p } = await previewImport(data)
    preview.value = p
  } catch (e) {
    parseError.value = e.response?.data?.error || 'Fel vid förhandsgranskning'
  } finally {
    previewing.value = false
  }
}

async function doImport() {
  const data = parseJson()
  if (!data) return
  result.value = null
  importing.value = true
  try {
    const { data: r } = await importDocks(data)
    result.value = r
    preview.value = null
  } catch (e) {
    parseError.value = e.response?.data?.error || 'Fel vid import'
  } finally {
    importing.value = false
  }
}

function onFileSelected(e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => {
    jsonText.value = ev.target.result
    preview.value = null
    result.value = null
    parseError.value = ''
  }
  reader.readAsText(file)
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
textarea { width: 100%; padding: 0.75rem; border: 1px solid #ddd; border-radius: 6px; font-family: monospace; font-size: 0.82rem; resize: vertical; }
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
.result-box.ok { background: #f0fff4; border: 1px solid #b2dfdb; }
.result-box.warn { background: #fffbf0; border: 1px solid #ffe082; }
.stats { display: flex; gap: 1.5rem; margin-bottom: 0.75rem; flex-wrap: wrap; }
.stat { display: flex; flex-direction: column; align-items: center; }
.stat-val { font-size: 1.75rem; font-weight: 700; line-height: 1; }
.stat-val.green { color: #2e7d32; }
.stat-val.blue { color: #1565c0; }
.stat-val.gray { color: #757575; }
.stat-label { font-size: 0.75rem; color: #555; margin-top: 0.2rem; }
.details { list-style: none; padding: 0; font-size: 0.85rem; color: #444; }
.details li { padding: 0.2rem 0; border-bottom: 1px solid rgba(0,0,0,0.05); }
.warn-list li { color: #7c5700; }
</style>

<template>
  <AppLayout>
    <h2>Import / Export</h2>

    <section class="panel section">
      <h3>Exportera</h3>
      <p class="desc">
        Laddar ner hela hamnen som en JSON-fil med en sektion per område: personer,
        båtar och bryggor med sina platser.
      </p>
      <button class="btn btn-primary btn-sm" :disabled="exporting" @click="doExport">
        {{ exporting ? 'Exporterar…' : 'Ladda ner portplanner-export.json' }}
      </button>
    </section>

    <section class="panel section">
      <h3>Importera</h3>
      <p class="desc">
        Klistra in eller ladda upp en fil med samma sektioner. Befintliga poster
        uppdateras inte – bara nya läggs till. Personer läses in före båtar, så en
        båt hittar sin ägare i samma fil.
      </p>

      <div class="input-row">
        <label class="file-btn">
          Välj fil
          <input type="file" accept=".json,application/json" hidden @change="onFileSelected" />
        </label>
        <span class="or">eller klistra in JSON nedan</span>
      </div>

      <textarea class="input" v-model="jsonText" rows="12" :placeholder="PLACEHOLDER" />

      <div class="btn-row">
        <button class="btn btn-secondary btn-sm" :disabled="!jsonText.trim() || previewing" @click="doPreview">
          {{ previewing ? 'Kontrollerar…' : 'Förhandsgranska' }}
        </button>
        <button class="btn btn-primary btn-sm" :disabled="!preview || importing" @click="doImport">
          {{ importing ? 'Importerar…' : 'Importera' }}
        </button>
      </div>

      <p v-if="parseError" class="error">{{ parseError }}</p>

      <div v-if="previewSections.length" class="preview-box">
        <h4>Förhandsgranskning</h4>
        <div v-for="area in previewSections" :key="area.title" class="area">
          <p class="section-title">{{ area.title }}</p>
          <div class="stats">
            <div v-for="stat in area.stats" :key="stat.label" class="stat">
              <span class="stat-val num" :class="stat.tone">{{ stat.value }}</span>
              <span class="stat-label">{{ stat.label }}</span>
            </div>
          </div>
          <ul v-if="area.details.length" class="details">
            <li v-for="(d, i) in area.details" :key="i">{{ d }}</li>
          </ul>
        </div>
      </div>

      <div v-if="resultSections.length" class="result-box" :class="warnings.length ? 'warn' : 'ok'">
        <h4>{{ warnings.length ? 'Importen är klar, med anmärkningar' : 'Importen är klar' }}</h4>
        <div v-for="area in resultSections" :key="area.title" class="area">
          <p class="section-title">{{ area.title }}</p>
          <div class="stats">
            <div v-for="stat in area.stats" :key="stat.label" class="stat">
              <span class="stat-val num" :class="stat.tone">{{ stat.value }}</span>
              <span class="stat-label">{{ stat.label }}</span>
            </div>
          </div>
        </div>
        <ul v-if="warnings.length" class="details warn-list">
          <li v-for="(w, i) in warnings" :key="i">{{ w }}</li>
        </ul>
      </div>
    </section>
  </AppLayout>
</template>

<script setup>
import { computed, ref } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { exportAll, previewImport, importAll } from '../api/importExport'

const PLACEHOLDER = `{
  "persons": [
    { "firstName": "Anna", "lastName": "Svensson", "email": "anna@example.org" }
  ],
  "boats": [
    { "model": "Mästerkatten", "lengthM": 8.5, "widthM": 2.8, "ownerEmail": "anna@example.org" }
  ],
  "docks": [
    { "name": "Bryggans namn", "slips": [{ "slipNumber": "1", "maxWidthM": 3.2 }] }
  ]
}`

const jsonText = ref('')
const parseError = ref('')
const preview = ref(null)
const result = ref(null)
const exporting = ref(false)
const previewing = ref(false)
const importing = ref(false)

const stat = (label, value, tone) => ({ label, value: value ?? 0, tone })

const previewSections = computed(() => {
  const p = preview.value
  if (!p) return []
  return [
    { title: 'Personer',
      stats: [stat('Nya', p.persons?.personsNew, 'ok'), stat('Befintliga', p.persons?.personsExisting, 'info')],
      details: p.persons?.details ?? [] },
    { title: 'Båtar',
      stats: [stat('Nya', p.boats?.boatsNew, 'ok'), stat('Befintliga', p.boats?.boatsExisting, 'info'),
              stat('Ägare saknas', p.boats?.ownersNotFound, 'idle')],
      details: p.boats?.details ?? [] },
    { title: 'Bryggor',
      stats: [stat('Nya bryggor', p.docks?.docksNew, 'ok'), stat('Befintliga', p.docks?.docksExisting, 'info'),
              stat('Nya platser', p.docks?.slipsNew, 'ok'), stat('Hoppar över', p.docks?.slipsSkipped, 'idle')],
      details: p.docks?.details ?? [] },
  ]
})

const resultSections = computed(() => {
  const r = result.value
  if (!r) return []
  return [
    { title: 'Personer',
      stats: [stat('Skapade', r.persons?.personsCreated, 'ok'), stat('Hoppade över', r.persons?.personsSkipped, 'idle')] },
    { title: 'Båtar',
      stats: [stat('Skapade', r.boats?.boatsCreated, 'ok'), stat('Hoppade över', r.boats?.boatsSkipped, 'idle')] },
    { title: 'Bryggor',
      stats: [stat('Skapade bryggor', r.docks?.docksCreated, 'ok'), stat('Skapade platser', r.docks?.slipsCreated, 'ok'),
              stat('Hoppade över', r.docks?.slipsSkipped, 'idle')] },
  ]
})

const warnings = computed(() => {
  const r = result.value
  if (!r) return []
  return [...(r.persons?.warnings ?? []), ...(r.boats?.warnings ?? []), ...(r.docks?.warnings ?? [])]
})

/** The file is one object with a section per area, not a bare list. */
function parseFile() {
  parseError.value = ''
  try {
    const data = JSON.parse(jsonText.value)
    if (Array.isArray(data)) {
      parseError.value = 'Filen ska ha en sektion per område: persons, boats och docks.'
      return null
    }
    if (!data || typeof data !== 'object') throw new Error('JSON måste vara ett objekt')
    return data
  } catch (e) {
    parseError.value = 'Ogiltig JSON: ' + e.message
    return null
  }
}

function onFileSelected(event) {
  const file = event.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    jsonText.value = String(reader.result)
    preview.value = null
    result.value = null
  }
  reader.readAsText(file)
  event.target.value = ''
}

function downloadJson(data, filename) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function doExport() {
  exporting.value = true
  try {
    const { data } = await exportAll()
    downloadJson(data, 'portplanner-export.json')
  } finally {
    exporting.value = false
  }
}

async function doPreview() {
  const file = parseFile()
  if (!file) return
  preview.value = null
  result.value = null
  previewing.value = true
  try {
    const { data } = await previewImport(file)
    preview.value = data
  } catch (e) {
    parseError.value = e.response?.data?.error || 'Fel vid förhandsgranskning'
  } finally {
    previewing.value = false
  }
}

async function doImport() {
  const file = parseFile()
  if (!file) return
  result.value = null
  importing.value = true
  try {
    const { data } = await importAll(file)
    result.value = data
    preview.value = null
  } catch (e) {
    parseError.value = e.response?.data?.error || 'Fel vid import'
  } finally {
    importing.value = false
  }
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

.area + .area { margin-top: 18px; border-top: 1px solid var(--paper-border); padding-top: 14px; }
.stats { display: flex; gap: 22px; margin-bottom: 10px; flex-wrap: wrap; }
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
.warn-list { margin-top: 14px; }
.warn-list li { color: var(--warn-text); }
</style>

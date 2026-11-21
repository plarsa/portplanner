<template>
  <AppLayout>
    <div class="toolbar">
      <h2>Händelselogg</h2>
      <div class="right">
        <div class="field">
          <label for="filterType">Typ</label>
          <select id="filterType" class="input" v-model="filterType" @change="load">
            <option value="">Alla typer</option>
            <option value="PERSON">Person</option>
            <option value="BOAT">Båt</option>
            <option value="DOCK">Brygga</option>
            <option value="SLIP">Plats</option>
            <option value="ASSIGNMENT">Tilldelning</option>
            <option value="QUEUE_ENTRY">Kö</option>
          </select>
        </div>
        <button class="btn btn-secondary btn-sm" :disabled="loading" @click="load">
          {{ loading ? 'Laddar…' : 'Uppdatera' }}
        </button>
      </div>
    </div>

    <p v-if="!entries.length && !loading" class="empty">Inga händelser att visa.</p>

    <div v-if="entries.length" class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Tidpunkt</th>
            <th>Händelse</th>
            <th>Typ</th>
            <th>Beskrivning</th>
            <th>Utförd av</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="e in entries" :key="e.id">
            <td class="num nowrap">{{ fmt(e.occurredAt) }}</td>
            <td><span class="pill" :class="pillClass(e.eventType)">{{ eventLabel(e.eventType) }}</span></td>
            <td class="muted nowrap">{{ entityLabel(e.entityType) }}</td>
            <td>{{ e.description }}</td>
            <td class="muted nowrap">{{ e.performedBy }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { getAuditLog } from '../api/audit'

const entries = ref([])
const filterType = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const { data } = await getAuditLog(filterType.value || undefined)
    entries.value = data
  } finally {
    loading.value = false
  }
}

function fmt(iso) {
  if (!iso) return '–'
  const d = new Date(iso)
  return d.toLocaleString('sv-SE', { dateStyle: 'short', timeStyle: 'medium' })
}

function eventLabel(t) {
  return {
    CREATED:        'Skapad',
    UPDATED:        'Uppdaterad',
    DELETED:        'Borttagen',
    ASSIGNED:       'Tilldelad',
    UNASSIGNED:     'Frigjord',
    QUEUED:         'Köad',
    DEQUEUED:       'Avköad',
    QUEUE_ASSIGNED: 'Köad → Plats',
    IMPORTED:       'Importerad',
  }[t] ?? t
}

function entityLabel(t) {
  return {
    PERSON:      'Person',
    BOAT:        'Båt',
    DOCK:        'Brygga',
    SLIP:        'Plats',
    ASSIGNMENT:  'Tilldelning',
    QUEUE_ENTRY: 'Kö',
  }[t] ?? (t ?? '–')
}

/*
 * Nine event types onto the five roles the guide defines: green is done, red is
 * gone, yellow is a step back, grey is neutral. Nine colours in a long log would
 * be noise, which is exactly what the guide warns about.
 */
function pillClass(t) {
  return {
    CREATED:        'pill-ok',
    UPDATED:        'pill-info',
    DELETED:        'pill-bad',
    ASSIGNED:       'pill-ok',
    UNASSIGNED:     'pill-warn',
    QUEUED:         'pill-info',
    DEQUEUED:       'pill-idle',
    QUEUE_ASSIGNED: 'pill-ok',
    IMPORTED:       'pill-info',
  }[t] ?? 'pill-idle'
}

onMounted(load)
</script>

<style scoped>
.nowrap { white-space: nowrap; }
.toolbar .field { min-width: 180px; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>

<template>
  <AppLayout>
    <div class="page-header">
      <h2>Händelselogg</h2>
      <div class="header-actions">
        <select v-model="filterType" @change="load">
          <option value="">Alla typer</option>
          <option value="PERSON">Person</option>
          <option value="BOAT">Båt</option>
          <option value="DOCK">Brygga</option>
          <option value="SLIP">Plats</option>
          <option value="ASSIGNMENT">Tilldelning</option>
          <option value="QUEUE_ENTRY">Kö</option>
        </select>
        <button class="btn-secondary" :disabled="loading" @click="load">
          {{ loading ? 'Laddar…' : '↺ Uppdatera' }}
        </button>
      </div>
    </div>

    <div class="card">
      <p v-if="!entries.length && !loading" class="empty">Inga händelser att visa.</p>

      <table v-if="entries.length">
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
            <td class="time">{{ fmt(e.occurredAt) }}</td>
            <td><span :class="['badge', badgeClass(e.eventType)]">{{ eventLabel(e.eventType) }}</span></td>
            <td class="entity">{{ entityLabel(e.entityType) }}</td>
            <td class="desc">{{ e.description }}</td>
            <td class="user">{{ e.performedBy }}</td>
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

function badgeClass(t) {
  return {
    CREATED:        'green',
    UPDATED:        'blue',
    DELETED:        'red',
    ASSIGNED:       'teal',
    UNASSIGNED:     'orange',
    QUEUED:         'yellow',
    DEQUEUED:       'gray',
    QUEUE_ASSIGNED: 'teal',
    IMPORTED:       'indigo',
  }[t] ?? 'gray'
}

onMounted(load)
</script>

<style scoped>
h2 { font-size: 1.5rem; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; }
.header-actions { display: flex; gap: 0.75rem; align-items: center; }
select { padding: 0.4rem 0.75rem; border: 1px solid #ddd; border-radius: 6px; font-size: 0.9rem; }
.btn-secondary { background: #eee; color: #333; border: none; padding: 0.45rem 1rem; border-radius: 6px; cursor: pointer; font-size: 0.9rem; }
.btn-secondary:hover:not(:disabled) { background: #ddd; }
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }
.card { background: white; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.07); overflow: hidden; }
.empty { color: #999; padding: 2rem; text-align: center; }
table { width: 100%; border-collapse: collapse; }
th { background: #f8f8f8; padding: 0.6rem 1rem; text-align: left; font-size: 0.82rem; color: #555; white-space: nowrap; }
td { padding: 0.55rem 1rem; border-top: 1px solid #f0f0f0; font-size: 0.88rem; vertical-align: middle; }
td.time { white-space: nowrap; color: #666; font-size: 0.82rem; }
td.entity { color: #555; white-space: nowrap; }
td.desc { color: #222; }
td.user { color: #888; font-size: 0.82rem; white-space: nowrap; }
.badge { display: inline-block; padding: 0.18rem 0.55rem; border-radius: 20px; font-size: 0.75rem; font-weight: 600; white-space: nowrap; }
.badge.green  { background: #d4edda; color: #155724; }
.badge.blue   { background: #d0e2ff; color: #1565c0; }
.badge.red    { background: #f8d7da; color: #721c24; }
.badge.teal   { background: #d0f0f0; color: #00695c; }
.badge.orange { background: #ffe0b2; color: #e65100; }
.badge.yellow { background: #fff9c4; color: #7c5700; }
.badge.gray   { background: #f0f0f0; color: #555; }
.badge.indigo { background: #e8eaf6; color: #283593; }
</style>

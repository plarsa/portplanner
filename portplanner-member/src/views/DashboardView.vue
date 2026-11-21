<template>
  <div>
    <p v-if="modules.notice" class="notice" @click="modules.notice = ''">{{ modules.notice }}</p>
    <h2>Välkommen, {{ auth.username }}</h2>

    <p v-if="loading" class="empty">Laddar...</p>

    <div v-else class="columns">
      <div class="column">
        <span class="column-title">Min plats</span>
        <span v-if="slip" class="column-value num">{{ slip.dockName }} {{ slip.slipNumber }}</span>
        <span v-else class="column-value muted">Ingen tilldelad plats</span>
        <router-link to="/slip" class="column-link">Visa mer</router-link>
      </div>

      <div class="column">
        <span class="column-title">Mina båtar</span>
        <span class="column-value num">{{ boats.length }}</span>
        <router-link to="/boats" class="column-link">Visa mer</router-link>
      </div>

      <div class="column">
        <span class="column-title">Köstatus</span>
        <span v-if="queueEntry" class="column-value">{{ statusLabel(queueEntry.status) }}</span>
        <span v-else class="column-value muted">Inte i kö</span>
        <span v-if="hasOffer" class="pill pill-warn">Erbjudande väntar</span>
        <router-link to="/slip" class="column-link">{{ hasOffer ? 'Svara på erbjudande' : 'Visa mer' }}</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useModulesStore } from '../stores/modules'
import api from '../api/axios'

const auth = useAuthStore()
const modules = useModulesStore()
const loading = ref(true)
const me = ref(null)
const assignments = ref([])
const queue = ref([])
const boats = ref([])

const slip = computed(() => assignments.value[0] ?? null)
const queueEntry = computed(() => queue.value[0] ?? null)
const hasOffer = computed(() => queueEntry.value?.status === 'OFFERED')

function statusLabel(s) {
  return { WAITING: 'Väntar', OFFERED: 'Erbjudande!', ASSIGNED: 'Tilldelad' }[s] ?? s
}

onMounted(async () => {
  try {
    const [mRes, aRes, qRes, bRes] = await Promise.all([
      api.get('/me'),
      api.get('/me/assignments'),
      api.get('/me/queue'),
      api.get('/me/boats'),
    ])
    me.value = mRes.data
    assignments.value = aRes.data
    queue.value = qRes.data
    boats.value = bRes.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
h2 { font-size: 1.45rem; margin: 0 0 24px; }
.notice {
  border-left: 2px solid var(--warn-border);
  background: var(--warn-bg);
  color: var(--warn-text);
  padding: 10px 14px;
  margin: 0 0 22px;
  font-size: 0.9rem;
  cursor: pointer;
}
.column-value.muted { font-size: 1rem; font-weight: 400; color: var(--text-muted); }
.column-link { font-size: 0.88rem; margin-top: auto; }
.pill { align-self: flex-start; }
</style>

<template>
  <div>
    <h2>Mina båtar</h2>
    <p v-if="loading" class="empty">Laddar...</p>
    <p v-else-if="boats.length === 0" class="empty">Inga registrerade båtar.</p>
    <div v-else class="panel">
      <div class="rows">
        <div v-for="boat in boats" :key="boat.id" class="row boat">
          <span class="boat-name">{{ boat.model }}</span>
          <span class="specs">
            <span v-if="boat.lengthM" class="num">{{ boat.lengthM }} m</span>
            <span v-if="boat.widthM" class="num">{{ boat.widthM }} m</span>
            <span v-if="boat.draftM" class="num">{{ boat.draftM }} m</span>
            <span v-if="boat.regNumber" class="num">{{ boat.regNumber }}</span>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/axios'

const loading = ref(true)
const boats = ref([])

onMounted(async () => {
  try {
    const { data } = await api.get('/me/boats')
    boats.value = data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
h2 { font-size: 1.45rem; margin: 0 0 24px; }
.boat-name { font-weight: 600; color: var(--ink); }
.specs { display: flex; gap: 22px; flex-wrap: wrap; color: var(--text-muted); }
@media (max-width: 620px) {
  .row.boat { flex-direction: column; gap: 6px; }
}
</style>

<template>
  <div>
    <h2>Mina båtar</h2>
    <div v-if="loading" class="loading">Laddar...</div>
    <div v-else-if="boats.length === 0" class="empty">Inga registrerade båtar.</div>
    <div v-else class="boat-list">
      <div v-for="boat in boats" :key="boat.id" class="card">
        <div class="boat-name">{{ boat.model }}</div>
        <div class="specs">
          <span v-if="boat.lengthM">Längd: {{ boat.lengthM }} m</span>
          <span v-if="boat.widthM">Bredd: {{ boat.widthM }} m</span>
          <span v-if="boat.draftM">Djupgång: {{ boat.draftM }} m</span>
          <span v-if="boat.regNumber">Reg: {{ boat.regNumber }}</span>
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
h2 { margin: 0 0 1.5rem; }
.loading, .empty { color: #888; }
.boat-list { display: flex; flex-direction: column; gap: 1rem; }
.card {
  background: white;
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.07);
}
.boat-name { font-size: 1.1rem; font-weight: 700; color: #1a3a5c; margin-bottom: 0.5rem; }
.specs { display: flex; gap: 1.25rem; flex-wrap: wrap; font-size: 0.875rem; color: #555; }
</style>

<template>
  <AppLayout>
    <h2>Dashboard</h2>
    <p style="color:#666; margin-top:0.5rem">Välkommen, {{ auth.username }}!</p>
    <div class="cards">
      <div class="card">
        <div class="card-title">Lediga platser</div>
        <div class="card-value">{{ stats.available ?? '–' }}</div>
        <div class="card-sub">av {{ stats.totalSlips ?? '–' }} totalt</div>
      </div>
      <div class="card">
        <div class="card-title">Tilldelade platser</div>
        <div class="card-value">{{ stats.occupied ?? '–' }}</div>
      </div>
      <div class="card">
        <div class="card-title">Kölängd</div>
        <div class="card-value">{{ stats.queueLength ?? '–' }}</div>
      </div>
      <div class="card">
        <div class="card-title">Bryggor</div>
        <div class="card-value">{{ stats.docks ?? '–' }}</div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { useAuthStore } from '../stores/auth'
import { getSlips } from '../api/slips'
import { getDocks } from '../api/docks'
import { getQueue } from '../api/queue'

const auth = useAuthStore()
const stats = ref({})

onMounted(async () => {
  const [{ data: slips }, { data: docks }, { data: queue }] = await Promise.all([
    getSlips(), getDocks(), getQueue()
  ])
  stats.value = {
    totalSlips: slips.length,
    available: slips.filter(s => s.status === 'AVAILABLE').length,
    occupied: slips.filter(s => s.status === 'OCCUPIED').length,
    docks: docks.length,
    queueLength: queue.length,
  }
})
</script>

<style scoped>
h2 { font-size: 1.5rem; }
.cards { display: flex; gap: 1.5rem; margin-top: 2rem; flex-wrap: wrap; }
.card {
  background: white; border-radius: 10px;
  padding: 1.5rem 2rem; min-width: 160px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.07);
}
.card-title { font-size: 0.85rem; color: #666; margin-bottom: 0.5rem; }
.card-value { font-size: 2rem; font-weight: 700; color: #1a3a5c; }
.card-sub { font-size: 0.8rem; color: #aaa; margin-top: 0.25rem; }
</style>

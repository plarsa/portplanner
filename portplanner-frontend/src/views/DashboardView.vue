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
      <div class="card card-money">
        <div class="card-title">Intäkt tilldelade platser</div>
        <div class="card-value">{{ formatKr(stats.occupiedRevenue) }}</div>
        <div class="card-sub">kr / år</div>
      </div>
      <div class="card card-money">
        <div class="card-title">Maximal intäkt (fullt uthyrt)</div>
        <div class="card-value">{{ formatKr(stats.totalRevenue) }}</div>
        <div class="card-sub">kr / år</div>
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
import { getTariffs } from '../api/tariffs'

const auth = useAuthStore()
const stats = ref({})

function formatKr(val) {
  if (val == null) return '–'
  return new Intl.NumberFormat('sv-SE', { maximumFractionDigits: 0 }).format(val)
}

onMounted(async () => {
  const [{ data: slips }, { data: docks }, { data: queue }, { data: tariffs }] = await Promise.all([
    getSlips(), getDocks(), getQueue(), getTariffs()
  ])

  const today = new Date().toISOString().slice(0, 10)
  const activeTariffs = tariffs.filter(t =>
    t.validFrom <= today && (!t.validTo || t.validTo >= today)
  )
  const feeByCategory = {}
  for (const t of activeTariffs) {
    if (!(t.category in feeByCategory)) feeByCategory[t.category] = 0
    feeByCategory[t.category] += Number(t.annualFeeKr)
  }

  let occupiedRevenue = 0
  let totalRevenue = 0
  for (const s of slips) {
    const fee = s.category ? (feeByCategory[s.category] ?? 0) : 0
    totalRevenue += fee
    if (s.status === 'OCCUPIED') occupiedRevenue += fee
  }

  stats.value = {
    totalSlips: slips.length,
    available: slips.filter(s => s.status === 'AVAILABLE').length,
    occupied: slips.filter(s => s.status === 'OCCUPIED').length,
    docks: docks.length,
    queueLength: queue.length,
    occupiedRevenue,
    totalRevenue,
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
.card-money { border-left: 4px solid #1a3a5c; }
.card-title { font-size: 0.85rem; color: #666; margin-bottom: 0.5rem; }
.card-value { font-size: 2rem; font-weight: 700; color: #1a3a5c; }
.card-sub { font-size: 0.8rem; color: #aaa; margin-top: 0.25rem; }
</style>

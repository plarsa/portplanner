<template>
  <AppLayout>
    <div class="page-header">
      <h2>Taxelista</h2>
      <button class="btn-primary" @click="openCreate">+ Ny taxa</button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Kategori</th>
            <th>Namn</th>
            <th>Årsavgift</th>
            <th>Gäller från</th>
            <th>Gäller till</th>
            <th>Beskrivning</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in tariffs" :key="t.id" :class="{ active: isActive(t) }">
            <td><span class="category-badge">{{ t.category }}</span></td>
            <td>{{ t.name }}</td>
            <td class="fee">{{ formatFee(t.annualFeeKr) }}</td>
            <td>{{ formatDate(t.validFrom) }}</td>
            <td>{{ t.validTo ? formatDate(t.validTo) : '–' }}</td>
            <td class="desc-cell">{{ t.description || '–' }}</td>
            <td class="actions">
              <button @click="openEdit(t)">Redigera</button>
              <button class="danger" @click="remove(t)">Ta bort</button>
            </td>
          </tr>
          <tr v-if="!tariffs.length">
            <td colspan="7" class="empty">Inga taxor registrerade</td>
          </tr>
        </tbody>
      </table>
    </div>

    <BaseModal v-if="modal" :title="editing ? 'Redigera taxa' : 'Ny taxa'"
               @close="modal = false" @save="save">
      <div class="form-grid">
        <label>Kategori *
          <input v-model="form.category" placeholder="t.ex. A, B, C" required />
        </label>
        <label>Namn *
          <input v-model="form.name" placeholder="t.ex. Kategori A – bred plats" required />
        </label>
        <label>Årsavgift (kr) *
          <input v-model.number="form.annualFeeKr" type="number" min="0" step="100" required />
        </label>
        <label>Gäller från *
          <input v-model="form.validFrom" type="date" required />
        </label>
        <label>Gäller till
          <input v-model="form.validTo" type="date" />
        </label>
        <label class="full">Beskrivning
          <input v-model="form.description" placeholder="Valfri beskrivning…" />
        </label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import BaseModal from '../components/BaseModal.vue'
import { getTariffs, createTariff, updateTariff, deleteTariff } from '../api/tariffs'

const tariffs = ref([])
const modal = ref(false)
const editing = ref(null)
const err = ref('')
const today = new Date().toISOString().slice(0, 10)

const emptyForm = () => ({
  category: '', name: '', annualFeeKr: '', validFrom: today, validTo: '', description: '',
})
const form = ref(emptyForm())

function formatDate(d) {
  return d ? new Date(d + 'T00:00:00').toLocaleDateString('sv-SE') : ''
}

function formatFee(fee) {
  return fee != null
    ? new Intl.NumberFormat('sv-SE', { style: 'currency', currency: 'SEK', maximumFractionDigits: 0 }).format(fee)
    : '–'
}

function isActive(t) {
  if (!t.validFrom) return false
  const from = new Date(t.validFrom)
  const to = t.validTo ? new Date(t.validTo) : null
  const now = new Date()
  return from <= now && (to === null || to >= now)
}

async function load() {
  const { data } = await getTariffs()
  tariffs.value = data
}

function openCreate() {
  editing.value = null
  form.value = emptyForm()
  err.value = ''
  modal.value = true
}

function openEdit(t) {
  editing.value = t
  form.value = {
    category: t.category,
    name: t.name,
    annualFeeKr: t.annualFeeKr,
    validFrom: t.validFrom,
    validTo: t.validTo || '',
    description: t.description || '',
  }
  err.value = ''
  modal.value = true
}

async function save() {
  err.value = ''
  try {
    const payload = { ...form.value, validTo: form.value.validTo || null }
    if (editing.value) await updateTariff(editing.value.id, payload)
    else await createTariff(payload)
    modal.value = false
    await load()
  } catch (e) {
    err.value = e.response?.data?.error || 'Något gick fel'
  }
}

async function remove(t) {
  if (!confirm(`Ta bort taxa "${t.name}" (${t.category})?`)) return
  try {
    await deleteTariff(t.id)
    await load()
  } catch (e) {
    alert(e.response?.data?.error || 'Kunde inte ta bort taxan')
  }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; }
h2 { font-size: 1.5rem; }
.table-wrap { background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.07); }
table { width: 100%; border-collapse: collapse; }
th { background: #f8f8f8; padding: 0.75rem 1rem; text-align: left; font-size: 0.85rem; color: #555; border-bottom: 1px solid #eee; }
td { padding: 0.75rem 1rem; border-bottom: 1px solid #f0f0f0; font-size: 0.9rem; }
tr.active td { background: #f0faf4; }
.category-badge { background: #1a3a5c; color: white; border-radius: 4px; padding: 0.15rem 0.55rem; font-size: 0.8rem; font-weight: 700; }
.fee { font-weight: 600; color: #1a3a5c; }
.desc-cell { color: #666; font-size: 0.85rem; max-width: 200px; }
.actions { display: flex; gap: 0.5rem; }
.empty { color: #999; text-align: center; padding: 2rem; }
button { padding: 0.35rem 0.8rem; border-radius: 5px; border: none; cursor: pointer; font-size: 0.85rem; background: #e8f0fe; color: #1a3a5c; }
button:hover { background: #d0e2ff; }
button.danger { background: #fee; color: #c0392b; }
button.danger:hover { background: #fcc; }
.btn-primary { background: #1a3a5c; color: white; padding: 0.5rem 1.1rem; border-radius: 6px; border: none; cursor: pointer; }
.btn-primary:hover { background: #234e7a; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.form-grid .full { grid-column: 1 / -1; }
label { display: flex; flex-direction: column; font-size: 0.85rem; font-weight: 600; gap: 0.3rem; }
label input { padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.95rem; font-weight: 400; }
.error { color: #c0392b; font-size: 0.85rem; margin-top: 0.5rem; }
</style>

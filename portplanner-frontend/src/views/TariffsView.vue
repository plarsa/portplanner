<template>
  <AppLayout>
    <div class="toolbar">
      <h2>Taxelista</h2>
      <div class="right">
        <button class="btn btn-primary btn-sm" @click="openCreate">Ny taxa</button>
      </div>
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
          <tr v-for="t in tariffs" :key="t.id" :class="{ current: isActive(t) }">
            <td class="num">{{ t.category }}</td>
            <td>{{ t.name }}</td>
            <td class="num fee">{{ formatFee(t.annualFeeKr) }}</td>
            <td class="num">{{ formatDate(t.validFrom) }}</td>
            <td class="num muted">{{ t.validTo ? formatDate(t.validTo) : '–' }}</td>
            <td class="desc-cell muted">{{ t.description || '–' }}</td>
            <td>
              <div class="actions">
                <button class="btn btn-secondary btn-sm" @click="openEdit(t)">Redigera</button>
                <button class="btn btn-danger btn-sm" @click="remove(t)">Ta bort</button>
              </div>
            </td>
          </tr>
          <tr v-if="!tariffs.length">
            <td colspan="7" class="empty-cell">Inga taxor registrerade</td>
          </tr>
        </tbody>
      </table>
    </div>

    <BaseModal v-if="modal" :title="editing ? 'Redigera taxa' : 'Ny taxa'"
               @close="modal = false" @save="save">
      <div class="form-grid">
        <div class="field">
          <label for="category">Kategori *</label>
          <input id="category" class="input num" v-model="form.category" placeholder="t.ex. A, B, C" required />
        </div>
        <div class="field">
          <label for="name">Namn *</label>
          <input id="name" class="input" v-model="form.name" placeholder="t.ex. Kategori A – bred plats" required />
        </div>
        <div class="field">
          <label for="fee">Årsavgift (kr) *</label>
          <input id="fee" class="input num" v-model.number="form.annualFeeKr" type="number" min="0" step="100" required />
        </div>
        <div class="field">
          <label for="validFrom">Gäller från *</label>
          <input id="validFrom" class="input num" v-model="form.validFrom" type="date" required />
        </div>
        <div class="field">
          <label for="validTo">Gäller till</label>
          <input id="validTo" class="input num" v-model="form.validTo" type="date" />
        </div>
        <div class="field full">
          <label for="description">Beskrivning</label>
          <input id="description" class="input" v-model="form.description" placeholder="Valfri beskrivning…" />
        </div>
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
/* A tariff that applies today is marked as such: green means current. */
tbody tr.current td { background: var(--ok-bg); }
.fee { font-weight: 500; color: var(--ink); }
.desc-cell { max-width: 220px; font-size: 0.88rem; }
.actions { display: flex; gap: 10px; }
.empty-cell { text-align: center; padding: 44px; color: var(--text-muted); }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 14px; }
.form-grid .full { grid-column: 1 / -1; }
@media (max-width: 620px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>

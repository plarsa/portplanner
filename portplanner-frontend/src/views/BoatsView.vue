<template>
  <AppLayout>
    <div class="page-header">
      <h2>Båtar</h2>
      <div class="header-right">
        <input v-model="search" class="search-input" placeholder="Sök båt eller ägare…" />
        <button class="btn-primary" @click="openCreate">+ Ny båt</button>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Namn</th><th>Registrering</th><th>Ägare</th>
            <th>L (m)</th><th>B (m)</th><th>Djup (m)</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in filteredBoats" :key="b.id">
            <td>{{ b.name }}</td>
            <td>{{ b.registrationNumber || '–' }}</td>
            <td>{{ b.ownerName }}</td>
            <td>{{ b.lengthM }}</td>
            <td>{{ b.widthM }}</td>
            <td>{{ b.draftM || '–' }}</td>
            <td class="actions">
              <button @click="openEdit(b)">Redigera</button>
              <button class="danger" @click="remove(b)">Ta bort</button>
            </td>
          </tr>
          <tr v-if="!filteredBoats.length">
            <td colspan="7" class="empty">{{ search ? 'Inga träffar' : 'Inga båtar registrerade' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <BaseModal v-if="modal" :title="editing ? 'Redigera båt' : 'Ny båt'"
               @close="modal = false" @save="save">
      <div class="form-grid">
        <label class="full">Båtnamn *<input v-model="form.name" required /></label>
        <label class="full">Registreringsnummer<input v-model="form.registrationNumber" /></label>
        <label>Längd m *<input v-model.number="form.lengthM" type="number" step="0.1" min="0" required /></label>
        <label>Bredd m *<input v-model.number="form.widthM" type="number" step="0.1" min="0" required /></label>
        <label class="full">Djupgång m<input v-model.number="form.draftM" type="number" step="0.1" min="0" /></label>
        <label class="full">Ägare *
          <select v-model.number="form.ownerId" required>
            <option value="">Välj ägare…</option>
            <option v-for="p in persons" :key="p.id" :value="p.id">
              {{ p.firstName }} {{ p.lastName }}
            </option>
          </select>
        </label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import BaseModal from '../components/BaseModal.vue'
import { getBoats, createBoat, updateBoat, deleteBoat } from '../api/boats'
import { getPersons } from '../api/persons'

const boats = ref([])
const persons = ref([])
const search = ref('')
const modal = ref(false)

const filteredBoats = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return boats.value
  return boats.value.filter(b =>
    b.name.toLowerCase().includes(q) ||
    (b.ownerName ?? '').toLowerCase().includes(q) ||
    (b.registrationNumber ?? '').toLowerCase().includes(q)
  )
})
const editing = ref(null)
const err = ref('')
const emptyForm = () => ({ name: '', registrationNumber: '', lengthM: '', widthM: '', draftM: '', ownerId: '' })
const form = ref(emptyForm())

async function load() {
  const [b, p] = await Promise.all([getBoats(), getPersons()])
  boats.value = b.data
  persons.value = p.data
}

function openCreate() { editing.value = null; form.value = emptyForm(); err.value = ''; modal.value = true }
function openEdit(b) {
  editing.value = b
  form.value = { name: b.name, registrationNumber: b.registrationNumber, lengthM: b.lengthM, widthM: b.widthM, draftM: b.draftM, ownerId: b.ownerId }
  err.value = ''
  modal.value = true
}

async function save() {
  err.value = ''
  try {
    const payload = { ...form.value, draftM: form.value.draftM || null }
    if (editing.value) await updateBoat(editing.value.id, payload)
    else await createBoat(payload)
    modal.value = false
    await load()
  } catch (e) {
    err.value = e.response?.data?.error || 'Något gick fel'
  }
}

async function remove(b) {
  if (!confirm(`Ta bort ${b.name}?`)) return
  await deleteBoat(b.id)
  await load()
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; }
h2 { font-size: 1.5rem; }
.header-right { display: flex; gap: 0.75rem; align-items: center; }
.search-input { padding: 0.45rem 0.85rem; border: 1px solid #ddd; border-radius: 6px; font-size: 0.9rem; width: 220px; }
.search-input:focus { outline: none; border-color: #1a3a5c; }
.table-wrap { background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.07); }
table { width: 100%; border-collapse: collapse; }
th { background: #f8f8f8; padding: 0.75rem 1rem; text-align: left; font-size: 0.85rem; color: #555; border-bottom: 1px solid #eee; }
td { padding: 0.75rem 1rem; border-bottom: 1px solid #f0f0f0; font-size: 0.9rem; }
.actions { display: flex; gap: 0.5rem; }
.empty { color: #999; text-align: center; padding: 2rem; }
button { padding: 0.35rem 0.8rem; border-radius: 5px; border: none; cursor: pointer; font-size: 0.85rem; background: #e8f0fe; color: #1a3a5c; }
button:hover { background: #d0e2ff; }
button.danger { background: #fee; color: #c0392b; }
.btn-primary { background: #1a3a5c; color: white; padding: 0.5rem 1.1rem; border-radius: 6px; border: none; cursor: pointer; }
.btn-primary:hover { background: #234e7a; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.form-grid .full { grid-column: 1 / -1; }
label { display: flex; flex-direction: column; font-size: 0.85rem; font-weight: 600; gap: 0.3rem; }
label input, label select { padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.95rem; font-weight: 400; }
.error { color: #c0392b; font-size: 0.85rem; margin-top: 0.5rem; }
</style>

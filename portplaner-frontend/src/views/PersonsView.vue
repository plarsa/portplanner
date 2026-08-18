<template>
  <AppLayout>
    <div class="page-header">
      <h2>Personer</h2>
      <button class="btn-primary" @click="openCreate">+ Ny person</button>
    </div>

    <div class="search-bar">
      <input v-model="search" placeholder="Sök namn eller e-post…" @input="load" />
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Namn</th><th>E-post</th><th>Telefon</th><th>Båtar</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in persons" :key="p.id">
            <td>{{ p.firstName }} {{ p.lastName }}</td>
            <td>{{ p.email }}</td>
            <td>{{ p.phone || '–' }}</td>
            <td>{{ p.boatCount }}</td>
            <td class="actions">
              <button @click="openEdit(p)">Redigera</button>
              <button class="danger" @click="remove(p)">Ta bort</button>
            </td>
          </tr>
          <tr v-if="!persons.length">
            <td colspan="5" class="empty">Inga personer hittades</td>
          </tr>
        </tbody>
      </table>
    </div>

    <BaseModal v-if="modal" :title="editing ? 'Redigera person' : 'Ny person'"
               @close="modal = false" @save="save">
      <div class="form-grid">
        <label>Förnamn *<input v-model="form.firstName" required /></label>
        <label>Efternamn *<input v-model="form.lastName" required /></label>
        <label class="full">E-post *<input v-model="form.email" type="email" required /></label>
        <label class="full">Telefon<input v-model="form.phone" /></label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import BaseModal from '../components/BaseModal.vue'
import { getPersons, createPerson, updatePerson, deletePerson } from '../api/persons'

const persons = ref([])
const search = ref('')
const modal = ref(false)
const editing = ref(null)
const err = ref('')
const emptyForm = () => ({ firstName: '', lastName: '', email: '', phone: '' })
const form = ref(emptyForm())

async function load() {
  const { data } = await getPersons(search.value)
  persons.value = data
}

function openCreate() { editing.value = null; form.value = emptyForm(); err.value = ''; modal.value = true }
function openEdit(p) { editing.value = p; form.value = { ...p }; err.value = ''; modal.value = true }

async function save() {
  err.value = ''
  try {
    if (editing.value) await updatePerson(editing.value.id, form.value)
    else await createPerson(form.value)
    modal.value = false
    await load()
  } catch (e) {
    err.value = e.response?.data?.error || 'Något gick fel'
  }
}

async function remove(p) {
  if (!confirm(`Ta bort ${p.firstName} ${p.lastName}?`)) return
  await deletePerson(p.id)
  await load()
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; }
h2 { font-size: 1.5rem; }
.search-bar { margin-bottom: 1rem; }
.search-bar input { padding: 0.5rem 0.8rem; border: 1px solid #ddd; border-radius: 6px; width: 280px; }
.table-wrap { background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.07); }
table { width: 100%; border-collapse: collapse; }
th { background: #f8f8f8; padding: 0.75rem 1rem; text-align: left; font-size: 0.85rem; color: #555; border-bottom: 1px solid #eee; }
td { padding: 0.75rem 1rem; border-bottom: 1px solid #f0f0f0; font-size: 0.9rem; }
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

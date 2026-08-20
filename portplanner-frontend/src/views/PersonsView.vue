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
            <th>Namn</th><th>E-post</th><th>Telefon</th><th>Adress</th><th>Båtar</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in persons" :key="p.id" class="clickable-row" @click="openDetail(p)">
            <td>{{ p.firstName }} {{ p.lastName }}</td>
            <td>{{ p.email }}</td>
            <td>{{ p.phone || '–' }}</td>
            <td class="address-cell">
              <span v-if="p.address">{{ p.address }}<span v-if="p.postalCode">, {{ p.postalCode }}</span></span>
              <span v-else class="muted">–</span>
            </td>
            <td>{{ p.boatCount }}</td>
            <td class="actions" @click.stop>
              <button @click="openEdit(p)">Redigera</button>
              <button class="danger" @click="remove(p)">Ta bort</button>
            </td>
          </tr>
          <tr v-if="!persons.length">
            <td colspan="6" class="empty">Inga personer hittades</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Detaljvy -->
    <div v-if="detail" class="detail-overlay" @click.self="detail = null">
      <div class="detail-card">
        <div class="detail-header">
          <h3>{{ detail.person.firstName }} {{ detail.person.lastName }}</h3>
          <button class="close-btn" @click="detail = null">✕</button>
        </div>

        <div class="detail-scroll">
          <dl class="detail-grid">
            <dt>E-post</dt>
            <dd>{{ detail.person.email || '–' }}</dd>
            <dt>Telefon</dt>
            <dd>{{ detail.person.phone || '–' }}</dd>
            <dt>Adress</dt>
            <dd>{{ detail.person.address ? detail.person.address + (detail.person.postalCode ? ', ' + detail.person.postalCode : '') : '–' }}</dd>
            <dt>Fastighetsbeteckning</dt>
            <dd>{{ detail.person.propertyDesignation || '–' }}</dd>
            <dt v-if="detail.person.notes">Noteringar</dt>
            <dd v-if="detail.person.notes" class="notes-text">{{ detail.person.notes }}</dd>
          </dl>

          <div class="boats-section">
            <div class="boats-title">Båtar</div>
            <div v-if="detailLoading" class="boats-loading">Laddar…</div>
            <div v-else-if="!detail.boats.length" class="boats-empty">Inga registrerade båtar</div>
            <div v-for="b in detail.boats" :key="b.id" class="boat-block">
              <div class="boat-block-header">
                <span class="boat-block-name">{{ b.name }}</span>
                <span class="boat-block-dims">{{ b.widthM }} m × {{ b.lengthM }} m</span>
                <span v-if="b.registrationNumber" class="boat-block-reg">{{ b.registrationNumber }}</span>
              </div>
              <div v-if="b.assignment" class="boat-assignment">
                <span class="assign-chip occupied">Tilldelad</span>
                <span class="assign-where">{{ b.assignment.dockName }} · Plats {{ b.assignment.slipNumber }}</span>
                <span class="assign-since">fr.o.m. {{ formatDate(b.assignment.assignedDate) }}</span>
              </div>
              <div v-else class="boat-assignment">
                <span class="assign-chip free">Ingen plats</span>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-footer">
          <button class="btn-primary" @click="openEdit(detail.person); detail = null">Redigera</button>
        </div>
      </div>
    </div>

    <BaseModal v-if="modal" :title="editing ? 'Redigera person' : 'Ny person'"
               @close="modal = false" @save="save">
      <div class="form-grid">
        <label>Förnamn *<input v-model="form.firstName" required /></label>
        <label>Efternamn *<input v-model="form.lastName" required /></label>
        <label class="full">E-post *<input v-model="form.email" type="email" required /></label>
        <label>Telefon<input v-model="form.phone" /></label>
        <label>Postnummer<input v-model="form.postalCode" /></label>
        <label class="full">Adress<input v-model="form.address" placeholder="Gatuadress" /></label>
        <label class="full">Fastighetsbeteckning<input v-model="form.propertyDesignation" placeholder="t.ex. Örby 1:23" /></label>
        <label class="full">Noteringar<textarea v-model="form.notes" rows="4" placeholder="Fri text…" /></label>
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
import { getBoats } from '../api/boats'
import { getAssignments } from '../api/assignments'

const persons = ref([])
const search = ref('')
const modal = ref(false)
const editing = ref(null)
const detail = ref(null)
const detailLoading = ref(false)
const err = ref('')

const emptyForm = () => ({
  firstName: '', lastName: '', email: '', phone: '',
  address: '', postalCode: '', propertyDesignation: '', notes: '',
})
const form = ref(emptyForm())

async function load() {
  const { data } = await getPersons(search.value)
  persons.value = data
}

function formatDate(d) {
  return d ? new Date(d + 'T00:00:00').toLocaleDateString('sv-SE') : '–'
}

async function openDetail(p) {
  detail.value = { person: p, boats: [] }
  detailLoading.value = true
  try {
    const [{ data: boats }, { data: assignments }] = await Promise.all([
      getBoats(p.id), getAssignments()
    ])
    const activeByBoat = {}
    for (const a of assignments) {
      if (a.status === 'ACTIVE') activeByBoat[a.boatId] = a
    }
    detail.value.boats = boats.map(b => ({ ...b, assignment: activeByBoat[b.id] ?? null }))
  } finally {
    detailLoading.value = false
  }
}
function openCreate() { editing.value = null; form.value = emptyForm(); err.value = ''; modal.value = true }
function openEdit(p) {
  editing.value = p
  form.value = {
    firstName: p.firstName, lastName: p.lastName, email: p.email, phone: p.phone || '',
    address: p.address || '', postalCode: p.postalCode || '',
    propertyDesignation: p.propertyDesignation || '', notes: p.notes || '',
  }
  err.value = ''; modal.value = true
}

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
  try {
    await deletePerson(p.id)
    await load()
  } catch (e) {
    alert(e.response?.data?.error || 'Kunde inte ta bort personen')
  }
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
td.address-cell { font-size: 0.85rem; color: #444; max-width: 200px; }
.muted { color: #bbb; }
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
label input, label textarea { padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.95rem; font-weight: 400; }
label textarea { resize: vertical; font-family: inherit; }
.error { color: #c0392b; font-size: 0.85rem; margin-top: 0.5rem; }
.clickable-row { cursor: pointer; transition: background 0.1s; }
.clickable-row:hover { background: #f5f8ff; }
.detail-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); z-index: 100; display: flex; align-items: center; justify-content: center; }
.detail-card { background: white; border-radius: 12px; width: 480px; max-width: 95vw; height: min(85vh, 720px); display: flex; flex-direction: column; box-shadow: 0 8px 32px rgba(0,0,0,0.18); overflow: hidden; }
.detail-header { flex-shrink: 0; display: flex; justify-content: space-between; align-items: center; padding: 1.1rem 1.4rem; border-bottom: 1px solid #eee; }
.detail-header h3 { font-size: 1.15rem; margin: 0; }
.close-btn { background: none; border: none; font-size: 1.1rem; color: #888; cursor: pointer; padding: 0.2rem 0.4rem; }
.close-btn:hover { color: #333; background: #f0f0f0; border-radius: 4px; }
.detail-scroll { flex: 1; overflow-y: auto; min-height: 0; }
.detail-grid { display: grid; grid-template-columns: 150px 1fr; gap: 0.5rem 1rem; padding: 1.2rem 1.4rem; margin: 0; }
dt { font-size: 0.82rem; font-weight: 600; color: #888; align-self: start; padding-top: 0.1rem; }
dd { font-size: 0.9rem; color: #222; margin: 0; }
.notes-text { white-space: pre-wrap; color: #555; font-style: italic; }
.detail-footer { flex-shrink: 0; padding: 0.9rem 1.4rem; border-top: 1px solid #eee; display: flex; justify-content: flex-end; }

/* Boats section */
.boats-section { border-top: 1px solid #eee; padding: 1rem 1.4rem; }
.boats-title { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: #888; margin-bottom: 0.75rem; }
.boats-loading { color: #aaa; font-size: 0.85rem; }
.boats-empty { color: #bbb; font-size: 0.88rem; }
.boat-block { background: #f8faff; border: 1px solid #e4ecff; border-radius: 8px; padding: 0.75rem 1rem; margin-bottom: 0.6rem; }
.boat-block-header { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 0.45rem; }
.boat-block-name { font-weight: 700; font-size: 0.95rem; color: #1a3a5c; }
.boat-block-dims { font-size: 0.78rem; color: #888; }
.boat-block-reg { font-size: 0.78rem; color: #aaa; margin-left: auto; }
.boat-assignment { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
.assign-chip { font-size: 0.72rem; font-weight: 700; padding: 0.15rem 0.5rem; border-radius: 10px; }
.assign-chip.occupied { background: #f8d7da; color: #721c24; }
.assign-chip.free { background: #d4edda; color: #155724; }
.assign-where { font-size: 0.85rem; font-weight: 600; color: #333; }
.assign-since { font-size: 0.78rem; color: #999; margin-left: auto; }
</style>

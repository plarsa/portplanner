<template>
  <AppLayout>
    <div class="page-header">
      <h2>Tilldelningar</h2>
      <button class="btn-primary" @click="openCreate">+ Ny tilldelning</button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Båt</th><th>Ägare</th><th>Brygga</th><th>Plats</th>
            <th>Från</th><th>Status</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in assignments" :key="a.id">
            <td>{{ a.boatName }}</td>
            <td>{{ a.ownerName }}</td>
            <td>{{ a.dockName }}</td>
            <td>{{ a.slipNumber }}</td>
            <td>{{ a.assignedDate }}</td>
            <td><span class="badge active">Aktiv</span></td>
            <td>
              <button class="danger" @click="end(a)">Avsluta</button>
            </td>
          </tr>
          <tr v-if="!assignments.length">
            <td colspan="7" class="empty">Inga aktiva tilldelningar</td>
          </tr>
        </tbody>
      </table>
    </div>

    <BaseModal v-if="modal" title="Ny tilldelning" @close="modal = false" @save="save">
      <div class="form-col">
        <label>Båt *
          <select v-model.number="form.boatId" required @change="onBoatChange">
            <option value="">Välj båt…</option>
            <option v-for="b in boats" :key="b.id" :value="b.id">
              {{ b.name }} ({{ b.ownerName }}) – {{ b.lengthM }}×{{ b.widthM }}m
            </option>
          </select>
        </label>
        <label>Ledig båtplats *
          <select v-model.number="form.slipId" required>
            <option value="">Välj plats…</option>
            <option v-for="s in availableSlips" :key="s.id" :value="s.id"
                    :disabled="!slipFits(s)">
              {{ s.dockName }} – {{ s.slipNumber }}
              (max {{ s.maxLengthM }}×{{ s.maxWidthM }}m)
              {{ !slipFits(s) ? '⚠ passar ej' : '' }}
            </option>
          </select>
        </label>
        <p v-if="form.boatId && selectedBoat" class="boat-info">
          Vald båt: {{ selectedBoat.lengthM }}m lång, {{ selectedBoat.widthM }}m bred
          <span v-if="selectedBoat.draftM">, {{ selectedBoat.draftM }}m djupgång</span>
        </p>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import BaseModal from '../components/BaseModal.vue'
import { getAssignments, createAssignment, endAssignment } from '../api/assignments'
import { getBoats } from '../api/boats'
import { getSlips } from '../api/slips'

const assignments = ref([])
const boats = ref([])
const allSlips = ref([])
const modal = ref(false)
const err = ref('')
const form = ref({ boatId: '', slipId: '' })

const availableSlips = computed(() => allSlips.value.filter(s => s.status === 'AVAILABLE'))
const selectedBoat = computed(() => boats.value.find(b => b.id === form.value.boatId))

function slipFits(slip) {
  const b = selectedBoat.value
  if (!b) return true
  if (Number(b.lengthM) > Number(slip.maxLengthM)) return false
  if (Number(b.widthM) > Number(slip.maxWidthM)) return false
  if (b.draftM && slip.maxDraftM && Number(b.draftM) > Number(slip.maxDraftM)) return false
  return true
}

function onBoatChange() { form.value.slipId = '' }

async function load() {
  const [a, b, s] = await Promise.all([getAssignments(), getBoats(), getSlips()])
  assignments.value = a.data
  boats.value = b.data
  allSlips.value = s.data
}

function openCreate() { form.value = { boatId: '', slipId: '' }; err.value = ''; modal.value = true }

async function save() {
  err.value = ''
  try {
    await createAssignment(form.value)
    modal.value = false
    await load()
  } catch (e) {
    err.value = e.response?.data?.error || 'Något gick fel'
  }
}

async function end(a) {
  if (!confirm(`Avsluta tilldelningen för ${a.boatName} på plats ${a.slipNumber}?`)) return
  try {
    await endAssignment(a.id)
    await load()
  } catch (e) {
    alert(e.response?.data?.error || 'Något gick fel')
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
.empty { color: #999; text-align: center; padding: 2rem; }
.badge { padding: 0.2rem 0.7rem; border-radius: 20px; font-size: 0.8rem; font-weight: 600; }
.badge.active { background: #d4edda; color: #155724; }
button { padding: 0.35rem 0.8rem; border-radius: 5px; border: none; cursor: pointer; font-size: 0.85rem; background: #e8f0fe; color: #1a3a5c; }
button.danger { background: #fee; color: #c0392b; }
button.danger:hover { background: #fcc; }
.btn-primary { background: #1a3a5c; color: white; padding: 0.5rem 1.1rem; border-radius: 6px; border: none; cursor: pointer; }
.btn-primary:hover { background: #234e7a; }
.form-col { display: flex; flex-direction: column; gap: 0.85rem; }
label { display: flex; flex-direction: column; font-size: 0.85rem; font-weight: 600; gap: 0.3rem; }
label select { padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.9rem; font-weight: 400; }
.boat-info { background: #e8f0fe; padding: 0.6rem 0.8rem; border-radius: 6px; font-size: 0.85rem; color: #1a3a5c; }
.error { color: #c0392b; font-size: 0.85rem; margin-top: 0.5rem; }
</style>

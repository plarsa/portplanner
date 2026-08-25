<template>
  <div>
    <h2>Min profil</h2>
    <div v-if="loading" class="loading">Laddar...</div>
    <form v-else @submit.prevent="save" class="form">
      <div class="field-group">
        <div class="field readonly">
          <label>Förnamn</label>
          <input :value="me.firstName" disabled />
        </div>
        <div class="field readonly">
          <label>Efternamn</label>
          <input :value="me.lastName" disabled />
        </div>
      </div>
      <div class="field readonly">
        <label>E-post</label>
        <input :value="me.email" disabled />
      </div>
      <div class="field">
        <label>Telefon</label>
        <input v-model="form.phone" type="tel" />
      </div>
      <div class="field">
        <label>Adress</label>
        <input v-model="form.address" type="text" />
      </div>
      <div class="field">
        <label>Postnummer</label>
        <input v-model="form.postalCode" type="text" />
      </div>
      <p v-if="saved" class="success">Uppgifterna sparades!</p>
      <p v-if="saveError" class="error">{{ saveError }}</p>
      <button type="submit" :disabled="saving">{{ saving ? 'Sparar…' : 'Spara' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/axios'

const loading = ref(true)
const saving = ref(false)
const saved = ref(false)
const saveError = ref('')
const me = ref({})
const form = ref({ phone: '', address: '', postalCode: '' })

async function save() {
  saving.value = true
  saved.value = false
  saveError.value = ''
  try {
    await api.put('/me', form.value)
    saved.value = true
    setTimeout(() => { saved.value = false }, 3000)
  } catch {
    saveError.value = 'Kunde inte spara uppgifterna.'
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const { data } = await api.get('/me')
    me.value = data
    form.value = { phone: data.phone ?? '', address: data.address ?? '', postalCode: data.postalCode ?? '' }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
h2 { margin: 0 0 1.5rem; }
.loading { color: #888; }
.form { background: white; border-radius: 10px; padding: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.07); max-width: 480px; }
.field-group { display: flex; gap: 1rem; }
.field { display: flex; flex-direction: column; gap: 0.3rem; margin-bottom: 1rem; flex: 1; }
label { font-size: 0.82rem; font-weight: 600; color: #555; }
input {
  padding: 0.55rem 0.8rem;
  border: 1px solid #ccd6e0;
  border-radius: 6px;
  font-size: 0.95rem;
  outline: none;
  background: white;
}
input:focus { border-color: #1a3a5c; }
.readonly input { background: #f7f9fb; color: #888; cursor: not-allowed; }
.success { color: #27ae60; font-size: 0.85rem; }
.error { color: #c0392b; font-size: 0.85rem; }
button {
  padding: 0.65rem 1.75rem;
  background: #1a3a5c;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.95rem;
  cursor: pointer;
}
button:hover:not(:disabled) { background: #24527f; }
button:disabled { opacity: 0.6; cursor: not-allowed; }
</style>

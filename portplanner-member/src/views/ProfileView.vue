<template>
  <div>
    <h2>Min profil</h2>
    <p v-if="loading" class="empty">Laddar...</p>
    <form v-else class="panel form" @submit.prevent="save">
      <div class="field-group">
        <div class="field">
          <label for="firstName">Förnamn</label>
          <input id="firstName" class="input" :value="me.firstName" disabled />
        </div>
        <div class="field">
          <label for="lastName">Efternamn</label>
          <input id="lastName" class="input" :value="me.lastName" disabled />
        </div>
      </div>
      <div class="field">
        <label for="email">E-post</label>
        <input id="email" class="input" :value="me.email" disabled />
      </div>
      <div class="field">
        <label for="phone">Telefon</label>
        <input id="phone" class="input" v-model="form.phone" type="tel" />
      </div>
      <div class="field">
        <label for="address">Adress</label>
        <input id="address" class="input" v-model="form.address" type="text" />
      </div>
      <div class="field">
        <label for="postalCode">Postnummer</label>
        <input id="postalCode" class="input num" v-model="form.postalCode" type="text" />
      </div>
      <p v-if="saved" class="success">Uppgifterna sparades.</p>
      <p v-if="saveError" class="error">{{ saveError }}</p>
      <button class="btn btn-primary" type="submit" :disabled="saving">{{ saving ? 'Sparar…' : 'Spara' }}</button>
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
h2 { font-size: 1.45rem; margin: 0 0 24px; }
.form { max-width: 480px; display: flex; flex-direction: column; gap: 14px; }
.field-group { display: flex; gap: 14px; }
.field-group .field { flex: 1; }
.input:disabled { background: var(--paper-deep); color: var(--text-muted); cursor: not-allowed; }
.btn { align-self: flex-start; margin-top: 8px; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
@media (max-width: 620px) {
  .field-group { flex-direction: column; }
}
</style>

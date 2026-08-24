<template>
  <div class="login-page">
    <div class="login-card">
      <h1>⚓ Medlemsportal</h1>
      <form @submit.prevent="submit">
        <div class="field">
          <label>Användarnamn</label>
          <input v-model="form.username" type="text" autocomplete="username" required />
        </div>
        <div class="field">
          <label>Lösenord</label>
          <input v-model="form.password" type="password" autocomplete="current-password" required />
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" :disabled="loading">{{ loading ? 'Loggar in…' : 'Logga in' }}</button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const form = ref({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await axios.post('/api/auth/login', form.value)
    auth.setUser(data)
    router.push('/dashboard')
  } catch {
    error.value = 'Felaktigt användarnamn eller lösenord.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4f7fb;
}
.login-card {
  background: white;
  border-radius: 12px;
  padding: 2.5rem 2rem;
  width: 100%;
  max-width: 380px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}
h1 { margin: 0 0 1.5rem; font-size: 1.4rem; color: #1a3a5c; text-align: center; }
.field { display: flex; flex-direction: column; gap: 0.35rem; margin-bottom: 1rem; }
label { font-size: 0.85rem; font-weight: 600; color: #444; }
input {
  padding: 0.6rem 0.8rem;
  border: 1px solid #ccd6e0;
  border-radius: 6px;
  font-size: 0.95rem;
  outline: none;
}
input:focus { border-color: #1a3a5c; }
.error { color: #c0392b; font-size: 0.85rem; margin: 0.5rem 0; }
button {
  width: 100%;
  margin-top: 0.5rem;
  padding: 0.75rem;
  background: #1a3a5c;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
}
button:disabled { opacity: 0.6; cursor: not-allowed; }
button:hover:not(:disabled) { background: #24527f; }
</style>

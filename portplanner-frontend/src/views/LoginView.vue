<template>
  <div class="login-page">
    <div class="login-card">
      <h1>⚓ Portplanner</h1>
      <p class="subtitle">Logga in för att fortsätta</p>
      <form @submit.prevent="handleLogin">
        <div class="field">
          <label>Användarnamn</label>
          <input v-model="form.username" type="text" autocomplete="username" required />
        </div>
        <div class="field">
          <label>Lösenord</label>
          <input v-model="form.password" type="password" autocomplete="current-password" required />
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" :disabled="loading">
          {{ loading ? 'Loggar in…' : 'Logga in' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const form = ref({ username: '', password: '' })
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    const { data } = await login(form.value.username, form.value.password)
    auth.setUser(data)
    router.push('/dashboard')
  } catch {
    error.value = 'Felaktigt användarnamn eller lösenord'
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
  background: #1a3a5c;
}
.login-card {
  background: white;
  padding: 2.5rem;
  border-radius: 12px;
  width: 100%;
  max-width: 380px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
h1 { font-size: 1.8rem; margin-bottom: 0.25rem; color: #1a3a5c; }
.subtitle { color: #666; margin-bottom: 1.5rem; }
.field { margin-bottom: 1rem; }
label { display: block; font-size: 0.875rem; font-weight: 600; margin-bottom: 0.3rem; }
input {
  width: 100%; padding: 0.6rem 0.8rem;
  border: 1px solid #ddd; border-radius: 6px; font-size: 1rem;
}
input:focus { outline: 2px solid #1a3a5c; border-color: transparent; }
button {
  width: 100%; padding: 0.75rem;
  background: #1a3a5c; color: white;
  border: none; border-radius: 6px;
  font-size: 1rem; font-weight: 600; cursor: pointer;
  margin-top: 0.5rem;
}
button:disabled { opacity: 0.6; cursor: not-allowed; }
button:hover:not(:disabled) { background: #234e7a; }
.error { color: #c0392b; font-size: 0.875rem; margin-bottom: 0.5rem; }
</style>

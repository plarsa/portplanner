<template>
  <div class="login-page">
    <form class="panel login-form" @submit.prevent="submit">
      <div class="brand">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <path d="M6 50H58" /><path d="M15 50V16" /><path d="M28 50V16" />
          <path d="M41 50V16" /><path d="M54 50V16" />
          <circle cx="21.5" cy="36" r="5" fill="currentColor" stroke="none" />
        </svg>
        <h1>Medlemsportal</h1>
      </div>
      <div class="field">
        <label for="username">Användarnamn</label>
        <input id="username" class="input" v-model="form.username" type="text" autocomplete="username" required />
      </div>
      <div class="field">
        <label for="password">Lösenord</label>
        <input id="password" class="input" v-model="form.password" type="password" autocomplete="current-password" required />
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <button class="btn btn-primary" type="submit" :disabled="loading">{{ loading ? 'Loggar in…' : 'Logga in' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api/axios'
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
    const { data } = await api.post('/auth/login', form.value)
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
.login-page { min-height: 60vh; display: flex; align-items: center; justify-content: center; }
.login-form { width: 100%; max-width: 380px; display: flex; flex-direction: column; gap: 14px; }
.brand { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; color: var(--ink); }
.brand svg { width: 26px; height: 26px; stroke-width: 7; }
h1 { font-size: 1.45rem; margin: 0; }
.btn { margin-top: 8px; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>

<template>
  <div class="login-page">
    <form class="panel login-form" @submit.prevent="handleLogin">
      <div class="brand">
        <svg viewBox="0 0 64 64" aria-hidden="true">
          <path d="M6 50H58" /><path d="M15 50V16" /><path d="M28 50V16" />
          <path d="M41 50V16" /><path d="M54 50V16" />
          <circle cx="21.5" cy="36" r="5" fill="currentColor" stroke="none" />
        </svg>
        <h1>Portplanner</h1>
      </div>
      <p class="hint">Logga in för att fortsätta</p>
      <div class="field">
        <label for="username">Användarnamn</label>
        <input id="username" class="input" v-model="form.username" type="text" autocomplete="username" required />
      </div>
      <div class="field">
        <label for="password">Lösenord</label>
        <input id="password" class="input" v-model="form.password" type="password" autocomplete="current-password" required />
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <button class="btn btn-primary" type="submit" :disabled="loading">
        {{ loading ? 'Loggar in…' : 'Logga in' }}
      </button>
    </form>
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
.login-page { flex: 1; display: flex; align-items: center; justify-content: center; padding: 44px 24px; }
.login-form { width: 100%; max-width: 380px; display: flex; flex-direction: column; gap: 14px; }
.brand { display: flex; align-items: center; gap: 10px; color: var(--ink); }
.brand svg { width: 26px; height: 26px; stroke-width: 7; }
h1 { font-size: 1.6rem; margin: 0; }
.hint { margin: -8px 0 4px; }
.btn { margin-top: 8px; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import router from '../router'
import api from '../api/axios'

export const useAuthStore = defineStore('auth', () => {
  const username = ref(localStorage.getItem('username'))
  const role = ref(localStorage.getItem('role'))

  const isLoggedIn = computed(() => !!username.value)

  async function checkAuth() {
    try {
      const { data } = await api.get('/auth/me')
      username.value = data.username
      role.value = data.role
      localStorage.setItem('username', data.username)
      localStorage.setItem('role', data.role)
    } catch {
      username.value = null
      role.value = null
      localStorage.removeItem('username')
      localStorage.removeItem('role')
    }
  }

  function setUser(data) {
    username.value = data.username
    role.value = data.role
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
  }

  async function logout() {
    try { await api.post('/auth/logout') } catch {}
    username.value = null
    role.value = null
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    router.push('/login')
  }

  return { username, role, isLoggedIn, checkAuth, setUser, logout }
})

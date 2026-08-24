import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import router from '../router'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token'))
  const username = ref(localStorage.getItem('username'))
  const role = ref(localStorage.getItem('role'))

  const isLoggedIn = computed(() => !!token.value)

  function setUser(data) {
    token.value = data.token
    username.value = data.username
    role.value = data.role
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
  }

  function logout() {
    token.value = null
    username.value = null
    role.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    router.push('/login')
  }

  return { token, username, role, isLoggedIn, setUser, logout }
})

import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import { useModulesStore } from '../stores/modules'

const api = axios.create({ baseURL: '/api', withCredentials: true })

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      useAuthStore().logout()
    }
    const code = err.response?.status === 403 && err.response.data?.error
    if (code === 'MODULE_DISABLED' || code === 'MODULE_ROLE_FORBIDDEN') {
      useModulesStore().load()
    }
    return Promise.reject(err)
  }
)

export default api

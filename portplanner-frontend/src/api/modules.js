import api from './axios'

export const getModules = () => api.get('/modules').then(r => r.data.modules)

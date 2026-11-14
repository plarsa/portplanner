import api from './axios'

export const getVersion = () => api.get('/version').then(r => r.data.version)

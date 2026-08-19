import api from './axios'

export const exportDocks = () => api.get('/export/docks')
export const previewImport = (data) => api.post('/import/docks/preview', data)
export const importDocks = (data) => api.post('/import/docks', data)

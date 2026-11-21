import api from './axios'

// The whole harbour travels as one file, one section per area.
export const exportAll = () => api.get('/export')
export const previewImport = (file) => api.post('/import/preview', file)
export const importAll = (file) => api.post('/import', file)

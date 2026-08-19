import api from './axios'

export const exportDocks   = ()     => api.get('/export/docks')
export const previewImport = (data) => api.post('/import/docks/preview', data)
export const importDocks   = (data) => api.post('/import/docks', data)

export const exportPersons         = ()     => api.get('/export/persons')
export const previewPersonImport   = (data) => api.post('/import/persons/preview', data)
export const importPersons         = (data) => api.post('/import/persons', data)

export const exportBoats           = ()     => api.get('/export/boats')
export const previewBoatImport     = (data) => api.post('/import/boats/preview', data)
export const importBoats           = (data) => api.post('/import/boats', data)

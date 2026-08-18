import api from './axios'

export const getBoats = (ownerId) => api.get('/boats', { params: { ownerId } })
export const getBoat = (id) => api.get(`/boats/${id}`)
export const createBoat = (data) => api.post('/boats', data)
export const updateBoat = (id, data) => api.put(`/boats/${id}`, data)
export const deleteBoat = (id) => api.delete(`/boats/${id}`)

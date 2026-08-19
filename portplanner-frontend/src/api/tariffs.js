import api from './axios'

export const getTariffs = () => api.get('/tariffs')
export const getActiveTariffs = () => api.get('/tariffs/active')
export const createTariff = (data) => api.post('/tariffs', data)
export const updateTariff = (id, data) => api.put(`/tariffs/${id}`, data)
export const deleteTariff = (id) => api.delete(`/tariffs/${id}`)

import api from './axios'

export const getSlips = () => api.get('/slips')
export const getSlip = (id) => api.get(`/slips/${id}`)
export const createSlip = (data) => api.post('/slips', data)
export const updateSlip = (id, data) => api.put(`/slips/${id}`, data)
export const deleteSlip = (id) => api.delete(`/slips/${id}`)

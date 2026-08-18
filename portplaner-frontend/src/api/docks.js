import api from './axios'

export const getDocks = () => api.get('/docks')
export const getDock = (id) => api.get(`/docks/${id}`)
export const getDockSlips = (id) => api.get(`/docks/${id}/slips`)
export const createDock = (data) => api.post('/docks', data)
export const updateDock = (id, data) => api.put(`/docks/${id}`, data)
export const deleteDock = (id) => api.delete(`/docks/${id}`)

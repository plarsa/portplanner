import api from './axios'

export const getQueue = () => api.get('/queue')
export const addToQueue = (data) => api.post('/queue', data)
export const getSuggestions = (id) => api.get(`/queue/${id}/suggestions`)
export const assignFromQueue = (id, slipId) => api.post(`/queue/${id}/assign`, null, { params: { slipId } })
export const cancelQueueEntry = (id) => api.delete(`/queue/${id}`)

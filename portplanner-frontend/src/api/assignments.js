import api from './axios'

export const getAssignments = () => api.get('/assignments')
export const getBoatHistory = (boatId) => api.get(`/assignments/boat/${boatId}`)
export const createAssignment = (data) => api.post('/assignments', data)
export const endAssignment = (id) => api.put(`/assignments/${id}/end`)

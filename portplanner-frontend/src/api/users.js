import api from './axios'

export const getUsers = () => api.get('/users')
export const getAssignableRoles = () => api.get('/roles')
export const createUser = (data) => api.post('/users', data)
export const resetPassword = (id) => api.post(`/users/${id}/reset-password`)

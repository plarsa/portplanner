import api from './axios'

export const getPersons = (search) => api.get('/persons', { params: { search } })
export const getPerson = (id) => api.get(`/persons/${id}`)
export const createPerson = (data) => api.post('/persons', data)
export const updatePerson = (id, data) => api.put(`/persons/${id}`, data)
export const deletePerson = (id) => api.delete(`/persons/${id}`)

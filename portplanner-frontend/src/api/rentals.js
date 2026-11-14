import api from './axios'

// Objects
export const getObjects = () => api.get('/rentals/objects')
export const getObject = (id) => api.get(`/rentals/objects/${id}`)
export const createObject = (data) => api.post('/rentals/objects', data)
export const updateObject = (id, data) => api.put(`/rentals/objects/${id}`, data)
export const deleteObject = (id) => api.delete(`/rentals/objects/${id}`)

// Image. The browser sets the multipart boundary, so no Content-Type here.
export const uploadImage = (id, file) => {
  const body = new FormData()
  body.append('file', file)
  return api.post(`/rentals/objects/${id}/image`, body)
}
export const deleteImage = (id) => api.delete(`/rentals/objects/${id}/image`)

// Bookings
export const getBookings = (objectId, from, to) =>
  api.get('/rentals/bookings', { params: { objectId, from, to } })
export const createBooking = (data) => api.post('/rentals/bookings', data)
export const approveBooking = (id) => api.post(`/rentals/bookings/${id}/approve`)
export const rejectBooking = (id) => api.post(`/rentals/bookings/${id}/reject`)
export const returnBooking = (id, faultDescription) =>
  api.post(`/rentals/bookings/${id}/return`, { faultDescription })
export const cancelBooking = (id) => api.delete(`/rentals/bookings/${id}`)

// Fault reports
export const getFaultReports = (params) => api.get('/rentals/fault-reports', { params })
export const resolveFaultReport = (id) => api.post(`/rentals/fault-reports/${id}/resolve`)

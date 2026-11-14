import api from './axios'

export const getObjects = () => api.get('/me/rentals/objects')
export const getAvailability = (objectId, from, to) =>
  api.get(`/me/rentals/objects/${objectId}/availability`, { params: { from, to } })
export const getMyBookings = () => api.get('/me/rentals/bookings')
export const createBooking = (data) => api.post('/me/rentals/bookings', data)
export const cancelBooking = (id) => api.delete(`/me/rentals/bookings/${id}`)
export const returnBooking = (id, faultDescription) =>
  api.post(`/me/rentals/bookings/${id}/return`, { faultDescription })

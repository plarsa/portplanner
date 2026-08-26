import api from './axios'

export const getAvailableSlots = () => api.get('/me/haul-out-slots/available')
export const getMyBookings = () => api.get('/me/haul-out-bookings')
export const createMyBooking = (slotId, boatId) =>
  api.post('/me/haul-out-bookings', null, { params: { slotId, boatId } })

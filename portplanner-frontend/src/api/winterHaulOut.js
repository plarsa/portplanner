import api from './axios'

// Winter seasons
export const getWinterSeasons = () => api.get('/winter-seasons')
export const getWinterSeason = (id) => api.get(`/winter-seasons/${id}`)
export const createWinterSeason = (data) => api.post('/winter-seasons', data)
export const updateWinterSeason = (id, data) => api.put(`/winter-seasons/${id}`, data)
export const deleteWinterSeason = (id) => api.delete(`/winter-seasons/${id}`)

// Haul-out slots
export const getHaulOutSlots = (seasonId) => api.get('/haul-out-slots', { params: { seasonId } })
export const createHaulOutSlot = (data) => api.post('/haul-out-slots', data)
export const updateHaulOutSlot = (id, data) => api.put(`/haul-out-slots/${id}`, data)
export const deleteHaulOutSlot = (id) => api.delete(`/haul-out-slots/${id}`)

// Haul-out bookings
export const getHaulOutBookingsBySlot = (slotId) => api.get('/haul-out-bookings', { params: { slotId } })
export const getHaulOutBookingsBySeason = (seasonId) => api.get('/haul-out-bookings', { params: { seasonId } })
export const createHaulOutBooking = (data) => api.post('/haul-out-bookings', data)
export const confirmHaulOutBooking = (id) => api.put(`/haul-out-bookings/${id}/confirm`)
export const deleteHaulOutBooking = (id) => api.delete(`/haul-out-bookings/${id}`)
export const getBookingPrice = (id) => api.get(`/haul-out-bookings/${id}/price`)

// Storage yards
export const getStorageYards = (seasonId) => api.get('/storage-yards', { params: { seasonId } })
export const getStorageYard = (id) => api.get(`/storage-yards/${id}`)
export const createStorageYard = (data) => api.post('/storage-yards', data)
export const updateStorageYard = (id, data) => api.put(`/storage-yards/${id}`, data)
export const calibrateStorageYard = (id, data) => api.put(`/storage-yards/${id}/calibration`, data)
export const suggestPlacements = (id) => api.post(`/storage-yards/${id}/suggest-placement`)
export const validateYard = (id) => api.post(`/storage-yards/${id}/validate`)
export const deleteStorageYard = (id) => api.delete(`/storage-yards/${id}`)

// Storage placements
export const getStoragePlacements = (yardId) => api.get('/storage-placements', { params: { yardId } })
export const updateStoragePlacement = (id, data) => api.put(`/storage-placements/${id}`, data)
export const launchStoragePlacement = (id) => api.put(`/storage-placements/${id}/launch`)

// Packing groups
export const getPackingGroups = (yardId) => api.get('/storage-packing-groups', { params: { yardId } })
export const createPackingGroup = (data) => api.post('/storage-packing-groups', data)
export const updatePackingGroup = (id, data) => api.put(`/storage-packing-groups/${id}`, data)
export const deletePackingGroup = (id) => api.delete(`/storage-packing-groups/${id}`)

// Pricing rules
export const getPricingRules = (seasonId) => api.get('/pricing-rules', { params: { seasonId } })
export const createPricingRule = (data) => api.post('/pricing-rules', data)
export const updatePricingRule = (id, data) => api.put(`/pricing-rules/${id}`, data)
export const deletePricingRule = (id) => api.delete(`/pricing-rules/${id}`)

import api from './axios'

export const getSlipActiveAssignment = (slipId) => api.get(`/slips/${slipId}/active-assignment`)

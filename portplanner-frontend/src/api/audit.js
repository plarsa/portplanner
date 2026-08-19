import api from './axios'

export const getAuditLog = (entityType) =>
  api.get('/audit', { params: entityType ? { entityType } : {} })

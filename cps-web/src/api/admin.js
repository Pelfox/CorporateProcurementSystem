import { requestJson, queryString } from './client.js';
export const getAuditEvents = (filters) => requestJson('/admin/audit' + queryString(filters));
export const restoreRecord = (entity, id) => requestJson(`/admin/deleted/${entity}/${id}/restore`, { method: 'POST' });
export const purgeRecord = (entity, id) => requestJson(`/admin/deleted/${entity}/${id}`, { method: 'DELETE' });

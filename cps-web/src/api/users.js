import { getAllPages, requestJson } from './client.js';
export const getUsers = () => getAllPages('/admin/users');
export const createUser = (username, password, role, fullName) => requestJson('/admin/users', {
    method: 'POST', body: JSON.stringify({ username, fullName, password, role }),
});
export const updateUser = (id, data) => requestJson(`/admin/users/${id}`, { method: 'PUT', body: JSON.stringify(data) });
export const setUserStatus = (id, enabled) => requestJson(`/admin/users/${id}/status`, { method: 'PATCH', body: JSON.stringify({ enabled }) });
export const resetUserPassword = (id, password) => requestJson(`/admin/users/${id}/password`, { method: 'PUT', body: JSON.stringify({ password }) });

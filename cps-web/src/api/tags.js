import { requestJson } from './client.js';
export const getTags = () => requestJson('/tags');
export const createTag = (name) => requestJson('/tags', { method: 'POST', body: JSON.stringify({ name }) });
export const updateTag = (id, name) => requestJson('/tags/' + id, { method: 'PATCH', body: JSON.stringify({ name }) });
export const deleteTag = (id) => requestJson('/tags/' + id, { method: 'DELETE' });

import { getAllPages, requestJson } from './client.js';
export const getCategories = () => getAllPages('/categories');
export const createCategory = (name) => requestJson('/categories', { method: 'POST', body: JSON.stringify({ name }) });
export const updateCategory = (id, name) => requestJson('/categories/' + id, { method: 'PATCH', body: JSON.stringify({ name }) });
export const deleteCategory = (id) => requestJson('/categories/' + id, { method: 'DELETE' });

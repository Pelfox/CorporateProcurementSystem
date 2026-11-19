import { getAllPages, requestJson } from './client.js';
export const getProducts = (filters) => getAllPages('/products', filters);
export const createProduct = (data) => requestJson('/products', { method: 'POST', body: JSON.stringify(data) });
export const updateProduct = (id, data) => requestJson('/products/' + id, { method: 'PATCH', body: JSON.stringify(data) });
export const deleteProduct = (id) => requestJson('/products/' + id, { method: 'DELETE' });
export const getProductTags = (id) => requestJson(`/products/${id}/tags`);
export const addProductTag = (id, tagId) => requestJson(`/products/${id}/tags/${tagId}`, { method: 'PUT' });
export const removeProductTag = (id, tagId) => requestJson(`/products/${id}/tags/${tagId}`, { method: 'DELETE' });

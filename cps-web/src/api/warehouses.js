import { getAllPages, requestJson } from './client.js';
export const getWarehouses = () => getAllPages('/warehouses');
export const createWarehouse = (name, location) => requestJson('/warehouses', { method: 'POST', body: JSON.stringify({ name, location }) });
export const updateWarehouse = (id, name, location) => requestJson('/warehouses/' + id, { method: 'PATCH', body: JSON.stringify({ name, location }) });
export const deleteWarehouse = (id) => requestJson('/warehouses/' + id, { method: 'DELETE' });

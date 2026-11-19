import { getAllPages, requestJson } from './client.js';
export const getAllInventory = () => getAllPages('/inventories');
export const getInventoryByWarehouse = (warehouseId) => getAllPages('/inventories', { warehouseId });
export const addInventory = (productId, warehouseId, quantity) => requestJson('/inventories', {
    method: 'POST', body: JSON.stringify({ productId, warehouseId, quantity: Number(quantity) }),
});
export const updateInventory = (id, quantity) => requestJson('/inventories/' + id, { method: 'PATCH', body: JSON.stringify({ quantity: Number(quantity) }) });
export const deleteInventory = (id) => requestJson('/inventories/' + id, { method: 'DELETE' });

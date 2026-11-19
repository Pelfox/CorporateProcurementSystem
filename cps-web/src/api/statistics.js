import { requestJson } from './client.js';
import { getWarehouses } from './warehouses.js';
import { getAllInventory } from './inventory.js';
export async function getStatistics() {
    const stats = await requestJson('/statistics/summary');
    return { ...stats, activeOrders: stats.createdOrders + stats.approvedOrders };
}
export async function getWarehouseStocks() {
    const [warehouses, inventory] = await Promise.all([getWarehouses(), getAllInventory()]);
    return warehouses.map((warehouse) => ({ warehouseName: warehouse.name,
        quantity: inventory.filter((item) => item.warehouseId === warehouse.id).reduce((sum, item) => sum + item.quantity, 0),
    }));
}
export const getProductStatistics = () => requestJson('/statistics/products');
export const getProductsByCategory = () => requestJson('/statistics/products/by-category');
export const getProductsByStock = () => requestJson('/statistics/products/by-stock');

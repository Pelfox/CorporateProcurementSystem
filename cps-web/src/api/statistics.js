import { apiRequest } from './client';

const USE_MOCKS = true;

const MOCK_STATISTICS = {
    totalUsers: 3,
    totalProducts: 12,
    totalCategories: 4,
    totalWarehouses: 3,
    totalOrders: 6,
    activeOrders: 3, // CREATED + APPROVED
    createdOrders: 2,
    approvedOrders: 1,
    deliveredOrders: 1,
    cancelledOrders: 1,
    totalStockQuantity: 542,
    totalOrderValue: 412730.75,
};

const MOCK_WAREHOUSE_STOCKS = [
    { warehouseName: 'Главный склад', quantity: 180 },
    { warehouseName: 'Резервный склад', quantity: 320 },
    { warehouseName: 'Склад филиала', quantity: 42 },
];

export async function getStatistics() {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 250));
        return MOCK_STATISTICS;
    }
    const response = await apiRequest('/statistics');
    if (!response.ok) {
        throw new Error('Не удалось загрузить статистику');
    }
    return response.json();
}

export async function getWarehouseStocks() {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 200));
        return MOCK_WAREHOUSE_STOCKS;
    }
    const response = await apiRequest('/statistics/warehouses');
    if (!response.ok) {
        throw new Error('Не удалось загрузить остатки по складам');
    }
    return response.json();
}
import { apiRequest } from './client';

const USE_MOCKS = true;

let mockInventory = [
    { id: '6001', warehouseId: '5001', productId: '3001', quantity: 15 },
    { id: '6002', warehouseId: '5001', productId: '3002', quantity: 42 },
    { id: '6003', warehouseId: '5001', productId: '3003', quantity: 120 },
    { id: '6004', warehouseId: '5002', productId: '3004', quantity: 300 },
    { id: '6005', warehouseId: '5002', productId: '3008', quantity: 55 },
    { id: '6006', warehouseId: '5003', productId: '3005', quantity: 8 },
    { id: '6007', warehouseId: '5003', productId: '3006', quantity: 12 },
];

let seq = 8;

export async function getAllInventory() {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 50));
        return mockInventory.slice();
    }
    const response = await apiRequest('/inventory');
    if (!response.ok) {
        throw new Error('Не удалось загрузить инвентарь');
    }
    return response.json();
}

export async function getInventoryByWarehouse(warehouseId) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 80));
        return mockInventory.filter((i) => i.warehouseId === warehouseId);
    }
    const response = await apiRequest('/inventory?warehouseId=' + warehouseId);
    if (!response.ok) {
        throw new Error('Не удалось загрузить остатки');
    }
    return response.json();
}

export async function addInventory(productId, warehouseId, quantity) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 100));
        if (mockInventory.some((i) => i.productId === productId && i.warehouseId === warehouseId)) {
            throw new Error('Товар уже размещён на этом складе. Измените существующий остаток.');
        }
        const qty = Number(quantity);
        if (!Number.isInteger(qty) || qty < 0) {
            throw new Error('Количество должно быть целым числом не меньше 0');
        }
        const id = seq + '00' + seq;
        seq += 1;
        const created = { id, warehouseId, productId, quantity: qty };
        mockInventory = [...mockInventory, created];
        return created;
    }
    const response = await apiRequest('/inventory', {
        method: 'POST',
        body: JSON.stringify({ productId, warehouseId, quantity }),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось добавить товар на склад');
    }
    return response.json();
}

export async function updateInventory(id, quantity) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 100));
        const qty = Number(quantity);
        if (!Number.isInteger(qty) || qty < 0) {
            throw new Error('Количество должно быть целым числом не меньше 0');
        }
        mockInventory = mockInventory.map((i) => (i.id === id ? { ...i, quantity: qty } : i));
        return mockInventory.find((i) => i.id === id);
    }
    const response = await apiRequest('/inventory/' + id, {
        method: 'PUT',
        body: JSON.stringify({ quantity }),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось сохранить остаток');
    }
    return response.json();
}

export async function deleteInventory(id) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 100));
        mockInventory = mockInventory.filter((i) => i.id !== id);
        return;
    }
    const response = await apiRequest('/inventory/' + id, {
        method: 'DELETE',
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось удалить остаток');
    }
}
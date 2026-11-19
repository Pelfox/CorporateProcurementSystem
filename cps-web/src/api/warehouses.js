import { apiRequest } from './client';

const USE_MOCKS = true;

let mockWarehouses = [
    { id: '5001', name: 'Главный склад', location: 'г. Москва, ул. Ленина 1' },
    { id: '5002', name: 'Резервный склад', location: 'г. Москва, ул. Мира 15' },
    { id: '5003', name: 'Склад филиала', location: 'г. Санкт-Петербург, Невский пр. 50' },
];

let seq = 4;

export async function getWarehouses() {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 100));
        return mockWarehouses.slice();
    }
    const response = await apiRequest('/warehouses');
    if (!response.ok) {
        throw new Error('Не удалось загрузить склады');
    }
    return response.json();
}

export async function createWarehouse(name, location) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 100));
        const trimmed = String(name || '').trim();
        if (!trimmed) {
            throw new Error('Название склада не может быть пустым');
        }
        const id = seq + '00' + seq;
        seq += 1;
        const created = { id, name: trimmed, location: location || null };
        mockWarehouses = [...mockWarehouses, created];
        return created;
    }
    const response = await apiRequest('/warehouses', {
        method: 'POST',
        body: JSON.stringify({ name, location }),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось создать склад');
    }
    return response.json();
}

export async function updateWarehouse(id, name, location) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 100));
        const trimmed = String(name || '').trim();
        if (!trimmed) {
            throw new Error('Название склада не может быть пустым');
        }
        mockWarehouses = mockWarehouses.map((w) =>
            w.id === id ? { ...w, name: trimmed, location: location || null } : w
        );
        return mockWarehouses.find((w) => w.id === id);
    }
    const response = await apiRequest('/warehouses/' + id, {
        method: 'PUT',
        body: JSON.stringify({ name, location }),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось сохранить склад');
    }
    return response.json();
}

export async function deleteWarehouse(id) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 100));
        // Имитация правила, если есть остатки, то удалить нельзя
        const { getInventoryByWarehouse } = await import('./inventory');
        const items = await getInventoryByWarehouse(id);
        if (items.length > 0) {
            throw new Error('На складе есть остатки. Удалите их, затем повторите.');
        }
        mockWarehouses = mockWarehouses.filter((w) => w.id !== id);
        return;
    }
    const response = await apiRequest('/warehouses/' + id, {
        method: 'DELETE',
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось удалить склад');
    }
}
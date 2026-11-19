import { apiRequest } from './client';

const USE_MOCKS = true;

let mockProducts = [
    { id: '3001', sku: 'NTB-001', name: 'Ноутбук Lenovo ThinkPad', categoryId: '2001', price: 89990.0 },
    { id: '3002', sku: 'MOU-002', name: 'Мышь Logitech MX Master', categoryId: '2001', price: 4500.0 },
    { id: '3003', sku: 'PAP-003', name: 'Бумага А4, 500 листов', categoryId: '2002', price: 420.5 },
    { id: '3004', sku: 'PEN-004', name: 'Ручка шариковая синяя', categoryId: '2002', price: 35.0 },
    { id: '3005', sku: 'CHR-005', name: 'Кресло офисное', categoryId: '2003', price: 18500.0 },
    { id: '3006', sku: 'CRT-006', name: 'Картридж HP 26A', categoryId: '2004', price: 8200.0 },
    { id: '3007', sku: 'TBL-007', name: 'Стол письменный', categoryId: '2003', price: 12000.0 },
    { id: '3008', sku: 'NOT-008', name: 'Блокнот А5', categoryId: '2002', price: 180.0 },
];

let seq = 9;

export async function getProducts() {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 200));
        return mockProducts.slice();
    }
    const response = await apiRequest('/products');
    if (!response.ok) {
        throw new Error('Не удалось загрузить товары');
    }
    return response.json();
}

export async function createProduct(data) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 150));
        const id = seq + '00' + seq;
        seq += 1;
        const created = {
            id,
            sku: data.sku,
            name: data.name,
            categoryId: data.categoryId,
            price: Number(data.price),
        };
        mockProducts = [...mockProducts, created];
        return created;
    }
    const response = await apiRequest('/products', {
        method: 'POST',
        body: JSON.stringify(data),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось создать товар');
    }
    return response.json();
}

export async function updateProduct(id, data) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 150));
        mockProducts = mockProducts.map((p) =>
            p.id === id
                ? { ...p, sku: data.sku, name: data.name, categoryId: data.categoryId, price: Number(data.price) }
                : p
        );
        return mockProducts.find((p) => p.id === id);
    }
    const response = await apiRequest('/products/' + id, {
        method: 'PUT',
        body: JSON.stringify(data),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось сохранить товар');
    }
    return response.json();
}

export async function deleteProduct(id) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 150));
        mockProducts = mockProducts.filter((p) => p.id !== id);
        return;
    }
    const response = await apiRequest('/products/' + id, {
        method: 'DELETE',
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось удалить товар');
    }
}
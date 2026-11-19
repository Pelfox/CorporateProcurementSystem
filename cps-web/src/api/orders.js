import { apiRequest } from './client';
import { getAllInventory } from './inventory';

const USE_MOCKS = true;

let mockOrders = [
    {
        id: '1',
        userId: '1',
        username: 'dim',
        status: 'CREATED',
        notes: 'Закупка канцтоваров для офиса',
        createdAt: '2025-11-14T09:15:00Z',
        updatedAt: '2025-11-14T09:15:00Z',
        items: [
            { productId: '3001', productName: 'Ноутбук Lenovo ThinkPad', sku: 'NTB-001', quantity: 1, purchasePrice: 89990.0 },
            { productId: '3002', productName: 'Мышь Logitech MX Master', sku: 'MOU-002', quantity: 2, purchasePrice: 4500.0 },
            { productId: '3003', productName: 'Бумага А4', sku: 'PAP-003', quantity: 10, purchasePrice: 420.5 },
        ],
    },
    {
        id: '2',
        userId: '2',
        username: 'nik',
        status: 'APPROVED',
        notes: 'Ноутбуки для разработчиков',
        createdAt: '2025-11-12T14:30:00Z',
        updatedAt: '2025-11-13T10:00:00Z',
        items: [
            { productId: '3001', productName: 'Ноутбук Lenovo ThinkPad', sku: 'NTB-001', quantity: 3, purchasePrice: 89990.0 },
        ],
    },
    {
        id: '3',
        userId: '1',
        username: 'ivan_sid',
        status: 'DELIVERED',
        notes: null,
        createdAt: '2025-11-08T11:00:00Z',
        updatedAt: '2025-11-10T16:45:00Z',
        items: [
            { productId: '3004', productName: 'Ручка шариковая', sku: 'PEN-004', quantity: 300, purchasePrice: 35.0 },
        ],
    },
    {
        id: '4',
        userId: '3',
        username: 'mat',
        status: 'CANCELLED',
        notes: 'Отменён по просьбе заказчика',
        createdAt: '2025-11-05T08:20:00Z',
        updatedAt: '2025-11-06T12:00:00Z',
        items: [
            { productId: '3008', productName: 'Блокнот А5', sku: 'NOT-008', quantity: 20, purchasePrice: 180.0 },
        ],
    },
    {
        id: '5',
        userId: '2',
        username: 'nik',
        status: 'CREATED',
        notes: 'Кулер и вода на месяц',
        createdAt: '2025-11-15T16:00:00Z',
        updatedAt: '2025-11-15T16:00:00Z',
        items: [
            { productId: '3005', productName: 'Кресло офисное', sku: 'CHR-005', quantity: 1, purchasePrice: 18500.0 },
            { productId: '3006', productName: 'Картридж HP 26A', sku: 'CRT-006', quantity: 2, purchasePrice: 8200.0 },
        ],
    },
    {
        id: '6',
        userId: '3',
        username: 'mat',
        status: 'APPROVED',
        notes: 'Офисные кресла — 5 штук',
        createdAt: '2025-11-10T12:00:00Z',
        updatedAt: '2025-11-11T09:30:00Z',
        items: [
            { productId: '3005', productName: 'Кресло офисное', sku: 'CHR-005', quantity: 5, purchasePrice: 8500.0 },
        ],
    },
];

let seq = 7;

export function orderTotal(order) {
    return order.items.reduce(
        (sum, item) => sum + item.purchasePrice * item.quantity,
        0
    );
}

export function orderItemsCount(order) {
    return order.items.reduce((sum, item) => sum + item.quantity, 0);
}

function withComputed(order) {
    return {
        ...order,
        itemsCount: orderItemsCount(order),
        total: orderTotal(order),
    };
}

export async function getMyOrders(userId) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 200));
        return mockOrders.map(withComputed);
    }
    const query = userId ? '?userId=' + encodeURIComponent(userId) : '';
    const response = await apiRequest('/orders' + query);
    if (!response.ok) {
        throw new Error('Не удалось загрузить заказы');
    }
    return response.json();
}

export async function getAllOrders() {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 200));
        return mockOrders.map(withComputed);
    }
    const response = await apiRequest('/orders');
    if (!response.ok) {
        throw new Error('Не удалось загрузить заказы');
    }
    return response.json();
}

export async function getOrder(orderId) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 80));
        const order = mockOrders.find((o) => o.id === orderId);
        return order ? withComputed(order) : null;
    }
    const response = await apiRequest('/orders/' + orderId);
    if (!response.ok) {
        throw new Error('Заказ не найден');
    }
    return response.json();
}

// массив [{productId, available}]
export async function getAvailability(editingOrderId) {
    if (!USE_MOCKS) {
        const url = editingOrderId
            ? '/inventory/availability?editingOrderId=' + encodeURIComponent(editingOrderId)
            : '/inventory/availability';
        const response = await apiRequest(url);
        if (!response.ok) {
            throw new Error('Не удалось получить доступные остатки');
        }
        return response.json();
    }

    // физический остаток минус резервы других активных заказов
    const { getProducts } = await import('./products');
    const [inventory, products] = await Promise.all([getAllInventory(), getProducts()]);

    const stock = {};
    inventory.forEach((i) => {
        stock[i.productId] = (stock[i.productId] || 0) + i.quantity;
    });

    mockOrders.forEach((o) => {
        if (o.id === editingOrderId) return;
        if (o.status !== 'CREATED' && o.status !== 'APPROVED') return;
        o.items.forEach((item) => {
            stock[item.productId] = (stock[item.productId] || 0) - item.quantity;
        });
    });

    return products.map((p) => ({
        productId: p.id,
        available: Math.max(0, stock[p.id] || 0),
    }));
}

export async function createOrder(userId, items, notes, usernameHint) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 200));
        if (!items || items.length === 0) {
            throw new Error('Добавьте хотя бы один товар');
        }

        const { getProducts } = await import('./products');
        const products = await getProducts();
        const productById = {};
        products.forEach((p) => { productById[p.id] = p; });

        const orderItems = items.map((it) => {
            const p = productById[it.productId];
            if (!p) throw new Error('Товар не найден');
            return {
                productId: p.id,
                productName: p.name,
                sku: p.sku,
                quantity: it.quantity,
                purchasePrice: p.price,
            };
        });

        const now = new Date().toISOString();
        const id = 'a1b2c3d4-' + String(seq).padStart(4, '0') + '-4000-8000-000000000001';
        seq += 1;

        const created = {
            id,
            userId,
            username: usernameHint || '(вы)',
            status: 'CREATED',
            notes: notes || null,
            createdAt: now,
            updatedAt: now,
            items: orderItems,
        };
        mockOrders = [...mockOrders, created];
        return withComputed(created);
    }

    const response = await apiRequest('/orders', {
        method: 'POST',
        body: JSON.stringify({
            userId,
            items: items.map((it) => ({ productId: it.productId, quantity: it.quantity })),
            notes: notes || null,
        }),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось создать заказ');
    }
    return response.json();
}

export async function updateOrder(orderId, userId, items, notes) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 200));
        const order = mockOrders.find((o) => o.id === orderId);
        if (!order) throw new Error('Заказ не найден');
        if (order.status !== 'CREATED') {
            throw new Error('Можно редактировать только созданный заказ');
        }
        if (!items || items.length === 0) {
            throw new Error('В заказе должна остаться хотя бы одна позиция');
        }

        const { getProducts } = await import('./products');
        const products = await getProducts();
        const productById = {};
        products.forEach((p) => { productById[p.id] = p; });

        const orderItems = items.map((it) => {
            const p = productById[it.productId];
            if (!p) throw new Error('Товар не найден');
            const existing = order.items.find((i) => i.productId === it.productId);
            return {
                productId: p.id,
                productName: p.name,
                sku: p.sku,
                quantity: it.quantity,
                purchasePrice: existing ? existing.purchasePrice : p.price,
            };
        });

        mockOrders = mockOrders.map((o) =>
            o.id === orderId
                ? { ...o, items: orderItems, notes: notes || null, updatedAt: new Date().toISOString() }
                : o
        );
        return;
    }

    const response = await apiRequest('/orders/' + orderId, {
        method: 'PUT',
        body: JSON.stringify({
            userId,
            items: items.map((it) => ({ productId: it.productId, quantity: it.quantity })),
            notes: notes || null,
        }),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось сохранить заказ');
    }
    return response.json();
}

export async function updateOrderNotes(orderId, notes, userId) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 150));
        const order = mockOrders.find((o) => o.id === orderId);
        if (!order) throw new Error('Заказ не найден');
        mockOrders = mockOrders.map((o) =>
            o.id === orderId ? { ...o, notes, updatedAt: new Date().toISOString() } : o
        );
        return;
    }
    const response = await apiRequest('/orders/' + orderId + '/notes', {
        method: 'PATCH',
        body: JSON.stringify({ userId, notes }),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось сохранить примечание');
    }
}

export async function changeOrderStatus(orderId, status, managerId) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 150));
        const order = mockOrders.find((o) => o.id === orderId);
        if (!order) throw new Error('Заказ не найден');
        if (order.status === 'DELIVERED') {
            throw new Error('Доставленный заказ нельзя перевести в другой статус');
        }
        mockOrders = mockOrders.map((o) =>
            o.id === orderId ? { ...o, status, updatedAt: new Date().toISOString() } : o
        );
        return;
    }
    const response = await apiRequest('/orders/' + orderId + '/status', {
        method: 'PATCH',
        body: JSON.stringify({ managerId, status }),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось изменить статус');
    }
}

export async function deleteOrder(orderId) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 150));
        mockOrders = mockOrders.filter((o) => o.id !== orderId);
        return;
    }
    const response = await apiRequest('/orders/' + orderId, {
        method: 'DELETE',
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось удалить заказ');
    }
}
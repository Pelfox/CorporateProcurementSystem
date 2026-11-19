import { getAllPages, requestJson, queryString, getCurrentUser } from './client.js';
import { getProducts } from './products.js';
export const orderTotal = (order) => order.totalSum ?? order.items.reduce((sum, item) => sum + item.purchasePrice * item.quantity, 0);
export const orderItemsCount = (order) => order.items.reduce((sum, item) => sum + item.quantity, 0);
async function hydrate(order, products) {
    const items = await requestJson(`/orders/${order.id}/items`);
    const currentUser = getCurrentUser();
    const normalized = { ...order, username: order.userId === currentUser?.id ? currentUser.fullName || currentUser.username : order.userId.slice(0, 8),
        items: items.map((item) => ({ ...item, productName: products.get(item.productId)?.name || 'Удалённый товар', sku: products.get(item.productId)?.sku || '—' })),
    };
    return { ...normalized, total: orderTotal(normalized), itemsCount: orderItemsCount(normalized) };
}
async function loadOrders(filters = {}) {
    const [orders, products] = await Promise.all([getAllPages('/orders', filters), getProducts()]);
    const byId = new Map(products.map((product) => [product.id, product]));
    const result = [];
    for (let offset = 0; offset < orders.length; offset += 6) {
        result.push(...await Promise.all(orders.slice(offset, offset + 6).map((order) => hydrate(order, byId))));
    }
    return result;
}
export const getMyOrders = (userId) => loadOrders({ userId });
export const getAllOrders = () => loadOrders();
export async function getOrder(orderId) {
    const [order, products] = await Promise.all([requestJson('/orders/' + orderId), getProducts()]);
    return hydrate(order, new Map(products.map((product) => [product.id, product])));
}
export async function getAvailability(editingOrderId) {
    const quantities = await requestJson('/orders/availability' + queryString({ editingOrderId }));
    return Object.entries(quantities).map(([productId, available]) => ({ productId, available }));
}
export const createOrder = (_userId, items, notes) => requestJson('/orders/checkout', {
    method: 'POST', body: JSON.stringify({ items, notes }),
});
export const updateOrder = (orderId, _userId, items, notes) => requestJson(`/orders/${orderId}/contents`, {
    method: 'PUT', body: JSON.stringify({ items, notes }),
});
export const updateOrderNotes = (orderId, notes) => requestJson('/orders/' + orderId, { method: 'PATCH', body: JSON.stringify({ notes: notes || '' }) });
export const changeOrderStatus = (orderId, status) => requestJson('/orders/' + orderId, { method: 'PATCH', body: JSON.stringify({ status }) });
export const deleteOrder = (orderId) => requestJson('/orders/' + orderId, { method: 'DELETE' });

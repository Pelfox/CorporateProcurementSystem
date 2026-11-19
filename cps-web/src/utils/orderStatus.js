export const ORDER_STATUSES = ['CREATED', 'APPROVED', 'DELIVERED', 'CANCELLED'];

const LABELS = {
    CREATED: 'Создан',
    APPROVED: 'Одобрен',
    DELIVERED: 'Доставлен',
    CANCELLED: 'Отменён',
};

const CSS = {
    CREATED: 'status-created',
    APPROVED: 'status-approved',
    DELIVERED: 'status-delivered',
    CANCELLED: 'status-cancelled',
};

export function statusLabel(status) {
    return LABELS[status] || status;
}

export function statusCss(status) {
    return CSS[status] || '';
}
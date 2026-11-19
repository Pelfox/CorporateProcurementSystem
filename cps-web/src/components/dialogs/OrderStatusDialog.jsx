import { useState } from 'react';
import { changeOrderStatus } from '../../api/orders';
import { ORDER_STATUSES, statusLabel, statusCss } from '../../utils/orderStatus';

// При переходе в новый статус появится подсказка для пользователя
const TRANSITION_HINTS = {
    APPROVED: 'Заказ будет одобрен. Товары останутся зарезервированы на складе.',
    DELIVERED:
        'Остатки будут списаны со склада. Отменить это действие нельзя, заказ перейдёт в финальный статус.',
    CANCELLED: 'Заказ будет отменён. Резервы товаров на складе снимутся.',
    CREATED: 'Заказ вернётся в статус «Создан». Товары останутся зарезервированы.',
};

export default function OrderStatusDialog({ order, currentUserId, onClose, onSaved }) {
    const currentStatus = order.status;
    const isDelivered = currentStatus === 'DELIVERED';

    const availableStatuses = ORDER_STATUSES.filter((s) => s !== currentStatus);
    const [newStatus, setNewStatus] = useState('');
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        if (!newStatus) {
            setError('Выберите новый статус');
            return;
        }
        setError('');
        setSaving(true);
        try {
            await changeOrderStatus(order.id, newStatus, currentUserId);
            onSaved();
        } catch (e) {
            setError(e.message || 'Не удалось изменить статус');
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="dialog-backdrop" onClick={onClose}>
            <div className="dialog-root" onClick={(e) => e.stopPropagation()}>
                <div className="dialog-header">
                    <h2 className="dialog-title">Изменение статуса</h2>
                    <button className="dialog-close" onClick={onClose} type="button">
                        ✕
                    </button>
                </div>

                <div
                    style={{
                        background: 'var(--bg-soft)',
                        borderRadius: 10,
                        padding: '14px 16px',
                        marginBottom: 20,
                    }}
                >
                    <div style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 6 }}>
                        Заказ №{' '}
                        <span style={{ fontFamily: 'monospace' }}>
                            {order.id.substring(0, 8)}
                        </span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                        <span style={{ fontSize: 14, color: 'var(--text-secondary)' }}>
                            Текущий статус:
                        </span>
                        <span className={'status ' + statusCss(currentStatus)}>
                            {statusLabel(currentStatus)}
                        </span>
                        {newStatus && (
                            <>
                                <span style={{ color: 'var(--text-muted)' }}>→</span>
                                <span className={'status ' + statusCss(newStatus)}>
                                    {statusLabel(newStatus)}
                                </span>
                            </>
                        )}
                    </div>
                </div>

                {isDelivered ? (
                    <div
                        style={{
                            background: '#fef3c7',
                            border: '1px solid #f59e0b',
                            color: '#92400e',
                            padding: '10px 14px',
                            borderRadius: 10,
                            fontSize: 13,
                            marginBottom: 16,
                        }}
                    >
                        Доставленный заказ нельзя перевести в другой статус.
                    </div>
                ) : (
                    <>
                        <form onSubmit={handleSubmit}>
                            {error && (
                                <div className="login-error" style={{ marginBottom: 12 }}>
                                    {error}
                                </div>
                            )}

                            <div className="form-group">
                                <label className="form-label">Новый статус *</label>
                                <select
                                    className="form-select"
                                    value={newStatus}
                                    onChange={(e) => {
                                        setNewStatus(e.target.value);
                                        setError('');
                                    }}
                                    disabled={saving}
                                >
                                    <option value="">— Выберите статус —</option>
                                    {availableStatuses.map((s) => (
                                        <option key={s} value={s}>
                                            {statusLabel(s)}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {newStatus && TRANSITION_HINTS[newStatus] && (
                                <div
                                    style={{
                                        background: '#eff6ff',
                                        border: '1px solid #bfdbfe',
                                        color: '#1e40af',
                                        padding: '10px 14px',
                                        borderRadius: 10,
                                        fontSize: 13,
                                        marginBottom: 16,
                                        lineHeight: 1.4,
                                    }}
                                >
                                    {TRANSITION_HINTS[newStatus]}
                                </div>
                            )}

                            <div className="dialog-actions">
                                <button
                                    type="button"
                                    className="btn btn-cancel"
                                    onClick={onClose}
                                    disabled={saving}
                                >
                                    Отмена
                                </button>
                                <button
                                    type="submit"
                                    className="btn btn-primary"
                                    disabled={saving || !newStatus}
                                >
                                    {saving ? 'Сохранение...' : 'Изменить статус'}
                                </button>
                            </div>
                        </form>
                    </>
                )}

                {isDelivered && (
                    <div className="dialog-actions">
                        <button
                            type="button"
                            className="btn btn-cancel"
                            onClick={onClose}
                        >
                            Закрыть
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}
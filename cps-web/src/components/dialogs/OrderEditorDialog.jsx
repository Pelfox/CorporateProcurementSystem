import { useState, useEffect } from 'react';
import { getProducts } from '../../api/products';
import { getAvailability, createOrder, updateOrder } from '../../api/orders';
import { formatMoneyRub } from '../../utils/format';

const MAX_LINE = 100000;
const NOTES_MAX = 255;

export default function OrderEditorDialog({ order, currentUser, onClose, onSaved }) {
    const isEditing = !!order;
    const locked = isEditing && order.status !== 'CREATED';

    const [products, setProducts] = useState([]);
    const [lines, setLines] = useState(
        order
            ? order.items.map((i) => ({ productId: i.productId, quantity: i.quantity }))
            : []
    );
    const [notes, setNotes] = useState(order?.notes || '');
    const [selectedProductId, setSelectedProductId] = useState('');
    const [quantity, setQuantity] = useState('1');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [addError, setAddError] = useState('');
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        async function load() {
            try {
                const [prods, avail] = await Promise.all([
                    getProducts(),
                    getAvailability(order?.id),
                ]);
                const availMap = {};
                avail.forEach((a) => {
                    availMap[a.productId] = a.available;
                });
                setProducts(
                    prods.map((p) => ({ ...p, available: availMap[p.id] || 0 }))
                );
            } catch (e) {
                setError(e.message || 'Не удалось загрузить товары');
            } finally {
                setLoading(false);
            }
        }
        load();
    }, [order?.id]);

    function productById(id) {
        return products.find((p) => p.id === id);
    }

    function inCartQuantity(productId) {
        return lines
            .filter((l) => l.productId === productId)
            .reduce((sum, l) => sum + l.quantity, 0);
    }

    function remaining(product) {
        return Math.max(0, product.available - inCartQuantity(product.id));
    }

    const availableProducts = products.filter((p) => remaining(p) > 0);

    function handleAdd() {
        setAddError('');
        if (!selectedProductId) {
            setAddError('Выберите товар');
            return;
        }
        const product = productById(selectedProductId);
        if (!product) {
            setAddError('Товар не найден');
            return;
        }
        const qty = parseInt(quantity, 10);
        if (!qty || qty < 1) {
            setAddError('Количество — целое число от 1');
            return;
        }
        if (qty > MAX_LINE) {
            setAddError(`Количество не должно превышать ${MAX_LINE}`);
            return;
        }
        const remainingQty = remaining(product);
        if (qty > remainingQty) {
            setAddError(
                `Недостаточно товара «${product.name}». Доступно: ${remainingQty}`
            );
            return;
        }
        const existing = lines.find((l) => l.productId === selectedProductId);
        if (existing) {
            const newQty = existing.quantity + qty;
            if (newQty > MAX_LINE) {
                setAddError(`Максимум ${MAX_LINE} единиц одного товара в заказе`);
                return;
            }
            setLines(
                lines.map((l) =>
                    l.productId === selectedProductId ? { ...l, quantity: newQty } : l
                )
            );
        } else {
            setLines([...lines, { productId: selectedProductId, quantity: qty }]);
        }
        setSelectedProductId('');
        setQuantity('1');
    }

    function handleRemove(productId) {
        setLines(lines.filter((l) => l.productId !== productId));
    }

    const total = lines.reduce((sum, l) => {
        const p = productById(l.productId);
        return sum + (p ? p.price * l.quantity : 0);
    }, 0);

    async function handleSave() {
        setError('');
        if (lines.length === 0) {
            setError('Добавьте хотя бы один товар');
            return;
        }
        setSaving(true);
        try {
            if (isEditing) {
                await updateOrder(order.id, currentUser.id, lines, notes || null);
            } else {
                await createOrder(
                    currentUser.id,
                    lines,
                    notes || null,
                    currentUser.username
                );
            }
            onSaved();
        } catch (e) {
            setError(e.message || 'Не удалось сохранить заказ');
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="dialog-backdrop" onClick={onClose}>
            <div className="dialog-root dialog-wide" onClick={(e) => e.stopPropagation()}>
                <div className="dialog-header">
                    <h2 className="dialog-title">
                        {isEditing ? 'Редактирование заказа' : 'Новый заказ'}
                    </h2>
                    <button className="dialog-close" onClick={onClose} type="button">
                        ✕
                    </button>
                </div>

                {locked && (
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
                        Редактирование доступно только для созданного заказа
                    </div>
                )}

                {error && (
                    <div className="login-error" style={{ marginBottom: 12 }}>
                        {error}
                    </div>
                )}

                {loading ? (
                    <div
                        style={{
                            padding: '40px 0',
                            textAlign: 'center',
                            color: 'var(--text-muted)',
                        }}
                    >
                        Загрузка...
                    </div>
                ) : (
                    <>
                        <div className="form-group">
                            <label className="form-label">Добавить товар</label>
                            <div className="add-line-row">
                                <select
                                    className="form-select"
                                    value={selectedProductId}
                                    onChange={(e) => setSelectedProductId(e.target.value)}
                                    disabled={
                                        locked || saving || availableProducts.length === 0
                                    }
                                >
                                    <option value="">
                                        {availableProducts.length === 0
                                            ? 'Нет товаров в наличии'
                                            : '— Выберите товар —'}
                                    </option>
                                    {availableProducts.map((p) => (
                                        <option key={p.id} value={p.id}>
                                            {p.name} — доступно: {remaining(p)}
                                        </option>
                                    ))}
                                </select>
                                <input
                                    className="form-input"
                                    type="number"
                                    min="1"
                                    max={MAX_LINE}
                                    step="1"
                                    value={quantity}
                                    onChange={(e) => setQuantity(e.target.value)}
                                    disabled={locked || saving}
                                />
                                <button
                                    type="button"
                                    className="btn-add-icon"
                                    onClick={handleAdd}
                                    disabled={locked || saving || !selectedProductId}
                                    title="Добавить товар"
                                >
                                    +
                                </button>
                            </div>
                            {addError && (
                                <div className="form-error" style={{ marginTop: 6 }}>
                                    {addError}
                                </div>
                            )}
                        </div>

                        <div className="form-group">
                            <label className="form-label">Позиции заказа</label>
                            {lines.length === 0 ? (
                                <div
                                    style={{
                                        padding: '20px 0',
                                        textAlign: 'center',
                                        color: 'var(--text-muted)',
                                        fontSize: 13,
                                    }}
                                >
                                    Позиции не добавлены
                                </div>
                            ) : (
                                <div className="dialog-items-table">
                                    <table>
                                        <thead>
                                        <tr>
                                            <th>Товар</th>
                                            <th
                                                style={{
                                                    width: '80px',
                                                    textAlign: 'right',
                                                }}
                                            >
                                                Кол-во
                                            </th>
                                            <th
                                                style={{
                                                    width: '120px',
                                                    textAlign: 'right',
                                                }}
                                            >
                                                Сумма
                                            </th>
                                            <th style={{ width: '50px' }}></th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {lines.map((line) => {
                                            const p = productById(line.productId);
                                            return (
                                                <tr key={line.productId}>
                                                    <td>
                                                        {p
                                                            ? p.name
                                                            : '(удалённый товар)'}
                                                    </td>
                                                    <td style={{ textAlign: 'right' }}>
                                                        {line.quantity}
                                                    </td>
                                                    <td
                                                        style={{
                                                            textAlign: 'right',
                                                            fontVariantNumeric:
                                                                'tabular-nums',
                                                        }}
                                                    >
                                                        {formatMoneyRub(
                                                            p ? p.price * line.quantity : 0
                                                        )}
                                                    </td>
                                                    <td>
                                                        <button
                                                            type="button"
                                                            className="btn-remove-icon"
                                                            onClick={() =>
                                                                handleRemove(line.productId)
                                                            }
                                                            disabled={locked || saving}
                                                            title="Удалить позицию"
                                                        >
                                                            ✕
                                                        </button>
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>

                        <div className="dialog-total-row">
                            <span className="dialog-total-label">
                                Итого: {formatMoneyRub(total)}
                            </span>
                        </div>

                        <div className="form-group">
                            <label className="form-label">Примечание</label>
                            <textarea
                                className="form-input"
                                rows={2}
                                value={notes}
                                onChange={(e) => setNotes(e.target.value)}
                                placeholder="Необязательное примечание к заказу"
                                disabled={locked || saving}
                                maxLength={NOTES_MAX}
                                style={{ resize: 'vertical', minHeight: 60 }}
                            />
                            <div
                                style={{
                                    fontSize: 11,
                                    color: 'var(--text-muted)',
                                    marginTop: 4,
                                    textAlign: 'right',
                                }}
                            >
                                {notes.length} / {NOTES_MAX}
                            </div>
                        </div>

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
                                type="button"
                                className="btn btn-primary"
                                onClick={handleSave}
                                disabled={saving || locked || lines.length === 0}
                            >
                                {saving
                                    ? 'Сохранение...'
                                    : isEditing
                                        ? 'Сохранить'
                                        : 'Создать заказ'}
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
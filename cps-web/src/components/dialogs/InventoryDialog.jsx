import { useState, useEffect } from 'react';
import { addInventory, updateInventory } from '../../api/inventory';
import { getProducts } from '../../api/products';

export default function InventoryDialog({ warehouse, inventoryItem, onClose, onSaved }) {
    const isEditing = !!inventoryItem;

    const [products, setProducts] = useState([]);
    const [productId, setProductId] = useState(inventoryItem?.productId || '');
    const [quantity, setQuantity] = useState(
        inventoryItem ? String(inventoryItem.quantity) : ''
    );
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (isEditing) return;
        async function loadProducts() {
            try {
                const data = await getProducts();
                setProducts(data);
            } catch (e) {
                setError('Не удалось загрузить товары');
            }
        }
        loadProducts();
    }, [isEditing]);

    async function handleSubmit(e) {
        e.preventDefault();
        if (!isEditing && !productId) {
            setError('Выберите товар');
            return;
        }
        if (quantity === '' || isNaN(Number(quantity)) || !Number.isInteger(Number(quantity)) || Number(quantity) < 0) {
            setError('Количество должно быть целым числом не меньше 0');
            return;
        }
        setError('');
        setSaving(true);
        try {
            if (isEditing) {
                await updateInventory(inventoryItem.id, Number(quantity));
            } else {
                await addInventory(productId, warehouse.id, Number(quantity));
            }
            onSaved();
        } catch (e) {
            setError(e.message || 'Не удалось сохранить остаток');
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="dialog-backdrop" onClick={onClose}>
            <div className="dialog-root" onClick={(e) => e.stopPropagation()}>
                <div className="dialog-header">
                    <h2 className="dialog-title">
                        {isEditing ? 'Изменить остаток' : 'Добавить товар на склад'}
                    </h2>
                    <button className="dialog-close" onClick={onClose} type="button">
                        ✕
                    </button>
                </div>

                <p style={{ marginTop: 0, marginBottom: 20, color: 'var(--text-muted)', fontSize: 14 }}>
                    Склад: <strong style={{ color: 'var(--text-main)' }}>{warehouse.name}</strong>
                </p>

                <form onSubmit={handleSubmit}>
                    {error && (
                        <div className="login-error" style={{ marginBottom: 12 }}>
                            {error}
                        </div>
                    )}

                    {isEditing ? (
                        <div className="form-group">
                            <label className="form-label">Товар</label>
                            <input
                                className="form-input"
                                value={inventoryItem.productName || 'Товар'}
                                disabled
                            />
                        </div>
                    ) : (
                        <div className="form-group">
                            <label className="form-label">Товар *</label>
                            <select
                                className="form-select"
                                value={productId}
                                onChange={(e) => setProductId(e.target.value)}
                                disabled={saving}
                            >
                                <option value="">— Выберите товар —</option>
                                {products.map((p) => (
                                    <option key={p.id} value={p.id}>
                                        {p.name} ({p.sku})
                                    </option>
                                ))}
                            </select>
                        </div>
                    )}

                    <div className="form-group">
                        <label className="form-label">Количество *</label>
                        <input
                            className="form-input"
                            type="number"
                            min="0"
                            step="1"
                            value={quantity}
                            onChange={(e) => setQuantity(e.target.value)}
                            placeholder="0"
                            disabled={saving}
                            autoFocus={isEditing}
                        />
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
                        <button type="submit" className="btn btn-primary" disabled={saving}>
                            {saving ? 'Сохранение...' : 'Сохранить'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
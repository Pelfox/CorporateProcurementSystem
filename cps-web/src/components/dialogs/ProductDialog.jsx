import { useState, useEffect } from 'react';
import { createProduct, updateProduct } from '../../api/products';
import { getCategories } from '../../api/categories';

// [A-Z0-9]{3,10}(-[A-Z0-9]{3,10})*
const SKU_PATTERN = /^[A-Z0-9]{3,10}(-[A-Z0-9]{3,10})*$/;
const PRICE_PATTERN = /^[+]?\d+(\.\d{1,2})?$/;

export default function ProductDialog({ product, onClose, onSaved }) {
    const isEditing = !!product;

    const [categories, setCategories] = useState([]);
    const [name, setName] = useState(product?.name || '');
    const [sku, setSku] = useState(product?.sku || '');
    const [categoryId, setCategoryId] = useState(product?.categoryId || '');
    const [price, setPrice] = useState(
        product ? String(product.price).replace('.', ',') : ''
    );
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        async function loadCategories() {
            try {
                const data = await getCategories();
                setCategories(data);
                // Если создание преселектим первую категорию
                if (!isEditing && data.length > 0) {
                    setCategoryId((prev) => prev || data[0].id);
                }
            } catch (e) {
                setError('Не удалось загрузить категории');
            }
        }
        loadCategories();
    }, [isEditing]);

    function validate() {
        if (!name.trim()) return 'Название товара не может быть пустым';
        if (name.trim().length > 255) return 'Название слишком длинное (макс. 255)';
        if (!sku.trim()) return 'Артикул не может быть пустым';
        if (!SKU_PATTERN.test(sku)) {
            return 'Артикул: только заглавные латинские буквы, цифры и дефис (от 3 символов)';
        }
        if (!categoryId) return 'Выберите категорию';
        if (!price) return 'Укажите цену';
        const normalizedPrice = price.replace(',', '.').trim();
        if (!PRICE_PATTERN.test(normalizedPrice)) {
            return 'Цена: положительное число, не более 2 знаков после запятой';
        }
        const n = Number(normalizedPrice);
        if (!(n > 0)) return 'Цена должна быть больше 0';
        if (n > 99999999.99) return 'Цена не должна превышать 99 999 999,99';
        return '';
    }

    async function handleSubmit(e) {
        e.preventDefault();
        const err = validate();
        if (err) {
            setError(err);
            return;
        }
        setError('');
        setSaving(true);

        const payload = {
            name: name.trim(),
            sku: sku.trim(),
            categoryId,
            price: price.replace(',', '.').trim(),
        };

        try {
            if (isEditing) {
                await updateProduct(product.id, payload);
            } else {
                await createProduct(payload);
            }
            onSaved();
        } catch (e) {
            setError(e.message || 'Не удалось сохранить товар');
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="dialog-backdrop" onClick={onClose}>
            <div className="dialog-root" onClick={(e) => e.stopPropagation()}>
                <div className="dialog-header">
                    <h2 className="dialog-title">{isEditing ? 'Редактирование товара' : 'Новый товар'}</h2>
                    <button className="dialog-close" onClick={onClose} type="button">
                        ✕
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    {error && <div className="login-error" style={{ marginBottom: 12 }}>{error}</div>}

                    <div className="form-group">
                        <label className="form-label">Название *</label>
                        <input
                            className="form-input"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="Введите название товара"
                            disabled={saving}
                            maxLength={255}
                            autoFocus
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Артикул *</label>
                        <input
                            className="form-input"
                            value={sku}
                            onChange={(e) => setSku(e.target.value.toUpperCase())}
                            placeholder="Например: NTB-001"
                            disabled={saving}
                            maxLength={40}
                            style={{ textTransform: 'uppercase' }}
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Категория *</label>
                        <select
                            className="form-select"
                            value={categoryId}
                            onChange={(e) => setCategoryId(e.target.value)}
                            disabled={saving}
                        >
                            <option value="">— Выберите категорию —</option>
                            {categories.map((c) => (
                                <option key={c.id} value={c.id}>
                                    {c.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label className="form-label">Цена (₽) *</label>
                        <input
                            className="form-input"
                            value={price}
                            onChange={(e) => setPrice(e.target.value)}
                            placeholder="0,00"
                            disabled={saving}
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
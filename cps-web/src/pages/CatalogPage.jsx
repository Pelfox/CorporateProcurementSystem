import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProducts, deleteProduct } from '../api/products';
import { getCategories } from '../api/categories';
import { formatMoneyRub } from '../utils/format';
import ProductDialog from '../components/dialogs/ProductDialog';
import ConfirmDeleteDialog from '../components/dialogs/ConfirmDeleteDialog';

const SORT_MODES = [
    { key: 'NAME_ASC', label: 'По названию (А→Я)' },
    { key: 'NAME_DESC', label: 'По названию (Я→А)' },
    { key: 'PRICE_ASC', label: 'По цене (сначала дешёвые)' },
    { key: 'PRICE_DESC', label: 'По цене (сначала дорогие)' },
];

function shortId(id) {
    return id ? id.substring(0, 8) : '';
}

export default function CatalogPage() {
    const navigate = useNavigate();

    const [products, setProducts] = useState([]);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const [search, setSearch] = useState('');
    const [categoryFilter, setCategoryFilter] = useState('');
    const [priceFrom, setPriceFrom] = useState('');
    const [priceTo, setPriceTo] = useState('');
    const [sortMode, setSortMode] = useState('NAME_ASC');

    const [selectedId, setSelectedId] = useState(null);
    const [showCreate, setShowCreate] = useState(false);
    const [showEdit, setShowEdit] = useState(false);
    const [showDelete, setShowDelete] = useState(false);
    const [actionError, setActionError] = useState('');

    async function load() {
        setLoading(true);
        setError('');
        try {
            const [prods, cats] = await Promise.all([getProducts(), getCategories()]);
            setProducts(prods);
            setCategories(cats);
        } catch (e) {
            setError(e.message || 'Не удалось загрузить товары');
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        load();
    }, []);

    const categoryNameById = useMemo(() => {
        const map = {};
        categories.forEach((c) => {
            map[c.id] = c.name;
        });
        return map;
    }, [categories]);

    const filtered = useMemo(() => {
        let list = products.slice();

        const q = search.trim().toLowerCase();
        if (q) {
            list = list.filter((p) => {
                return (
                    p.id.toLowerCase().includes(q) ||
                    p.name.toLowerCase().includes(q) ||
                    p.sku.toLowerCase().includes(q)
                );
            });
        }

        if (categoryFilter) {
            list = list.filter((p) => p.categoryId === categoryFilter);
        }

        const fromNum = priceFrom ? Number(priceFrom.replace(',', '.')) : null;
        if (fromNum != null && !isNaN(fromNum)) {
            list = list.filter((p) => p.price >= fromNum);
        }
        const toNum = priceTo ? Number(priceTo.replace(',', '.')) : null;
        if (toNum != null && !isNaN(toNum)) {
            list = list.filter((p) => p.price <= toNum);
        }

        list.sort((a, b) => {
            switch (sortMode) {
                case 'NAME_DESC':
                    return b.name.localeCompare(a.name, 'ru');
                case 'PRICE_ASC':
                    return a.price - b.price;
                case 'PRICE_DESC':
                    return b.price - a.price;
                case 'NAME_ASC':
                default:
                    return a.name.localeCompare(b.name, 'ru');
            }
        });

        return list;
    }, [products, search, categoryFilter, priceFrom, priceTo, sortMode]);

    const selected = products.find((p) => p.id === selectedId);

    async function handleDelete() {
        if (!selected) return;
        setActionError('');
        try {
            await deleteProduct(selected.id);
            setShowDelete(false);
            setSelectedId(null);
            await load();
        } catch (e) {
            setActionError(e.message || 'Не удалось удалить товар');
            setShowDelete(false);
        }
    }

    function handleSaved() {
        setShowCreate(false);
        setShowEdit(false);
        load();
    }

    return (
        <div className="page-content page-content--fixed">
            <div className="page-header">
                <button className="back-btn" onClick={() => navigate('/services')}>
                    ←
                </button>
                <h1 className="page-title">Каталог товаров</h1>
            </div>

            <div className="content-block">
                <div className="search-row">
                    <input
                        className="search-input"
                        placeholder="Поиск по названию или артикулу..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        style={{ flex: 1, minWidth: 240 }}
                    />
                    <select
                        className="filter-select"
                        value={categoryFilter}
                        onChange={(e) => setCategoryFilter(e.target.value)}
                    >
                        <option value="">Все категории</option>
                        {categories.map((c) => (
                            <option key={c.id} value={c.id}>
                                {c.name}
                            </option>
                        ))}
                    </select>
                    <input
                        className="filter-input"
                        placeholder="Цена от"
                        value={priceFrom}
                        onChange={(e) => setPriceFrom(e.target.value)}
                    />
                    <input
                        className="filter-input"
                        placeholder="Цена до"
                        value={priceTo}
                        onChange={(e) => setPriceTo(e.target.value)}
                    />
                    <select
                        className="filter-select"
                        value={sortMode}
                        onChange={(e) => setSortMode(e.target.value)}
                    >
                        {SORT_MODES.map((m) => (
                            <option key={m.key} value={m.key}>
                                {m.label}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="action-row" style={{ marginTop: 16 }}>
                    <button className="btn btn-add" onClick={() => setShowCreate(true)}>
                        Добавить
                    </button>
                    <button
                        className="btn btn-edit"
                        onClick={() => setShowEdit(true)}
                        disabled={!selected}
                    >
                        Редактировать
                    </button>
                    <button
                        className="btn btn-delete"
                        onClick={() => setShowDelete(true)}
                        disabled={!selected}
                    >
                        Удалить
                    </button>
                </div>

                {actionError && (
                    <div className="form-error" style={{ marginTop: 10 }}>
                        {actionError}
                    </div>
                )}
            </div>

            <div className="content-block table-container">
                {loading && (
                    <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--text-muted)' }}>
                        Загрузка...
                    </div>
                )}

                {!loading && error && (
                    <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--danger)' }}>
                        {error}
                    </div>
                )}

                {!loading && !error && filtered.length === 0 && (
                    <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--text-muted)' }}>
                        Товары не найдены
                    </div>
                )}

                {!loading && !error && filtered.length > 0 && (
                    <div className="table-scroll">
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th style={{ width: '100px' }}>ID</th>
                                <th>Название</th>
                                <th style={{ width: '160px' }}>Артикул</th>
                                <th style={{ width: '140px', textAlign: 'right' }}>Цена</th>
                                <th style={{ width: '200px' }}>Категория</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filtered.map((p) => (
                                <tr
                                    key={p.id}
                                    className={selectedId === p.id ? 'selected' : ''}
                                    onClick={() => setSelectedId(p.id)}
                                >
                                    <td style={{ fontFamily: 'monospace' }}>{shortId(p.id)}</td>
                                    <td>{p.name}</td>
                                    <td style={{ fontFamily: 'monospace' }}>{p.sku}</td>
                                    <td style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
                                        {formatMoneyRub(p.price)}
                                    </td>
                                    <td>{categoryNameById[p.categoryId] || '—'}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {showCreate && (
                <ProductDialog product={null} onClose={() => setShowCreate(false)} onSaved={handleSaved} />
            )}

            {showEdit && selected && (
                <ProductDialog product={selected} onClose={() => setShowEdit(false)} onSaved={handleSaved} />
            )}

            {showDelete && selected && (
                <ConfirmDeleteDialog
                    title="Удаление товара"
                    entityName={selected.name}
                    onCancel={() => setShowDelete(false)}
                    onConfirm={handleDelete}
                />
            )}
        </div>
    );
}
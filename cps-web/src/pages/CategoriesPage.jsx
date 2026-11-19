import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCategories, deleteCategory } from '../api/categories';
import { getProducts } from '../api/products';
import { pluralProducts } from '../utils/plural';
import CategoryDialog from '../components/dialogs/CategoryDialog';
import ConfirmDeleteDialog from '../components/dialogs/ConfirmDeleteDialog';

const COLORS = [
    'cat-purple',
    'cat-orange',
    'cat-teal',
    'cat-green',
    'cat-indigo',
    'cat-cyan',
    'cat-red',
    'cat-blue',
    'cat-rose',
    'cat-amber',
];

export default function CategoriesPage() {
    const navigate = useNavigate();

    const [categories, setCategories] = useState([]);
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const [search, setSearch] = useState('');
    const [selectedId, setSelectedId] = useState(null);

    const [showCreate, setShowCreate] = useState(false);
    const [showEdit, setShowEdit] = useState(false);
    const [showDelete, setShowDelete] = useState(false);
    const [actionError, setActionError] = useState('');

    async function load() {
        setLoading(true);
        setError('');
        try {
            const [cats, prods] = await Promise.all([getCategories(), getProducts()]);
            setCategories(cats);
            setProducts(prods);
        } catch (e) {
            setError(e.message || 'Не удалось загрузить категории');
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        load();
    }, []);

    const countByCategory = useMemo(() => {
        const map = {};
        products.forEach((p) => {
            if (p.categoryId) {
                map[p.categoryId] = (map[p.categoryId] || 0) + 1;
            }
        });
        return map;
    }, [products]);

    const filtered = useMemo(() => {
        const q = search.trim().toLowerCase();
        if (!q) return categories;
        return categories.filter((c) => c.name.toLowerCase().includes(q));
    }, [categories, search]);

    const selected = categories.find((c) => c.id === selectedId);

    async function handleDelete() {
        if (!selected) return;
        setActionError('');
        try {
            await deleteCategory(selected.id);
            setShowDelete(false);
            setSelectedId(null);
            await load();
        } catch (e) {
            setActionError(e.message || 'Не удалось удалить категорию');
            setShowDelete(false);
        }
    }

    function handleSaved() {
        setShowCreate(false);
        setShowEdit(false);
        load();
    }

    function handleCardClick(id, event) {
        setSelectedId(id);
        if (event.detail === 2) {
            // двойной клик — редактирование
            setShowEdit(true);
        }
    }

    return (
        <div className="page-content page-content--fixed">
            <div className="page-header">
                <button className="back-btn" onClick={() => navigate('/services')}>
                    ←
                </button>
                <h1 className="page-title">Категории</h1>
            </div>

            <div className="content-block">
                <div className="search-row">
                    <input
                        className="search-input"
                        placeholder="Поиск по названию категории..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        style={{ flex: 1, minWidth: 240 }}
                    />
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

            <div className="content-block categories-container">
                {loading && (
                    <div style={{ textAlign: 'center', width: '100%', padding: '40px 0', color: 'var(--text-muted)' }}>
                        Загрузка...
                    </div>
                )}

                {!loading && error && (
                    <div style={{ textAlign: 'center', width: '100%', padding: '40px 0', color: 'var(--danger)' }}>
                        {error}
                    </div>
                )}

                {!loading && !error && filtered.length === 0 && (
                    <div className="empty-categories" style={{ width: '100%' }}>
                        Категории не найдены
                    </div>
                )}

                {!loading && !error && filtered.length > 0 && (
                    <div className="categories-block">
                        <div className="categories-grid">
                            {filtered.map((c, index) => {
                                const colorClass = COLORS[index % COLORS.length];
                                const count = countByCategory[c.id] || 0;
                                return (
                                    <div
                                        key={c.id}
                                        className={
                                            'category-card ' +
                                            colorClass +
                                            (selectedId === c.id ? ' selected' : '')
                                        }
                                        onClick={(e) => handleCardClick(c.id, e)}
                                    >
                                        <div className="category-card-title">{c.name}</div>
                                        <div className="category-card-count">
                                            {pluralProducts(count)}
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}
            </div>

            {showCreate && (
                <CategoryDialog
                    category={null}
                    onClose={() => setShowCreate(false)}
                    onSaved={handleSaved}
                />
            )}

            {showEdit && selected && (
                <CategoryDialog
                    category={selected}
                    onClose={() => setShowEdit(false)}
                    onSaved={handleSaved}
                />
            )}

            {showDelete && selected && (
                <ConfirmDeleteDialog
                    title="Удаление категории"
                    entityName={selected.name}
                    onCancel={() => setShowDelete(false)}
                    onConfirm={handleDelete}
                />
            )}
        </div>
    );
}
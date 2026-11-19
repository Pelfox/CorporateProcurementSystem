import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { getWarehouses, deleteWarehouse } from '../api/warehouses';
import { getInventoryByWarehouse, deleteInventory } from '../api/inventory';
import { getProducts } from '../api/products';
import WarehouseDialog from '../components/dialogs/WarehouseDialog';
import InventoryDialog from '../components/dialogs/InventoryDialog';
import ConfirmDeleteDialog from '../components/dialogs/ConfirmDeleteDialog';

export default function WarehousesPage() {
    const navigate = useNavigate();

    const [warehouses, setWarehouses] = useState([]);
    const [products, setProducts] = useState([]);
    const [inventory, setInventory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const [search, setSearch] = useState('');
    const [selectedWarehouseId, setSelectedWarehouseId] = useState(null);
    const [selectedInventoryId, setSelectedInventoryId] = useState(null);

    const [showCreateWarehouse, setShowCreateWarehouse] = useState(false);
    const [showEditWarehouse, setShowEditWarehouse] = useState(false);
    const [showDeleteWarehouse, setShowDeleteWarehouse] = useState(false);
    const [showAddInventory, setShowAddInventory] = useState(false);
    const [showEditInventory, setShowEditInventory] = useState(false);
    const [showDeleteInventory, setShowDeleteInventory] = useState(false);
    const [actionError, setActionError] = useState('');

    async function loadBase() {
        setLoading(true);
        setError('');
        try {
            const [whs, prods] = await Promise.all([getWarehouses(), getProducts()]);
            setWarehouses(whs);
            setProducts(prods);
            // Автовыбор первого склада, если ни один не выбран
            if (whs.length > 0 && !selectedWarehouseId) {
                setSelectedWarehouseId(whs[0].id);
            }
        } catch (e) {
            setError(e.message || 'Не удалось загрузить данные');
        } finally {
            setLoading(false);
        }
    }

    async function loadInventory(warehouseId) {
        if (!warehouseId) {
            setInventory([]);
            return;
        }
        try {
            const data = await getInventoryByWarehouse(warehouseId);
            setInventory(data);
        } catch (e) {
            setInventory([]);
        }
    }

    useEffect(() => {
        loadBase();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        setSelectedInventoryId(null);
        loadInventory(selectedWarehouseId);
    }, [selectedWarehouseId]);

    const productById = useMemo(() => {
        const map = {};
        products.forEach((p) => {
            map[p.id] = p;
        });
        return map;
    }, [products]);

    const filteredWarehouses = useMemo(() => {
        const q = search.trim().toLowerCase();
        if (!q) return warehouses;
        return warehouses.filter((w) => {
            const name = (w.name || '').toLowerCase();
            const loc = (w.location || '').toLowerCase();
            return name.includes(q) || loc.includes(q);
        });
    }, [warehouses, search]);

    const selectedWarehouse = warehouses.find((w) => w.id === selectedWarehouseId);
    const selectedInventory = inventory.find((i) => i.id === selectedInventoryId);

    async function handleDeleteWarehouse() {
        if (!selectedWarehouse) return;
        setActionError('');
        try {
            await deleteWarehouse(selectedWarehouse.id);
            setShowDeleteWarehouse(false);
            setSelectedWarehouseId(null);
            await loadBase();
        } catch (e) {
            setActionError(e.message || 'Не удалось удалить склад');
            setShowDeleteWarehouse(false);
        }
    }

    async function handleDeleteInventory() {
        if (!selectedInventory) return;
        setActionError('');
        try {
            await deleteInventory(selectedInventory.id);
            setShowDeleteInventory(false);
            setSelectedInventoryId(null);
            await loadInventory(selectedWarehouseId);
        } catch (e) {
            setActionError(e.message || 'Не удалось удалить остаток');
            setShowDeleteInventory(false);
        }
    }

    async function handleWarehouseSaved() {
        setShowCreateWarehouse(false);
        setShowEditWarehouse(false);
        await loadBase();
    }

    async function handleInventorySaved() {
        setShowAddInventory(false);
        setShowEditInventory(false);
        await loadInventory(selectedWarehouseId);
    }

    const productLabelFor = (item) => {
        const p = productById[item.productId];
        return p ? p.name : '(удалённый товар)';
    };

    const skuFor = (item) => {
        const p = productById[item.productId];
        return p ? p.sku : '—';
    };

    return (
        <div className="page-content page-content--fixed">
            <div className="page-header">
                <button className="back-btn" onClick={() => navigate('/services')}>
                    ←
                </button>
                <h1 className="page-title">Склады и остатки</h1>
            </div>

            <div className="content-block">
                <div className="search-row">
                    <input
                        className="search-input"
                        placeholder="Поиск склада по названию или адресу..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        style={{ flex: 1, minWidth: 240 }}
                    />
                </div>
                <div className="action-row" style={{ marginTop: 16 }}>
                    <button
                        className="btn btn-success"
                        onClick={() => setShowCreateWarehouse(true)}
                    >
                        Создать склад
                    </button>
                    <button
                        className="btn btn-edit"
                        onClick={() => setShowEditWarehouse(true)}
                        disabled={!selectedWarehouse}
                    >
                        Редактировать склад
                    </button>
                    <button
                        className="btn btn-primary"
                        onClick={() => setShowAddInventory(true)}
                        disabled={!selectedWarehouse}
                    >
                        Добавить товар на склад
                    </button>
                    <button
                        className="btn btn-warning"
                        onClick={() => setShowEditInventory(true)}
                        disabled={!selectedInventory}
                    >
                        Изменить остаток
                    </button>
                    <button
                        className="btn btn-danger"
                        onClick={() => setShowDeleteWarehouse(true)}
                        disabled={!selectedWarehouse}
                    >
                        Удалить склад
                    </button>
                </div>

                {actionError && (
                    <div className="form-error" style={{ marginTop: 10 }}>
                        {actionError}
                    </div>
                )}
            </div>

            {loading && (
                <div className="content-block" style={{ textAlign: 'center', padding: '40px 0', color: 'var(--text-muted)' }}>
                    Загрузка...
                </div>
            )}

            {!loading && error && (
                <div className="content-block" style={{ textAlign: 'center', padding: '40px 0', color: 'var(--danger)' }}>
                    {error}
                </div>
            )}

            {!loading && !error && (
                <div className="warehouses-layout">
                    <div className="warehouses-list-panel">
                        <div className="warehouses-panel-title">Склады</div>

                        {filteredWarehouses.length === 0 && (
                            <div className="empty-panel" style={{ padding: 20 }}>
                                Склады не найдены
                            </div>
                        )}

                        <div className="warehouse-items">
                            {filteredWarehouses.map((w) => (
                                <div
                                    key={w.id}
                                    className={
                                        'warehouse-item' +
                                        (selectedWarehouseId === w.id ? ' selected' : '')
                                    }
                                    onClick={() => setSelectedWarehouseId(w.id)}
                                >
                                    <div className="warehouse-item-name">{w.name}</div>
                                    <div className="warehouse-item-address">
                                        {w.location || 'Адрес не указан'}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="inventory-panel">
                        <div className="warehouses-panel-title">
                            Остатки{selectedWarehouse ? ` — ${selectedWarehouse.name}` : ''}
                        </div>

                        {!selectedWarehouse && (
                            <div className="empty-panel">Выберите склад слева</div>
                        )}

                        {selectedWarehouse && inventory.length === 0 && (
                            <div className="empty-panel">Нет данных по остаткам</div>
                        )}

                        {selectedWarehouse && inventory.length > 0 && (
                            <div className="table-container">
                                <div className="table-scroll">
                                    <table className="data-table">
                                        <thead>
                                        <tr>
                                            <th>Товар</th>
                                            <th style={{ width: '180px' }}>Артикул</th>
                                            <th style={{ width: '130px', textAlign: 'right' }}>
                                                Количество
                                            </th>
                                            <th style={{ width: '60px' }}></th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {inventory.map((item) => (
                                            <tr
                                                key={item.id}
                                                className={
                                                    selectedInventoryId === item.id ? 'selected' : ''
                                                }
                                                onClick={() => setSelectedInventoryId(item.id)}
                                            >
                                                <td>{productLabelFor(item)}</td>
                                                <td style={{ fontFamily: 'monospace' }}>
                                                    {skuFor(item)}
                                                </td>
                                                <td style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
                                                    {item.quantity}
                                                </td>
                                                <td>
                                                    <button
                                                        className="row-delete-btn"
                                                        title="Удалить остаток"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            setSelectedInventoryId(item.id);
                                                            setShowDeleteInventory(true);
                                                        }}
                                                    >
                                                        ✕
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {showCreateWarehouse && (
                <WarehouseDialog
                    warehouse={null}
                    onClose={() => setShowCreateWarehouse(false)}
                    onSaved={handleWarehouseSaved}
                />
            )}

            {showEditWarehouse && selectedWarehouse && (
                <WarehouseDialog
                    warehouse={selectedWarehouse}
                    onClose={() => setShowEditWarehouse(false)}
                    onSaved={handleWarehouseSaved}
                />
            )}

            {showAddInventory && selectedWarehouse && (
                <InventoryDialog
                    warehouse={selectedWarehouse}
                    inventoryItem={null}
                    onClose={() => setShowAddInventory(false)}
                    onSaved={handleInventorySaved}
                />
            )}

            {showEditInventory && selectedWarehouse && selectedInventory && (
                <InventoryDialog
                    warehouse={selectedWarehouse}
                    inventoryItem={{
                        ...selectedInventory,
                        productName: productLabelFor(selectedInventory),
                    }}
                    onClose={() => setShowEditInventory(false)}
                    onSaved={handleInventorySaved}
                />
            )}

            {showDeleteWarehouse && selectedWarehouse && (
                <ConfirmDeleteDialog
                    title="Удаление склада"
                    entityName={selectedWarehouse.name}
                    onCancel={() => setShowDeleteWarehouse(false)}
                    onConfirm={handleDeleteWarehouse}
                />
            )}

            {showDeleteInventory && selectedInventory && (
                <ConfirmDeleteDialog
                    title="Удаление остатка"
                    entityName={productLabelFor(selectedInventory)}
                    onCancel={() => setShowDeleteInventory(false)}
                    onConfirm={handleDeleteInventory}
                />
            )}
        </div>
    );
}
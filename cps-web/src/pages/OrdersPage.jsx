import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { getAllOrders, deleteOrder } from '../api/orders';
import { ORDER_STATUSES, statusLabel, statusCss } from '../utils/orderStatus';
import { formatDateTime, formatMoneyRub, formatMoney } from '../utils/format';
import OrderNotesDialog from '../components/dialogs/OrderNotesDialog';
import OrderStatusDialog from '../components/dialogs/OrderStatusDialog';
import ConfirmDeleteDialog from '../components/dialogs/ConfirmDeleteDialog';
import OrderEditorDialog from '../components/dialogs/OrderEditorDialog';

const SORT_MODES = [
    { key: 'CREATED_DESC', label: 'Сначала новые' },
    { key: 'CREATED_ASC', label: 'Сначала старые' },
    { key: 'TOTAL_ASC', label: 'По сумме ↑' },
    { key: 'TOTAL_DESC', label: 'По сумме ↓' },
];

function shortId(id) {
    return id ? id.substring(0, 8) : '';
}

export default function OrdersPage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [search, setSearch] = useState('');
    const [status, setStatus] = useState('');
    const [userFilter, setUserFilter] = useState('');
    const [priceFrom, setPriceFrom] = useState('');
    const [priceTo, setPriceTo] = useState('');
    const [sortMode, setSortMode] = useState('CREATED_DESC');
    const [selectedId, setSelectedId] = useState(null);
    const [showNotes, setShowNotes] = useState(false);
    const [showStatus, setShowStatus] = useState(false);
    const [showDelete, setShowDelete] = useState(false);
    const [actionError, setActionError] = useState('');
    const [infoMessage, setInfoMessage] = useState('');
    const [showEditor, setShowEditor] = useState(false);
    const [editingOrder, setEditingOrder] = useState(null);

    async function load() {
        setLoading(true);
        setError('');
        try {
            const data = await getAllOrders();
            setOrders(data);
        } catch (e) {
            setError(e.message || 'Не удалось загрузить заказы');
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        load();
    }, []);

    // Уникальные пользователи из заказов (для фильтра)
    const usersInOrders = useMemo(() => {
        const seen = new Map();
        orders.forEach((o) => {
            if (o.userId && !seen.has(o.userId)) {
                seen.set(o.userId, { id: o.userId, username: o.username });
            }
        });
        return Array.from(seen.values()).sort((a, b) =>
            (a.username || '').localeCompare(b.username || '', 'ru')
        );
    }, [orders]);

    const filtered = useMemo(() => {
        let list = orders.slice();
        const q = search.trim().toLowerCase();
        if (q) {
            list = list.filter((o) => {
                const idMatch = o.id.toLowerCase().includes(q);
                const notesMatch = (o.notes || '').toLowerCase().includes(q);
                return idMatch || notesMatch;
            });
        }
        if (status) {
            list = list.filter((o) => o.status === status);
        }
        if (userFilter) {
            list = list.filter((o) => o.userId === userFilter);
        }
        const fromNum = priceFrom ? Number(priceFrom.replace(',', '.')) : null;
        if (fromNum != null && !isNaN(fromNum)) {
            list = list.filter((o) => o.total >= fromNum);
        }
        const toNum = priceTo ? Number(priceTo.replace(',', '.')) : null;
        if (toNum != null && !isNaN(toNum)) {
            list = list.filter((o) => o.total <= toNum);
        }
        list.sort((a, b) => {
            switch (sortMode) {
                case 'CREATED_ASC':
                    return new Date(a.createdAt) - new Date(b.createdAt);
                case 'TOTAL_ASC':
                    return a.total - b.total;
                case 'TOTAL_DESC':
                    return b.total - a.total;
                case 'CREATED_DESC':
                default:
                    return new Date(b.createdAt) - new Date(a.createdAt);
            }
        });
        return list;
    }, [orders, search, status, userFilter, priceFrom, priceTo, sortMode]);

    const selected = orders.find((o) => o.id === selectedId);

    function handleOpenNotes() {
        setActionError('');
        if (selected.userId !== user.id) {
            setActionError('Можно редактировать примечание только в своих заказах');
            return;
        }
        setShowNotes(true);
    }

    function handleOpenFullEdit() {
        setActionError('');
        if (!selected) return;
        if (selected.userId !== user.id) { setActionError('Состав заказа может менять только его владелец'); return; }
        if (selected.status !== 'CREATED') {
            setActionError('Редактировать можно только созданный заказ');
            return;
        }
        setEditingOrder(selected);
        setShowEditor(true);
    }

    function handleOpenStatus() {
        setActionError('');
        if (!selected) return;
        if (selected.status === 'DELIVERED') {
            setActionError('Доставленный заказ нельзя перевести в другой статус');
            return;
        }
        setShowStatus(true);
    }

    async function handleDelete() {
        if (!selected) return;
        setActionError('');
        try {
            await deleteOrder(selected.id);
            setShowDelete(false);
            setSelectedId(null);
            setInfoMessage('Заказ удалён');
            setTimeout(() => setInfoMessage(''), 4000);
            await load();
        } catch (e) {
            setActionError(e.message || 'Не удалось удалить заказ');
            setShowDelete(false);
        }
    }

    function handleSaved() {
        setShowNotes(false);
        setShowStatus(false);
        load();
    }

    function handleCreateClick() {
        setEditingOrder(null);
        setShowEditor(true);
    }

    function singlePrice(order) {
        if (!order.items || order.items.length !== 1) return '—';
        return formatMoneyRub(order.items[0].purchasePrice);
    }

    return (
        <div className="page-content page-content--fixed">
            <div className="page-header">
                <button className="back-btn" onClick={() => navigate('/services')}>
                    Назад
                </button>
                <h1 className="page-title">Заказы</h1>
            </div>
            {infoMessage && (
                <div
                    style={{
                        background: '#eff6ff',
                        border: '1px solid #bfdbfe',
                        color: '#1e40af',
                        padding: '12px 16px',
                        borderRadius: 10,
                        fontSize: 13,
                    }}
                >
                    {infoMessage}
                </div>
            )}
            <div className="content-block">
                <div className="search-row">
                    <input
                        className="search-input"
                        placeholder="Поиск по номеру или примечанию..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        style={{ flex: 1, minWidth: 220 }}
                    />
                    <select
                        className="filter-select"
                        value={status}
                        onChange={(e) => setStatus(e.target.value)}
                    >
                        <option value="">Все статусы</option>
                        {ORDER_STATUSES.map((s) => (
                            <option key={s} value={s}>
                                {statusLabel(s)}
                            </option>
                        ))}
                    </select>
                    <select
                        className="filter-select"
                        value={userFilter}
                        onChange={(e) => setUserFilter(e.target.value)}
                    >
                        <option value="">Все пользователи</option>
                        {usersInOrders.map((u) => (
                            <option key={u.id} value={u.id}>
                                {u.username}
                            </option>
                        ))}
                    </select>
                    <input
                        className="filter-input"
                        placeholder="Сумма от"
                        value={priceFrom}
                        onChange={(e) => setPriceFrom(e.target.value)}
                    />
                    <input
                        className="filter-input"
                        placeholder="Сумма до"
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
                    <button className="btn btn-create" onClick={handleCreateClick}>
                        Создать заказ
                    </button>
                    <button
                        className="btn btn-edit"
                        onClick={handleOpenFullEdit}
                        disabled={!selected}
                        title="Редактировать состав заказа"
                    >
                        Редактировать
                    </button>
                    <button
                        className="btn btn-edit"
                        onClick={handleOpenNotes}
                        disabled={!selected}
                    >
                        Редактировать примечание
                    </button>
                    <button
                        className="btn btn-warning"
                        onClick={handleOpenStatus}
                        disabled={!selected || selected.status === 'DELIVERED'}
                        title={selected?.status === 'DELIVERED' ? 'Доставленный заказ нельзя изменить' : ''}
                    >
                        Сменить статус
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
                        Заказы не найдены
                    </div>
                )}
                {!loading && !error && filtered.length > 0 && (
                    <div className="table-scroll">
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th style={{ width: '80px' }}>№</th>
                                <th style={{ width: '140px' }}>Пользователь</th>
                                <th style={{ width: '130px' }}>Статус</th>
                                <th style={{ width: '150px' }}>Дата создания</th>
                                <th style={{ width: '100px', textAlign: 'right' }}>Количество</th>
                                <th style={{ width: '140px', textAlign: 'right' }}>Цена за 1 товар</th>
                                <th style={{ width: '130px', textAlign: 'right' }}>Сумма</th>
                                <th>Примечание</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filtered.map((o) => (
                                <tr
                                    key={o.id}
                                    className={selectedId === o.id ? 'selected' : ''}
                                    onClick={() => setSelectedId(o.id)}
                                >
                                    <td style={{ fontFamily: 'monospace' }}>{shortId(o.id)}</td>
                                    <td>{o.username}</td>
                                    <td>
                                        <span className={'status ' + statusCss(o.status)}>
                                            {statusLabel(o.status)}
                                        </span>
                                    </td>
                                    <td>{formatDateTime(o.createdAt)}</td>
                                    <td style={{ textAlign: 'right' }}>{o.itemsCount}</td>
                                    <td style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
                                        {singlePrice(o)}
                                    </td>
                                    <td style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
                                        {formatMoneyRub(o.total)}
                                    </td>
                                    <td>{o.notes || ''}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
            {showNotes && selected && (
                <OrderNotesDialog
                    order={selected}
                    currentUserId={user.id}
                    onClose={() => setShowNotes(false)}
                    onSaved={handleSaved}
                />
            )}
            {showStatus && selected && (
                <OrderStatusDialog
                    order={selected}
                    currentUserId={user.id}
                    onClose={() => setShowStatus(false)}
                    onSaved={handleSaved}
                />
            )}
            {showDelete && selected && (
                <ConfirmDeleteDialog
                    title="Удаление заказа"
                    entityName={`Заказ ${shortId(selected.id)}`}
                    onCancel={() => setShowDelete(false)}
                    onConfirm={handleDelete}
                />
            )}
            {showEditor && (
                <OrderEditorDialog
                    order={editingOrder}
                    currentUser={user}
                    onClose={() => {
                        setShowEditor(false);
                        setEditingOrder(null);
                    }}
                    onSaved={() => {
                        setShowEditor(false);
                        setEditingOrder(null);
                        load();
                    }}
                />
            )}
        </div>
    );
}
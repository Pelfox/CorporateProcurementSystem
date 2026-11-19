import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getMyOrders } from '../api/orders';
import { ORDER_STATUSES, statusLabel, statusCss } from '../utils/orderStatus';
import { formatDateTime, formatMoneyRub } from '../utils/format';
import OrderEditorDialog from '../components/dialogs/OrderEditorDialog';

const SORT_MODES = [
    { key: 'newest', label: 'Сначала новые' },
    { key: 'oldest', label: 'Сначала старые' },
];

function shortId(id) {
    return id ? id.substring(0, 8) : '';
}

export default function MyOrdersPage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [search, setSearch] = useState('');
    const [status, setStatus] = useState('');
    const [dateFrom, setDateFrom] = useState('');
    const [dateTo, setDateTo] = useState('');
    const [sortMode, setSortMode] = useState('newest');
    const [selectedId, setSelectedId] = useState(null);
    const [showEditor, setShowEditor] = useState(false);
    const [editingOrder, setEditingOrder] = useState(null);
    const [actionError, setActionError] = useState('');

    async function load() {
        setLoading(true);
        setError('');
        try {
            const data = await getMyOrders(user?.id);
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

    const dateError = useMemo(() => {
        if (dateFrom && dateTo && dateFrom > dateTo) {
            return 'Дата «от» не должна быть позже даты «до».';
        }
        return '';
    }, [dateFrom, dateTo]);

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
        if (dateFrom) {
            const from = new Date(dateFrom + 'T00:00:00');
            list = list.filter((o) => new Date(o.createdAt) >= from);
        }
        if (dateTo) {
            const to = new Date(dateTo + 'T23:59:59');
            list = list.filter((o) => new Date(o.createdAt) <= to);
        }
        list.sort((a, b) => {
            const da = new Date(a.createdAt).getTime();
            const db = new Date(b.createdAt).getTime();
            return sortMode === 'newest' ? db - da : da - db;
        });
        return list;
    }, [orders, search, status, dateFrom, dateTo, sortMode]);

    const selected = orders.find((o) => o.id === selectedId);

    function handleCreateClick() {
        setActionError('');
        setEditingOrder(null);
        setShowEditor(true);
    }

    function handleOpenEdit() {
        setActionError('');
        if (!selected) return;
        if (selected.status !== 'CREATED') {
            setActionError('Редактировать можно только созданный заказ');
            return;
        }
        setEditingOrder(selected);
        setShowEditor(true);
    }

    function handleClearDates() {
        setDateFrom('');
        setDateTo('');
    }

    return (
        <div className="page-content page-content--fixed">
            <h1 className="page-title">Мои заказы</h1>
            <div className="welcome-banner">
                <h2 className="welcome-title">Здравствуйте, {user?.username}!</h2>
                <p className="welcome-subtitle">
                    Здесь собраны все ваши заказы. Вы можете отслеживать их статус и повторять прошлые покупки.
                </p>
            </div>

            {/* Блок с фильтрами и кнопками */}
            <div className="content-block">
                <div className="search-row">
                    <input
                        className="search-input"
                        placeholder="Поиск по номеру или примечанию..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        style={{ flex: 1, minWidth: 240 }}
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
                    <input
                        type="date"
                        className="filter-date"
                        value={dateFrom}
                        onChange={(e) => setDateFrom(e.target.value)}
                    />
                    <span className="date-separator">—</span>
                    <input
                        type="date"
                        className="filter-date"
                        value={dateTo}
                        onChange={(e) => setDateTo(e.target.value)}
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
                    <button
                        className="btn btn-cancel"
                        onClick={handleClearDates}
                        disabled={!dateFrom && !dateTo}
                    >
                        Сбросить даты
                    </button>
                </div>

                {dateError && (
                    <span
                        className="form-error"
                        style={{ display: 'block', marginTop: 10 }}
                    >
                        {dateError}
                    </span>
                )}

                <div className="action-row" style={{ marginTop: 16 }}>
                    <button className="btn btn-create" onClick={handleCreateClick}>
                        Создать заказ
                    </button>
                    <button
                        className="btn btn-edit"
                        onClick={handleOpenEdit}
                        disabled={!selected}
                    >
                        Редактировать
                    </button>
                </div>

                {actionError && (
                    <div className="form-error" style={{ marginTop: 10 }}>
                        {actionError}
                    </div>
                )}
            </div>

            {/* Таблица заказов */}
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
                                <th style={{ width: '70px' }}>№</th>
                                <th style={{ width: '150px' }}>Дата</th>
                                <th style={{ width: '130px' }}>Статус</th>
                                <th style={{ width: '100px' }}>Позиций</th>
                                <th style={{ width: '150px' }}>Обновлён</th>
                                <th style={{ width: '600px' }}>Примечание</th>
                                <th style={{ width: '140px', textAlign: 'left' }}>Сумма</th>
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
                                    <td>{formatDateTime(o.createdAt)}</td>
                                    <td>
                                        <span className={'status ' + statusCss(o.status)}>
                                            {statusLabel(o.status)}
                                        </span>
                                    </td>
                                    <td>{o.itemsCount}</td>
                                    <td>{formatDateTime(o.updatedAt)}</td>
                                    <td>{o.notes || ''}</td>
                                    <td style={{ textAlign: 'left', fontVariantNumeric: 'tabular-nums' }}>
                                        {formatMoneyRub(o.total)}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

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
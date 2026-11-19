import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getUsers, setUserStatus } from '../api/users';
import UserCreateDialog from '../components/dialogs/UserCreateDialog';
import ResetPasswordDialog from '../components/dialogs/ResetPasswordDialog';

const ROLE_LABELS = {
    USER: 'Пользователь',
    MANAGER: 'Менеджер',
    ADMIN: 'Администратор',
};

function formatDate(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function UsersPage() {
    const { user: currentUser } = useAuth();
    const navigate = useNavigate();

    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [search, setSearch] = useState('');
    const [tab, setTab] = useState('ALL');
    const [selectedId, setSelectedId] = useState(null);
    const [showCreate, setShowCreate] = useState(false);
    const [showReset, setShowReset] = useState(false);
    const [actionError, setActionError] = useState('');

    async function load() {
        setLoading(true);
        setError('');
        try {
            const data = await getUsers();
            setUsers(data);
        } catch (e) {
            setError(e.message || 'Не удалось загрузить пользователей');
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        load();
    }, []);

    const filtered = useMemo(() => {
        let list = users;
        if (tab !== 'ALL') list = list.filter((u) => u.role === tab);
        const q = search.trim().toLowerCase();
        if (q) list = list.filter((u) => u.username.toLowerCase().includes(q));
        return list;
    }, [users, tab, search]);

    const selected = users.find((u) => u.id === selectedId);
    const isSelf = !!(selected && currentUser && selected.id === currentUser.id);

    async function handleToggle() {
        if (!selected || isSelf) return;
        setActionError('');
        try {
            const updated = await setUserStatus(selected.id, !selected.enabled);
            setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
        } catch (e) {
            setActionError(e.message || 'Не удалось изменить статус');
        }
    }

    function handleCreated() {
        setShowCreate(false);
        load();
    }

    function handleResetDone() {
        setShowReset(false);
    }

    const tabs = [
        { key: 'ALL', label: 'Все' },
        { key: 'USER', label: 'Пользователи' },
        { key: 'MANAGER', label: 'Менеджеры' },
        { key: 'ADMIN', label: 'Администраторы' },
    ];

    return (
        <div className="page-content page-content--fixed">
            <div className="page-header">
                <button className="back-btn" onClick={() => navigate('/services')}>
                    ←
                </button>
                <h1 className="page-title">Пользователи</h1>
            </div>

            <div className="tabs">
                {tabs.map((t) => (
                    <button
                        key={t.key}
                        className={'tab' + (tab === t.key ? ' selected' : '')}
                        onClick={() => setTab(t.key)}
                    >
                        {t.label}
                    </button>
                ))}
            </div>

            <div className="content-block">
                <div className="search-actions-row">
                    <input
                        className="search-input"
                        placeholder="Поиск по имени пользователя..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                    />
                    <div className="action-row">
                        <button className="btn btn-add" onClick={() => setShowCreate(true)}>
                            Добавить
                        </button>
                        <button
                            className="btn btn-warning"
                            disabled={!selected}
                            onClick={() => setShowReset(true)}
                        >
                            Сбросить пароль
                        </button>
                        <button
                            className={'btn ' + (selected?.enabled ? 'btn-delete' : 'btn-success')}
                            disabled={!selected || isSelf}
                            title={isSelf ? 'Нельзя изменить свой собственный статус' : ''}
                            onClick={handleToggle}
                        >
                            {selected?.enabled ? 'Отключить' : 'Включить'}
                        </button>
                    </div>
                </div>

                {actionError && (
                    <div className="form-error" style={{ marginTop: 12 }}>
                        {actionError}
                    </div>
                )}
            </div>

            <div className="content-block  table-container">
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
                        Пользователи не найдены
                    </div>
                )}

                {!loading && !error && filtered.length > 0 && (
                    <div className="table-scroll">
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th style={{ width: '60px' }}>№</th>
                                <th>Имя пользователя</th>
                                <th style={{ width: '160px' }}>Роль</th>
                                <th style={{ width: '140px' }}>Статус</th>
                                <th style={{ width: '180px' }}>Дата создания</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filtered.map((u, i) => (
                                <tr
                                    key={u.id}
                                    className={selectedId === u.id ? 'selected' : ''}
                                    onClick={() => setSelectedId(u.id)}
                                >
                                    <td>{i + 1}</td>
                                    <td>{u.username}</td>
                                    <td>
                                            <span className={'role-badge role-' + u.role.toLowerCase()}>
                                                {ROLE_LABELS[u.role] || u.role}
                                            </span>
                                    </td>
                                    <td>
                                            <span className={'status ' + (u.enabled ? 'status-approved' : 'status-cancelled')}>
                                                {u.enabled ? 'Активен' : 'Отключён'}
                                            </span>
                                    </td>
                                    <td>{formatDate(u.createdAt)}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {showCreate && (
                <UserCreateDialog
                    onClose={() => setShowCreate(false)}
                    onCreated={handleCreated}
                />
            )}

            {showReset && selected && (
                <ResetPasswordDialog
                    user={selected}
                    onClose={() => setShowReset(false)}
                    onDone={handleResetDone}
                />
            )}
        </div>
    );
}
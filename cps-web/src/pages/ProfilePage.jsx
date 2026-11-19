import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { changePassword } from '../api/auth';
import PageHeader from '../components/PageHeader';
import Notice from '../components/Notice';
export default function ProfilePage() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [currentPassword, setCurrent] = useState('');
    const [newPassword, setNew] = useState('');
    const [confirmation, setConfirmation] = useState('');
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    async function submit(event) {
        event.preventDefault(); setError('');
        if (newPassword !== confirmation) { setError('Новые пароли не совпадают'); return; }
        if (newPassword === currentPassword) { setError('Новый пароль должен отличаться от текущего'); return; }
        setSaving(true);
        try {
            await changePassword(currentPassword, newPassword);
            await logout();
            navigate('/login', { replace: true, state: { message: 'Пароль изменён. Войдите с новым паролем.' } });
        } catch (failure) { setError(failure.message); } finally { setSaving(false); }
    }
    return <div className="page-content"><PageHeader title="Мой профиль" />
        {user.mustChangePassword && <Notice>Для продолжения работы смените временный пароль. После смены войдите заново.</Notice>}
        <div className="content-block profile-grid"><div><h2>{user.fullName || user.username}</h2><p>{user.username}</p><p className="muted">{{ USER: 'Пользователь', MANAGER: 'Менеджер', ADMIN: 'Администратор' }[user.role]}</p><p className="record-id">{user.id}</p></div>
            <form onSubmit={submit}><h2>Сменить пароль</h2><Notice>{error}</Notice>
                <label className="form-group form-label">Текущий пароль<input className="form-input" type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrent(event.target.value)} required disabled={saving} /></label>
                <label className="form-group form-label">Новый пароль<input className="form-input" type="password" autoComplete="new-password" minLength={12} maxLength={72} value={newPassword} onChange={(event) => setNew(event.target.value)} required disabled={saving} /></label>
                <label className="form-group form-label">Повторите новый пароль<input className="form-input" type="password" autoComplete="new-password" minLength={12} maxLength={72} value={confirmation} onChange={(event) => setConfirmation(event.target.value)} required disabled={saving} /></label>
                <p className="muted">От 12 до 72 символов. Все текущие сессии будут завершены.</p><button className="btn btn-primary" disabled={saving}>{saving ? 'Сохранение…' : 'Сменить пароль'}</button>
            </form></div></div>;
}

import { useState } from 'react';
import { createUser } from '../../api/users';

export default function UserCreateDialog({ onClose, onCreated }) {
    const [username, setUsername] = useState('');
    const [fullName, setFullName] = useState('');
    const [password, setPassword] = useState('');
    const [role, setRole] = useState('USER');
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        setError('');

        if (username.trim().length === 0) {
            setError('Введите имя пользователя');
            return;
        }
        if (!fullName.trim()) { setError('Введите полное имя'); return; }
        if (password.length < 12) {
            setError('Пароль должен быть не короче 12 символов');
            return;
        }
        if (password.length > 72) {
            setError('Пароль должен быть не длиннее 72 символов');
            return;
        }

        setSaving(true);
        try {
            await createUser(username.trim(), password, role, fullName.trim());
            onCreated();
        } catch (e) {
            setError(e.message || 'Не удалось создать пользователя');
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="dialog-backdrop" onClick={onClose}>
            <div className="dialog-root" onClick={(e) => e.stopPropagation()}>
                <div className="dialog-header">
                    <h2 className="dialog-title">Новый пользователь</h2>
                    <button className="dialog-close" onClick={onClose} type="button">
                        ✕
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    {error && <div className="login-error" style={{ marginBottom: 12 }}>{error}</div>}

                    <div className="form-group">
                        <label className="form-label">Имя пользователя *</label>
                        <input
                            className="form-input"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="Не более 255 символов"
                            disabled={saving}
                            maxLength={255}
                            autoFocus
                        />
                    </div>

                    <label className="form-group form-label">Полное имя *<input className="form-input" value={fullName} onChange={(event) => setFullName(event.target.value)} disabled={saving} maxLength={255} required /></label>
                    <div className="form-group">
                        <label className="form-label">Временный пароль *</label>
                        <input
                            className="form-input"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Не короче 12 символов"
                            disabled={saving}
                            minLength={12}
                            maxLength={72}
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Роль *</label>
                        <select
                            className="form-select"
                            value={role}
                            onChange={(e) => setRole(e.target.value)}
                            disabled={saving}
                        >
                            <option value="USER">Пользователь</option>
                            <option value="MANAGER">Менеджер</option>
                            <option value="ADMIN">Администратор</option>
                        </select>
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
                            {saving ? 'Создание...' : 'Создать'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
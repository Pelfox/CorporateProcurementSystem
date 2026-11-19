import { useState } from 'react';
import { resetUserPassword } from '../../api/users';

export default function ResetPasswordDialog({ user, onClose, onDone }) {
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        setError('');

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
            await resetUserPassword(user.id, password);
            onDone();
        } catch (e) {
            setError(e.message || 'Не удалось сбросить пароль');
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="dialog-backdrop" onClick={onClose}>
            <div className="dialog-root" onClick={(e) => e.stopPropagation()}>
                <div className="dialog-header">
                    <h2 className="dialog-title">Сброс пароля</h2>
                    <button className="dialog-close" onClick={onClose} type="button">
                        ✕
                    </button>
                </div>

                <p style={{ marginTop: 0, marginBottom: 20, color: 'var(--text-muted)', fontSize: 14 }}>
                    Пользователь:{' '}
                    <strong style={{ color: 'var(--text-main)' }}>{user.username}</strong>
                </p>

                <form onSubmit={handleSubmit}>
                    {error && <div className="login-error" style={{ marginBottom: 12 }}>{error}</div>}

                    <div className="form-group">
                        <label className="form-label">Новый пароль *</label>
                        <input
                            className="form-input"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Не короче 12 символов"
                            disabled={saving}
                            minLength={12}
                            maxLength={72}
                            autoFocus
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
                        <button type="submit" className="btn btn-warning" disabled={saving}>
                            {saving ? 'Сохранение...' : 'Сбросить пароль'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
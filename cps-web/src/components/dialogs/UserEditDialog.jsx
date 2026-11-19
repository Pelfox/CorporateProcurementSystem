import { useState } from 'react';
import { updateUser } from '../../api/users';
import Notice from '../Notice';
export default function UserEditDialog({ user, onClose, onSaved }) {
    const [username, setUsername] = useState(user.username);
    const [fullName, setFullName] = useState(user.fullName);
    const [role, setRole] = useState(user.role);
    const [enabled, setEnabled] = useState(user.enabled);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    async function save(event) {
        event.preventDefault(); setSaving(true); setError('');
        try { await updateUser(user.id, { username: username.trim(), fullName: fullName.trim(), role, enabled }); onSaved(); }
        catch (failure) { setError(failure.message); } finally { setSaving(false); }
    }
    return <div className="dialog-backdrop" onClick={saving ? undefined : onClose}><section className="dialog-root" role="dialog" aria-modal="true" aria-labelledby="edit-user-title" onClick={(event) => event.stopPropagation()}><div className="dialog-header"><h2 className="dialog-title" id="edit-user-title">Редактировать пользователя</h2><button className="dialog-close" disabled={saving} onClick={onClose} aria-label="Закрыть">✕</button></div><form onSubmit={save}><Notice>{error}</Notice>
        <label className="form-group form-label">Полное имя<input className="form-input" required maxLength={255} value={fullName} onChange={(event) => setFullName(event.target.value)} disabled={saving} /></label>
        <label className="form-group form-label">Логин<input className="form-input" required maxLength={255} value={username} onChange={(event) => setUsername(event.target.value)} disabled={saving} /></label>
        <label className="form-group form-label">Роль<select className="form-select" value={role} disabled={saving} onChange={(event) => setRole(event.target.value)}><option value="USER">Пользователь</option><option value="MANAGER">Менеджер</option><option value="ADMIN">Администратор</option></select></label>
        <label className="form-group"><input type="checkbox" checked={enabled} disabled={saving} onChange={(event) => setEnabled(event.target.checked)} /> Учётная запись активна</label><p className="muted">Изменение логина, роли или активности завершит все сессии пользователя.</p><div className="dialog-actions"><button className="btn btn-cancel" type="button" disabled={saving} onClick={onClose}>Отмена</button><button className="btn btn-primary" disabled={saving}>{saving ? 'Сохранение…' : 'Сохранить'}</button></div>
    </form></section></div>;
}

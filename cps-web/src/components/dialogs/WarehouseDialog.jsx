import { useState } from 'react';
import { createWarehouse, updateWarehouse } from '../../api/warehouses';

export default function WarehouseDialog({ warehouse, onClose, onSaved }) {
    const isEditing = !!warehouse;
    const [name, setName] = useState(warehouse?.name || '');
    const [location, setLocation] = useState(warehouse?.location || '');
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        const trimmed = name.trim();
        if (!trimmed) {
            setError('Название склада не может быть пустым');
            return;
        }
        if (trimmed.length > 255) {
            setError('Название не должно превышать 255 символов');
            return;
        }
        if (location.trim().length > 255) {
            setError('Адрес не должен превышать 255 символов');
            return;
        }
        setError('');
        setSaving(true);
        try {
            if (isEditing) {
                await updateWarehouse(warehouse.id, trimmed, location.trim() || null);
            } else {
                await createWarehouse(trimmed, location.trim() || null);
            }
            onSaved();
        } catch (e) {
            setError(e.message || 'Не удалось сохранить склад');
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="dialog-backdrop" onClick={onClose}>
            <div className="dialog-root" onClick={(e) => e.stopPropagation()}>
                <div className="dialog-header">
                    <h2 className="dialog-title">{isEditing ? 'Редактирование склада' : 'Новый склад'}</h2>
                    <button className="dialog-close" onClick={onClose} type="button">
                        ✕
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    {error && (
                        <div className="login-error" style={{ marginBottom: 12 }}>
                            {error}
                        </div>
                    )}

                    <div className="form-group">
                        <label className="form-label">Название склада *</label>
                        <input
                            className="form-input"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="Название склада — не более 255 символов"
                            disabled={saving}
                            maxLength={255}
                            autoFocus
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Адрес (местоположение)</label>
                        <input
                            className="form-input"
                            value={location}
                            onChange={(e) => setLocation(e.target.value)}
                            placeholder="Адрес — не более 255 символов"
                            disabled={saving}
                            maxLength={255}
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
                        <button type="submit" className="btn btn-primary" disabled={saving}>
                            {saving ? 'Сохранение...' : 'Сохранить'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
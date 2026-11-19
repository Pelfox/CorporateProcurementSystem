import { useState } from 'react';
import { createCategory, updateCategory } from '../../api/categories';

export default function CategoryDialog({ category, onClose, onSaved }) {
    const isEditing = !!category;
    const [name, setName] = useState(category?.name || '');
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        const trimmed = name.trim();
        if (!trimmed) {
            setError('Название категории не может быть пустым');
            return;
        }
        if (trimmed.length > 255) {
            setError('Название не должно превышать 255 символов');
            return;
        }
        setError('');
        setSaving(true);
        try {
            if (isEditing) {
                await updateCategory(category.id, trimmed);
            } else {
                await createCategory(trimmed);
            }
            onSaved();
        } catch (e) {
            setError(e.message || 'Не удалось сохранить категорию');
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="dialog-backdrop" onClick={onClose}>
            <div className="dialog-root" onClick={(e) => e.stopPropagation()}>
                <div className="dialog-header">
                    <h2 className="dialog-title">
                        {isEditing ? 'Редактирование категории' : 'Новая категория'}
                    </h2>
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
                        <label className="form-label">Название категории *</label>
                        <input
                            className="form-input"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="Название категории — не более 255 символов"
                            disabled={saving}
                            maxLength={255}
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
                        <button type="submit" className="btn btn-primary" disabled={saving}>
                            {saving ? 'Сохранение...' : 'Сохранить'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
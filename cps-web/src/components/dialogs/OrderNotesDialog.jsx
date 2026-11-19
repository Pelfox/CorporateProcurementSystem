import { useState } from 'react';
import { updateOrderNotes } from '../../api/orders';

export default function OrderNotesDialog({ order, currentUserId, onClose, onSaved }) {
    const [notes, setNotes] = useState(order.notes || '');
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        if (notes.length > 255) {
            setError('Примечание не должно превышать 255 символов');
            return;
        }
        setError('');
        setSaving(true);
        try {
            await updateOrderNotes(
                order.id,
                notes.trim() === '' ? null : notes.trim(),
                currentUserId
            );
            onSaved();
        } catch (e) {
            setError(e.message || 'Не удалось сохранить примечание');
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="dialog-backdrop" onClick={onClose}>
            <div className="dialog-root" onClick={(e) => e.stopPropagation()}>
                <div className="dialog-header">
                    <h2 className="dialog-title">Примечание к заказу</h2>
                    <button className="dialog-close" onClick={onClose} type="button">
                        ✕
                    </button>
                </div>

                <p style={{ marginTop: 0, marginBottom: 16, color: 'var(--text-muted)', fontSize: 13 }}>
                    Заказ № <span style={{ fontFamily: 'monospace' }}>{order.id.substring(0, 8)}</span>
                </p>

                <form onSubmit={handleSubmit}>
                    {error && (
                        <div className="login-error" style={{ marginBottom: 12 }}>
                            {error}
                        </div>
                    )}

                    <div className="form-group">
                        <label className="form-label">Примечание</label>
                        <textarea
                            className="form-input"
                            rows={3}
                            value={notes}
                            onChange={(e) => setNotes(e.target.value)}
                            placeholder="Необязательное примечание"
                            disabled={saving}
                            maxLength={255}
                            style={{ resize: 'vertical', minHeight: 80 }}
                            autoFocus
                        />
                        <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 4, textAlign: 'right' }}>
                            {notes.length} / 255
                        </div>
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
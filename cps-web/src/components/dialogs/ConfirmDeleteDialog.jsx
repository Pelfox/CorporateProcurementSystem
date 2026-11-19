export default function ConfirmDeleteDialog({ title, entityName, onCancel, onConfirm }) {
    return (
        <div className="dialog-backdrop" onClick={onCancel}>
            <div className="dialog-root" onClick={(e) => e.stopPropagation()} style={{ textAlign: 'center', width: 380 }}>
                <div style={{ fontSize: 56, lineHeight: 1, marginBottom: 12 }}>⚠</div>
                <h2
                    className="dialog-title"
                    style={{ textAlign: 'center', marginBottom: 12 }}
                >
                    {title || 'Подтверждение удаления'}
                </h2>
                <p style={{ fontSize: 14, color: '#475569', marginBottom: 8, whiteSpace: 'pre-line' }}>
                    Вы уверены, что хотите удалить{'\n'}
                    <strong style={{ color: 'var(--text-main)' }}>«{entityName}»</strong>?
                </p>
                <p style={{ fontSize: 12, color: '#888', marginBottom: 20 }}>
                    Это действие нельзя отменить.
                </p>
                <div className="dialog-actions" style={{ justifyContent: 'center' }}>
                    <button className="btn btn-cancel" onClick={onCancel} type="button">
                        Отмена
                    </button>
                    <button className="btn btn-delete" onClick={onConfirm} type="button">
                        Удалить
                    </button>
                </div>
            </div>
        </div>
    );
}
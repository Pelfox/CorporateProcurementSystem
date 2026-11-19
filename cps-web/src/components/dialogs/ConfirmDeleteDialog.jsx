import { useState } from 'react';
export default function ConfirmDeleteDialog({ title, entityName, onCancel, onConfirm, irreversible = false, busy = false }) {
    const [pending, setPending] = useState(false);
    const locked = pending || busy;
    async function confirm() {
        if (locked) return;
        setPending(true);
        try { await onConfirm(); } finally { setPending(false); }
    }
    return <div className="dialog-backdrop" onClick={locked ? undefined : onCancel}><section className="dialog-root" role="dialog" aria-modal="true" aria-labelledby="delete-title" onClick={(event) => event.stopPropagation()}>
        <h2 className="dialog-title" id="delete-title">{title || 'Подтверждение удаления'}</h2><p>Удалить «<strong>{entityName}</strong>»?</p><p className="muted">{irreversible ? 'Запись и зависимые удалённые записи будут удалены без возможности восстановления.' : 'Запись будет скрыта. Администратор сможет восстановить её по UUID.'}</p><div className="dialog-actions"><button className="btn btn-cancel" onClick={onCancel} disabled={locked}>Отмена</button><button className="btn btn-delete" onClick={confirm} disabled={locked}>{locked ? 'Удаление…' : 'Удалить'}</button></div>
    </section></div>;
}

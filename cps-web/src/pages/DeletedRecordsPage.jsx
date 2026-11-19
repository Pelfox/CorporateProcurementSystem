import { useState } from 'react';
import { restoreRecord, purgeRecord } from '../api/admin';
import PageHeader from '../components/PageHeader';
import Notice from '../components/Notice';
import ConfirmDeleteDialog from '../components/dialogs/ConfirmDeleteDialog';
const ENTITIES = { users: 'Пользователь', categories: 'Категория', products: 'Товар', warehouses: 'Склад', inventories: 'Остаток', orders: 'Заказ', 'order-items': 'Позиция заказа', 'audit-logs': 'История статусов', tags: 'Тег' };
export default function DeletedRecordsPage() {
    const [entity, setEntity] = useState('products');
    const [id, setId] = useState('');
    const [busy, setBusy] = useState(false);
    const [confirm, setConfirm] = useState(false);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    async function act(purge) {
        setBusy(true); setError(''); setMessage('');
        try { await (purge ? purgeRecord : restoreRecord)(entity, id.trim()); setMessage(purge ? 'Запись окончательно удалена.' : 'Запись восстановлена.'); setConfirm(false); }
        catch (failure) { setConfirm(false); const dependencies = failure.problem?.dependencies || []; setError(failure.message + dependencies.map((item) => ` ${item.entity}: ${item.id}`).join(';')); }
        finally { setBusy(false); }
    }
    return <div className="page-content"><PageHeader title="Удалённые записи" /><Notice>{error}</Notice><Notice success>{message}</Notice><form className="content-block" onSubmit={(event) => { event.preventDefault(); act(false); }}><h2>Восстановление по UUID</h2><p className="muted">Выберите тип записи и укажите её UUID из журнала аудита. При восстановлении связанные записи должны быть активны.</p><div className="search-row"><label className="form-label">Тип записи<select className="form-select" value={entity} disabled={busy} onChange={(event) => setEntity(event.target.value)}>{Object.entries(ENTITIES).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label><label className="form-label grow">UUID записи<input className="form-input" required pattern="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" value={id} onChange={(event) => setId(event.target.value)} disabled={busy} placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" /></label></div><div className="action-row"><button className="btn btn-success" disabled={busy}>{busy ? 'Выполнение…' : 'Восстановить'}</button><button className="btn btn-delete" disabled={busy || !/^[0-9a-f-]{36}$/i.test(id.trim())} type="button" onClick={(event) => { if (event.currentTarget.form.reportValidity()) setConfirm(true); }}>Удалить окончательно</button></div></form>
        {confirm && <ConfirmDeleteDialog title="Окончательное удаление" entityName={`${ENTITIES[entity]} ${id}`} irreversible busy={busy} onCancel={() => setConfirm(false)} onConfirm={() => act(true)} />}</div>;
}

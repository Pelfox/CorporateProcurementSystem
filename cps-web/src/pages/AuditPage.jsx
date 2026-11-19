import { useCallback, useState } from 'react';
import { getAuditEvents } from '../api/admin';
import useResource from '../hooks/useResource';
import PageHeader from '../components/PageHeader';
import Notice from '../components/Notice';
import { formatDateTime } from '../utils/format';
export default function AuditPage() {
    const [q, setQ] = useState('');
    const [entityId, setEntityId] = useState('');
    const [filters, setFilters] = useState({});
    const [page, setPage] = useState(0);
    const loader = useCallback(() => getAuditEvents({ ...filters, page, size: 20 }), [filters, page]);
    const { data, loading, error, reload } = useResource(loader);
    function search(event) { event.preventDefault(); setPage(0); setFilters({ q: q.trim(), entityId: entityId.trim() }); }
    return <div className="page-content"><PageHeader title="Журнал аудита"><button className="btn btn-cancel" onClick={reload} disabled={loading}>Обновить</button></PageHeader><Notice>{error}</Notice>
        <form className="content-block search-row" onSubmit={search}><label className="form-label">Поиск<input className="form-input" placeholder="Автор, описание, Correlation ID" value={q} maxLength={255} onChange={(event) => setQ(event.target.value)} /></label><label className="form-label">UUID записи<input className="form-input" placeholder="Необязательно" value={entityId} onChange={(event) => setEntityId(event.target.value)} /></label><button className="btn btn-primary">Найти</button><button className="btn btn-cancel" type="button" onClick={() => { setQ(''); setEntityId(''); setFilters({}); setPage(0); }}>Сбросить</button></form>
        <div className="content-block"><div className="table-scroll"><table className="data-table"><thead><tr><th>Дата</th><th>Автор</th><th>Событие</th><th>Запись</th><th>Описание</th></tr></thead><tbody>{data?.content.map((event) => <tr key={event.id}><td>{formatDateTime(event.occurredAt)}</td><td>{event.actorUsername || 'Система'}</td><td>{event.eventType}</td><td><span>{event.entityType}</span><div className="record-id">{event.entityId || '—'}</div></td><td>{event.description}<details><summary>Подробности</summary><div className="record-id">Correlation ID: {event.correlationId || '—'}<br />Автор: {event.actorId || '—'}</div></details></td></tr>)}</tbody></table></div>{loading && <p role="status">Загрузка…</p>}{!loading && !data?.content.length && <p className="muted">События не найдены</p>}
        <div className="pagination"><button className="btn btn-cancel" disabled={loading || page === 0} onClick={() => setPage(page - 1)}>Предыдущая</button><span>Страница {page + 1} из {Math.max(1, data?.totalPages || 0)} · Всего {data?.totalElements || 0}</span><button className="btn btn-cancel" disabled={loading || page + 1 >= (data?.totalPages || 0)} onClick={() => setPage(page + 1)}>Следующая</button></div></div></div>;
}

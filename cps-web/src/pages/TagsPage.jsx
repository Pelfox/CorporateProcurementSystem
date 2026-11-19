import { useState } from 'react';
import { useAuth } from '../context/useAuth';
import { getTags, createTag, updateTag, deleteTag } from '../api/tags';
import useResource from '../hooks/useResource';
import PageHeader from '../components/PageHeader';
import Notice from '../components/Notice';
import ConfirmDeleteDialog from '../components/dialogs/ConfirmDeleteDialog';
export default function TagsPage() {
    const { user } = useAuth();
    const canEdit = ['MANAGER', 'ADMIN'].includes(user.role);
    const { data: tags, loading, error, reload } = useResource(getTags);
    const [search, setSearch] = useState('');
    const [selected, setSelected] = useState(null);
    const [name, setName] = useState('');
    const [busy, setBusy] = useState(false);
    const [actionError, setError] = useState('');
    const [deleting, setDeleting] = useState(false);
    async function save(event) {
        event.preventDefault(); setBusy(true); setError('');
        try { if (selected) await updateTag(selected.id, name.trim()); else await createTag(name.trim()); setSelected(null); setName(''); reload(); }
        catch (failure) { setError(failure.message); } finally { setBusy(false); }
    }
    async function remove() {
        setBusy(true); setError('');
        try { await deleteTag(selected.id); setSelected(null); setName(''); setDeleting(false); reload(); }
        catch (failure) { setError(failure.message); setDeleting(false); } finally { setBusy(false); }
    }
    return <div className="page-content"><PageHeader title="Теги товаров" /><Notice>{actionError || error}</Notice>
        {canEdit && <form className="content-block search-row" onSubmit={save}><label className="form-label">{selected ? 'Название тега' : 'Новый тег'}<input className="form-input" required maxLength={255} value={name} disabled={busy} onChange={(event) => setName(event.target.value)} /></label><button className="btn btn-primary" disabled={busy || !name.trim()}>{selected ? 'Сохранить' : 'Добавить'}</button>{selected && <><button className="btn btn-cancel" type="button" onClick={() => { setSelected(null); setName(''); }}>Отменить выбор</button><button className="btn btn-delete" type="button" disabled={busy} onClick={() => setDeleting(true)}>Удалить</button></>}</form>}
        <section className="content-block"><input className="search-input" aria-label="Поиск тегов" placeholder="Поиск тега…" value={search} onChange={(event) => setSearch(event.target.value)} />
            {loading ? <p role="status">Загрузка…</p> : <><p className="muted">Для привязки тега выберите товар в каталоге и нажмите «Теги».</p><div className="tag-list">{(tags || []).filter((tag) => tag.name.toLowerCase().includes(search.toLowerCase())).map((tag) => <button className={'tag-chip' + (selected?.id === tag.id ? ' selected' : '')} key={tag.id} disabled={!canEdit || busy} onClick={() => { setSelected(tag); setName(tag.name); }}>{tag.name}</button>)}</div>{tags?.length === 0 && <p className="muted">Тегов пока нет</p>}</>}
        </section>{deleting && selected && <ConfirmDeleteDialog entityName={selected.name} busy={busy} onCancel={() => setDeleting(false)} onConfirm={remove} />}</div>;
}

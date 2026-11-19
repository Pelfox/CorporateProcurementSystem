import { useCallback, useState } from 'react';
import { getTags } from '../../api/tags';
import { getProductTags, addProductTag, removeProductTag } from '../../api/products';
import useResource from '../../hooks/useResource';
import Notice from '../Notice';
export default function ProductTagsDialog({ product, canEdit, onClose }) {
    const loader = useCallback(() => Promise.all([getTags(), getProductTags(product.id)]), [product.id]);
    const { data, loading, error, reload } = useResource(loader);
    const [busy, setBusy] = useState(false);
    const [actionError, setError] = useState('');
    async function toggle(tagId, assigned) {
        setBusy(true); setError('');
        try { await (assigned ? removeProductTag : addProductTag)(product.id, tagId); reload(); }
        catch (failure) { setError(failure.message); } finally { setBusy(false); }
    }
    return <div className="dialog-backdrop" onClick={busy ? undefined : onClose}><section className="dialog-root" role="dialog" aria-modal="true" aria-labelledby="product-tags-title" onClick={(event) => event.stopPropagation()}><div className="dialog-header"><h2 className="dialog-title" id="product-tags-title">Теги: {product.name}</h2><button className="dialog-close" aria-label="Закрыть" onClick={onClose} disabled={busy}>✕</button></div><Notice>{actionError || error}</Notice>
        {loading ? <p role="status">Загрузка…</p> : <div className="tag-list">{data?.[0].map((tag) => { const assigned = data[1].some((item) => item.id === tag.id); return <label className="tag-chip" key={tag.id}><input type="checkbox" checked={assigned} disabled={!canEdit || busy} onChange={() => toggle(tag.id, assigned)} /> {tag.name}</label>; })}{data?.[0].length === 0 && <p>Сначала создайте тег в разделе «Теги».</p>}</div>}<div className="dialog-actions"><button className="btn btn-cancel" onClick={onClose} disabled={busy}>Закрыть</button></div></section></div>;
}

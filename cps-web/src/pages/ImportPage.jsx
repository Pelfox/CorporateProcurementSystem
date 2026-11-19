import { useState } from 'react';
import { validateProducts, applyProducts } from '../api/import';
import { downloadBlob } from '../utils/download';
import PageHeader from '../components/PageHeader';
import Notice from '../components/Notice';
export default function ImportPage() {
    const [file, setFile] = useState(null);
    const [mode, setMode] = useState('CREATE_ONLY');
    const [validation, setValidation] = useState(null);
    const [result, setResult] = useState(null);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    function reset() { setValidation(null); setResult(null); setError(''); }
    async function validate(event) {
        event.preventDefault(); reset();
        if (!file || !file.size || file.size > 2 * 1024 * 1024) { setError('Выберите непустой CSV-файл размером до 2 МБ'); return; }
        setBusy(true);
        try { setValidation(await validateProducts(file, mode)); } catch (failure) { setError(failure.message); } finally { setBusy(false); }
    }
    async function apply() {
        setBusy(true); setError('');
        try { setResult(await applyProducts(validation.importId)); } catch (failure) { setError(failure.message); } finally { setBusy(false); }
    }
    function sample() {
        downloadBlob(new Blob(['sku,name,category_id,price\nPAPER-001,Бумага А4,,420.50\n'], { type: 'text/csv;charset=utf-8' }), 'products-template.csv');
    }
    return <div className="page-content"><PageHeader title="Импорт товаров"><button className="btn btn-cancel" onClick={sample}>Образец CSV</button></PageHeader>
        <Notice>{error}</Notice><Notice success>{result && `Импорт завершён. Создано: ${result.createdRows}; обновлено: ${result.updatedRows}.`}</Notice>
        <form className="content-block" onSubmit={validate}><h2>Проверка файла</h2><p className="muted">CSV в UTF-8, до 2 МБ и 10 000 строк. Столбцы: sku, name, category_id, price. Разделитель — запятая; category_id можно оставить пустым.</p>
            <div className="search-row"><label className="form-label">CSV-файл<input className="form-input" type="file" accept=".csv,text/csv" disabled={busy} required onChange={(event) => { reset(); setFile(event.target.files[0] || null); }} /></label>
                <label className="form-label">Режим<select className="form-select" value={mode} disabled={busy} onChange={(event) => { reset(); setMode(event.target.value); }}><option value="CREATE_ONLY">Только новые товары</option><option value="UPSERT">Создать и обновить по артикулу</option></select></label>
                <button className="btn btn-primary" disabled={busy || !file}>{busy ? 'Обработка…' : 'Проверить файл'}</button></div></form>
        {validation && <section className="content-block"><h2>Результат проверки</h2><p>Всего строк: <strong>{validation.totalRows}</strong> · Корректных: <strong>{validation.validRows}</strong> · С ошибками: <strong>{validation.invalidRows}</strong></p>
            {validation.errors.length > 0 && <div className="table-scroll"><table className="data-table"><thead><tr><th>Строка</th><th>Поле</th><th>Ошибка</th></tr></thead><tbody>{validation.errors.map((entry, index) => <tr key={index}><td>{entry.row}</td><td>{entry.field}</td><td>{entry.message}</td></tr>)}</tbody></table></div>}
            <p className="muted">Проверка не меняет данные. Примените результат в течение 15 минут под этой же учётной записью.</p><button className="btn btn-success" onClick={apply} disabled={!validation.canApply || busy || !!result}>{result ? 'Импорт применён' : busy ? 'Применение…' : 'Применить импорт'}</button></section>}
    </div>;
}

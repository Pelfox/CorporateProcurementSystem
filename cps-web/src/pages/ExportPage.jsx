import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { exportAllData } from '../api/export';
import { downloadBlob } from '../utils/download';

const EXPORT_ITEMS = [
    'Пользователи (users.csv)',
    'Категории (categories.csv)',
    'Товары (products.csv)',
    'Склады (warehouses.csv)',
    'Остатки (inventories.csv)',
    'Заказы (orders.csv)',
    'Позиции заказов (order_items.csv)',
    'История изменений (audit_logs.csv)',
];

export default function ExportPage() {
    const navigate = useNavigate();

    const [exporting, setExporting] = useState(false);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    async function handleExport() {
        if (exporting) return;
        setError('');
        setSuccessMessage('');
        setExporting(true);

        try {
            const { blob, filename } = await exportAllData();
            downloadBlob(blob, filename);
            setSuccessMessage('Архив подготовлен. Скачивание началось.');
            setTimeout(() => setSuccessMessage(''), 5000);
        } catch (e) {
            setError(e.message || 'Не удалось выполнить экспорт');
        } finally {
            setExporting(false);
        }
    }

    return (
        <div className="page-content page-content--fixed">
            <div className="page-header">
                <button className="back-btn" onClick={() => navigate('/services')}>
                    Назад
                </button>
                <h1 className="page-title">Экспорт данных</h1>
            </div>

            {successMessage && (
                <div className="export-success">
                    <span>{successMessage}</span>
                </div>
            )}

            {error && (
                <div
                    style={{
                        background: '#fef2f2',
                        border: '1px solid #fecaca',
                        color: '#991b1b',
                        padding: '14px 18px',
                        borderRadius: 12,
                        fontSize: 14,
                    }}
                >
                    {error}
                </div>
            )}

            <div className="content-block">
                <div
                    className={'export-card' + (exporting ? ' exporting' : '')}
                    onClick={handleExport}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault();
                            handleExport();
                        }
                    }}
                >
                    <div className="export-emoji">💾</div>
                    <div className="export-title">Полный экспорт всех данных</div>
                    <div className="export-desc">Выгрузить все таблицы системы в CSV</div>
                    {exporting && <div className="export-progress">Подготовка архива...</div>}
                </div>
            </div>

            <div className="content-block">
                <div className="info-title">Будет экспортировано</div>

                <div className="info-grid">
                    {EXPORT_ITEMS.map((label) => (
                        <div key={label} className="info-list-item">
                            <span className="info-checkmark">✓</span>
                            <span>{label}</span>
                        </div>
                    ))}
                </div>

                <div className="info-note">
                    Браузер скачает ZIP-архив с восемью CSV-файлами.
                </div>
            </div>
        </div>
    );
}
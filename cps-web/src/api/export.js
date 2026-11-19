import { apiRequest } from './client';

const USE_MOCKS = true;

export async function exportAllData(managerId) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 700));
        const content =
            'MOCK EXPORT\n\n' +
            '(GET /api/exports/csv), ' +
            'ZIP-архив с 8 файлами:\n\n' +
            'users.csv\ncategories.csv\nproducts.csv\nwarehouses.csv\n' +
            'inventories.csv\norders.csv\norder_items.csv\naudit_logs.csv\n';
        const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
        return { blob, filename: 'export-mock.txt' };
    }
    const response = await apiRequest(
        '/exports/csv?managerId=' + encodeURIComponent(managerId)
    );
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось выгрузить данные');
    }
    const blob = await response.blob();
    return { blob, filename: 'export.zip' };
}
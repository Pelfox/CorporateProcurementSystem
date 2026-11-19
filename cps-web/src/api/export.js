import { apiRequest, responseError } from './client.js';
export async function exportAllData() {
    const response = await apiRequest('/exports/all');
    if (!response.ok) throw await responseError(response, 'Не удалось выгрузить данные');
    return { blob: await response.blob(), filename: 'export.zip' };
}

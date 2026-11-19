import { requestJson } from './client.js';
export function validateProducts(file, mode) {
    const body = new FormData();
    body.set('file', file);
    body.set('mode', mode);
    return requestJson('/imports/products/validate', { method: 'POST', body });
}
export const applyProducts = (importId) => requestJson(`/imports/products/${importId}/apply`, { method: 'POST' });

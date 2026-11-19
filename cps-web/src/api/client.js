export const API_URL = (import.meta.env?.VITE_API_URL || '/api').replace(/\/$/, '');
export const getAccessToken = () => localStorage.getItem('accessToken');
export const getRefreshToken = () => localStorage.getItem('refreshToken');
export function saveTokens(data) {
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
}
export function clearTokens() {
    for (const key of ['accessToken', 'refreshToken', 'user']) localStorage.removeItem(key);
}
export function getCurrentUser() {
    try { return JSON.parse(localStorage.getItem('user') || 'null'); }
    catch { return null; }
}
export const saveCurrentUser = (user) => localStorage.setItem('user', JSON.stringify(user));
export class ApiError extends Error {
    constructor(message, status, problem = {}) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.problem = problem;
    }
}
export async function responseError(response, fallback = 'Не удалось выполнить запрос') {
    const problem = await response.json().catch(() => ({}));
    return new ApiError(problem.detail || problem.message || fallback, response.status, problem);
}
function expireSession() {
    clearTokens();
    window.dispatchEvent(new Event('auth:expired'));
}
let refreshInFlight;
async function refreshTokens() {
    const token = getRefreshToken();
    if (!token) return false;
    const response = await fetch(API_URL + '/auth/refresh', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: token }),
    });
    if (!response.ok) {
        if (response.status === 401 || response.status === 403) return false;
        throw await responseError(response, 'Не удалось обновить сессию. Попробуйте ещё раз.');
    }
    saveTokens(await response.json());
    return true;
}
export async function apiRequest(path, options = {}) {
    const headers = new Headers(options.headers);
    if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
    const token = getAccessToken();
    if (token) headers.set('Authorization', 'Bearer ' + token);
    const send = () => fetch(API_URL + path, { ...options, headers });
    let response = await send();
    if (response.status === 401) {
        const alreadyRefreshed = getAccessToken() && getAccessToken() !== token;
        if (!alreadyRefreshed && !refreshInFlight) refreshInFlight = refreshTokens().finally(() => { refreshInFlight = undefined; });
        if (!alreadyRefreshed && !await refreshInFlight) {
            expireSession();
            throw new ApiError('Сессия завершена. Войдите снова.', 401);
        }
        headers.set('Authorization', 'Bearer ' + getAccessToken());
        response = await send();
        if (response.status === 401) expireSession();
    }
    return response;
}
export function queryString(values = {}) {
    const query = new URLSearchParams();
    for (const [key, value] of Object.entries(values)) {
        if (value !== undefined && value !== null && value !== '') query.set(key, String(value));
    }
    const text = query.toString();
    return text ? '?' + text : '';
}
export async function requestJson(path, options = {}, fallback) {
    const response = await apiRequest(path, options);
    if (!response.ok) throw await responseError(response, fallback);
    if (response.status === 204) return;
    const text = await response.text();
    return text ? JSON.parse(text) : undefined;
}
export async function getAllPages(path, filters = {}) {
    const items = [];
    for (let page = 0; ; page++) {
        const data = await requestJson(path + queryString({ ...filters, page, size: 100 }));
        if (Array.isArray(data)) return data;
        if (!Array.isArray(data?.content)) throw new Error('Сервер вернул некорректный список данных');
        items.push(...data.content);
        const totalPages = data.page?.totalPages ?? data.totalPages;
        if (totalPages !== undefined ? page + 1 >= totalPages : data.content.length < 100) return items;
    }
}

const API_URL = import.meta.env.VITE_API_URL || '/api';

export { API_URL };

// Токены
export function getAccessToken() {
    return localStorage.getItem('accessToken');
}

export function getRefreshToken() {
    return localStorage.getItem('refreshToken');
}

export function saveTokens(data) {
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
}

export function clearTokens() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
}

// Сохранённый пользователь (чтобы не мигал экран при загрузке)
export function getCurrentUser() {
    const raw = localStorage.getItem('user');
    if (!raw) return null;
    try {
        return JSON.parse(raw);
    } catch (e) {
        return null;
    }
}

export function saveCurrentUser(user) {
    localStorage.setItem('user', JSON.stringify(user));
}

// Обновляем access-токен по refresh-токену
async function tryRefresh() {
    const refreshToken = getRefreshToken();
    if (!refreshToken) return false;

    try {
        const response = await fetch(API_URL + '/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
        });
        if (!response.ok) {
            clearTokens();
            return false;
        }
        const data = await response.json();
        saveTokens(data);
        return true;
    } catch (e) {
        clearTokens();
        return false;
    }
}

// Универсальный запрос
export async function apiRequest(path, options = {}) {
    const headers = { ...(options.headers || {}) };

    if (options.body && !(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }

    const token = getAccessToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    let response = await fetch(API_URL + path, { ...options, headers });

    // Если 401 — пробуем refresh один раз
    if (response.status === 401) {
        const refreshed = await tryRefresh();
        if (!refreshed) {
            throw new Error('AUTH_REQUIRED');
        }
        headers['Authorization'] = 'Bearer ' + getAccessToken();
        response = await fetch(API_URL + path, { ...options, headers });
    }

    return response;
}
import { API_URL, requestJson, responseError, saveTokens, clearTokens, saveCurrentUser, getRefreshToken } from './client.js';
export async function login(username, password) {
    const response = await fetch(API_URL + '/auth/login', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
    });
    if (!response.ok) throw await responseError(response, 'Не удалось войти');
    saveTokens(await response.json());
    return loadCurrentUser();
}
export async function loadCurrentUser() {
    const user = await requestJson('/auth/me');
    saveCurrentUser(user);
    return user;
}
export async function logout() {
    const refreshToken = getRefreshToken();
    try {
        if (refreshToken) await fetch(API_URL + '/auth/logout', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
        });
    } finally { clearTokens(); }
}
export async function changePassword(currentPassword, newPassword) {
    await requestJson('/auth/password', { method: 'PUT', body: JSON.stringify({ currentPassword, newPassword }) });
    clearTokens();
}

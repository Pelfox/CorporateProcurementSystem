import {
    API_URL,
    apiRequest,
    saveTokens,
    clearTokens,
    saveCurrentUser,
    getRefreshToken,
} from './client';

export async function login(username, password) {
    const response = await fetch(API_URL + '/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
    });

    if (!response.ok) {
        throw new Error('Неверное имя пользователя или пароль');
    }

    const tokens = await response.json();
    saveTokens(tokens);

    const user = await loadCurrentUser();
    return user;
}

export async function loadCurrentUser() {
    const response = await apiRequest('/auth/me');
    if (!response.ok) {
        throw new Error('Не удалось загрузить данные пользователя');
    }
    const user = await response.json();
    saveCurrentUser(user);
    return user;
}

export async function logout() {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
        try {
            await apiRequest('/auth/logout', {
                method: 'POST',
                body: JSON.stringify({ refreshToken }),
            });
        } catch (e) {
        }
    }
    clearTokens();
}

export async function changePassword(currentPassword, newPassword) {
    const response = await apiRequest('/auth/password', {
        method: 'PUT',
        body: JSON.stringify({ currentPassword, newPassword }),
    });

    if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.detail || 'Не удалось сменить пароль');
    }
}
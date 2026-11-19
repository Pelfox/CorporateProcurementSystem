import { apiRequest } from './client';

export async function getUsers() {
    const response = await apiRequest('/users');
    if (!response.ok) {
        throw new Error('Не удалось загрузить пользователей');
    }
    return response.json();
}

export async function createUser(username, password, role) {
    const response = await apiRequest('/users', {
        method: 'POST',
        body: JSON.stringify({ username, password, role }),
    });
    if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.detail || 'Не удалось создать пользователя');
    }
    return response.json();
}

export async function setUserStatus(id, enabled) {
    const response = await apiRequest(`/users/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ enabled }),
    });
    if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.detail || 'Не удалось изменить статус');
    }
    return response.json();
}

export async function resetUserPassword(id, password) {
    const response = await apiRequest(`/users/${id}/password`, {
        method: 'PUT',
        body: JSON.stringify({ password }),
    });
    if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.detail || 'Не удалось сбросить пароль');
    }
}
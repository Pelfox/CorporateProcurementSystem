import { apiRequest } from './client';

const USE_MOCKS = true;

let mockCategories = [
    { id: '2001', name: 'Офисная техника' },
    { id: '2002', name: 'Канцелярские товары' },
    { id: '2003', name: 'Мебель' },
    { id: '2004', name: 'Расходные материалы' },
];

let seq = 5;

export async function getCategories() {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 80));
        return mockCategories.slice();
    }
    const response = await apiRequest('/categories');
    if (!response.ok) {
        throw new Error('Не удалось загрузить категории');
    }
    return response.json();
}

export async function createCategory(name) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 100));
        const trimmed = String(name || '').trim();
        if (!trimmed) {
            throw new Error('Название категории не может быть пустым');
        }
        if (mockCategories.some((c) => c.name.toLowerCase() === trimmed.toLowerCase())) {
            throw new Error('Категория с таким названием уже существует');
        }
        const id = seq + '00' + seq;
        seq += 1;
        const created = { id, name: trimmed };
        mockCategories = [...mockCategories, created];
        return created;
    }
    const response = await apiRequest('/categories', {
        method: 'POST',
        body: JSON.stringify({ name }),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось создать категорию');
    }
    return response.json();
}

export async function updateCategory(id, name) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 100));
        const trimmed = String(name || '').trim();
        if (!trimmed) {
            throw new Error('Название категории не может быть пустым');
        }
        if (
            mockCategories.some(
                (c) => c.id !== id && c.name.toLowerCase() === trimmed.toLowerCase()
            )
        ) {
            throw new Error('Категория с таким названием уже существует');
        }
        mockCategories = mockCategories.map((c) =>
            c.id === id ? { ...c, name: trimmed } : c
        );
        return mockCategories.find((c) => c.id === id);
    }
    const response = await apiRequest('/categories/' + id, {
        method: 'PUT',
        body: JSON.stringify({ name }),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось сохранить категорию');
    }
    return response.json();
}

export async function deleteCategory(id) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 100));
        // Имитация правила:
        // нельзя удалить категорию, в которой есть товары.
        const { getProducts } = await import('./products');
        const products = await getProducts();
        const used = products.some((p) => p.categoryId === id);
        if (used) {
            throw new Error('Сначала удалите или перенесите товары этой категории');
        }
        mockCategories = mockCategories.filter((c) => c.id !== id);
        return;
    }
    const response = await apiRequest('/categories/' + id, {
        method: 'DELETE',
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось удалить категорию');
    }
}
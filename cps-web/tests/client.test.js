import test, { beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { apiRequest, getAllPages, requestJson, saveTokens } from '../src/api/client.js';
import { createOrder, updateOrder } from '../src/api/orders.js';

const values = new Map();
globalThis.localStorage = { getItem: (key) => values.get(key) ?? null, setItem: (key, value) => values.set(key, String(value)), removeItem: (key) => values.delete(key) };
globalThis.window = new EventTarget();
beforeEach(() => values.clear());
const json = (data, status = 200) => new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } });

test('loads every page of Spring PagedModel responses and preserves categoryId null', async () => {
    const requests = [];
    globalThis.fetch = async (url) => {
        requests.push(url);
        return url.includes('page=0') ? json({ content: [{ id: 'first', categoryId: null }], page: { totalPages: 2 } }) : json({ content: [{ id: 'second' }], page: { totalPages: 2 } });
    };
    assert.deepEqual(await getAllPages('/products', { name: 'Paper' }), [{ id: 'first', categoryId: null }, { id: 'second' }]);
    assert.equal(requests.length, 2);
    assert.ok(requests[1].includes('page=1'));
    assert.ok(requests[1].includes('name=Paper'));
});

test('supports the flat pagination used by admin users', async () => {
    globalThis.fetch = async () => json({ content: [{ id: 'user' }], totalPages: 1 });
    assert.deepEqual(await getAllPages('/admin/users'), [{ id: 'user' }]);
});

test('concurrent unauthorized requests rotate a refresh token once', async () => {
    saveTokens({ accessToken: 'expired', refreshToken: 'refresh-one' });
    let refreshes = 0;
    globalThis.fetch = async (url, options) => {
        if (url.endsWith('/auth/refresh')) {
            refreshes++;
            await new Promise((resolve) => setTimeout(resolve, 10));
            return json({ accessToken: 'fresh', refreshToken: 'refresh-two' });
        }
        return options.headers.get('Authorization') === 'Bearer fresh' ? json({ ok: true }) : json({}, 401);
    };
    const responses = await Promise.all([apiRequest('/products'), apiRequest('/categories'), apiRequest('/orders')]);
    assert.ok(responses.every((response) => response.ok));
    assert.equal(refreshes, 1);
    assert.equal(localStorage.getItem('refreshToken'), 'refresh-two');
});

test('an expired session clears persisted credentials and informs the auth provider', async () => {
    saveTokens({ accessToken: 'expired', refreshToken: 'expired-refresh' });
    let expired = false;
    window.addEventListener('auth:expired', () => { expired = true; }, { once: true });
    globalThis.fetch = async () => json({}, 401);
    await assert.rejects(apiRequest('/products'), (error) => error.status === 401);
    assert.equal(localStorage.getItem('accessToken'), null);
    assert.equal(expired, true);
});

test('preserves Problem Detail conflict information', async () => {
    globalThis.fetch = async () => json({ detail: 'Restore the parent first', dependencies: [{ entity: 'categories', id: 'parent' }] }, 409);
    await assert.rejects(requestJson('/admin/deleted/products/test/restore'), (error) => error.status === 409 && error.problem.dependencies[0].id === 'parent');
});

test('multipart uploads keep their browser boundary and no-content responses are accepted', async () => {
    const body = new FormData(); body.set('mode', 'CREATE_ONLY');
    globalThis.fetch = async (_url, options) => {
        assert.equal(options.headers.has('Content-Type'), false);
        assert.equal(options.body, body);
        return new Response(null, { status: 204 });
    };
    assert.equal(await requestJson('/imports/products/validate', { method: 'POST', body }), undefined);
});

test('a cart is sent to a single transactional endpoint without a client-selected owner', async () => {
    const calls = [];
    globalThis.fetch = async (url, options) => { calls.push({ url, options }); return json({ id: 'order' }); };
    const items = [{ productId: 'product', quantity: 2 }];
    await createOrder('untrusted-user', items, 'Notes');
    await updateOrder('order', 'untrusted-user', items, null);
    assert.equal(calls[0].url, '/api/orders/checkout');
    assert.deepEqual(JSON.parse(calls[0].options.body), { items, notes: 'Notes' });
    assert.equal(calls[1].url, '/api/orders/order/contents');
    assert.equal(calls[1].options.method, 'PUT');
});


test('a late 401 reuses a token already refreshed by another request', async () => {
    saveTokens({ accessToken: 'old', refreshToken: 'refresh-one' });
    let refreshes = 0;
    globalThis.fetch = async (url, options) => {
        if (url.endsWith('/auth/refresh')) { refreshes++; return json({ accessToken: 'fresh', refreshToken: 'refresh-two' }); }
        const expired = options.headers.get('Authorization') === 'Bearer old';
        if (url.endsWith('/late') && expired) await new Promise((resolve) => setTimeout(resolve, 15));
        return expired ? json({}, 401) : json({ ok: true });
    };
    const responses = await Promise.all([apiRequest('/first'), apiRequest('/late')]);
    assert.ok(responses.every((response) => response.ok));
    assert.equal(refreshes, 1);
});

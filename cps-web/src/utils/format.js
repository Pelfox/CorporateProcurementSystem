export function formatDateTime(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function formatDate(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()}`;
}

export function formatMoney(value) {
    if (value == null) return '—';
    const n = Number(value);
    if (isNaN(n)) return String(value);
    // 12345.67 -> "12 345,67"
    const fixed = n.toFixed(2); // "12345.67"
    const [intPart, frac] = fixed.split('.');
    const withSpaces = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
    return `${withSpaces},${frac}`;
}

export function formatMoneyRub(value) {
    return formatMoney(value) + ' ₽';
}
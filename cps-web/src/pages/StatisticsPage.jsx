import { useState, useEffect } from 'react';
import {
    PieChart,
    Pie,
    Cell,
    Tooltip,
    Legend,
    ResponsiveContainer,
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
} from 'recharts';
import { getStatistics, getWarehouseStocks, getProductStatistics, getProductsByCategory, getProductsByStock } from '../api/statistics';
import { formatMoneyRub } from '../utils/format';

// Цвета для секторов круговой диаграммы (CREATED, APPROVED, DELIVERED, CANCELLED)
const STATUS_COLORS = ['#3b82f6', '#10b981', '#9ca3af', '#ef4444'];
const STATUS_LABELS = {
    CREATED: 'Создан',
    APPROVED: 'Одобрен',
    DELIVERED: 'Доставлен',
    CANCELLED: 'Отменён',
};

export default function StatisticsPage() {
    const [stats, setStats] = useState(null);
    const [analytics, setAnalytics] = useState(null);
    const [revision, setRevision] = useState(0);
    const [warehouseStocks, setWarehouseStocks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        async function load() {
            setLoading(true);
            setError('');
            try {
                const [s, ws, products, categories, stock] = await Promise.all([
                    getStatistics(),
                    getWarehouseStocks(),
                    getProductStatistics(), getProductsByCategory(), getProductsByStock(),
                ]);
                setStats(s);
                setAnalytics({ products, categories, stock });
                setWarehouseStocks(ws);
            } catch (e) {
                setError(e.message || 'Не удалось загрузить статистику');
            } finally {
                setLoading(false);
            }
        }
        load();
    }, [revision]);

    if (loading) {
        return (
            <div className="page-content page-content--fixed">
                <div className="page-header">
                    <h1 className="page-title">Статистика</h1>
                </div>
                <div className="content-block">
                    <div className="state-message muted">Загрузка...</div>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="page-content page-content--fixed">
                <div className="page-header">
                    <h1 className="page-title">Статистика</h1>
                </div>
                <div className="content-block">
                    <div className="state-message danger" role="alert">{error}</div><button className="btn btn-primary" onClick={() => setRevision(revision + 1)}>Повторить загрузку</button>
                </div>
            </div>
        );
    }

    // Данные для круговой диаграммы
    const statusData = [
        { name: STATUS_LABELS.CREATED, value: stats.createdOrders },
        { name: STATUS_LABELS.APPROVED, value: stats.approvedOrders },
        { name: STATUS_LABELS.DELIVERED, value: stats.deliveredOrders },
        { name: STATUS_LABELS.CANCELLED, value: stats.cancelledOrders },
    ].filter((item) => item.value > 0); // не показываем нулевые сектора

    return (
        <div className="page-content page-content--fixed">
            <div className="page-header">
                <h1 className="page-title">Статистика</h1><button className="btn btn-cancel" onClick={() => setRevision(revision + 1)}>Обновить</button>
            </div>

            <div className="content-block">
                {/* Сетка карточек */}
                <div className="stats-cards">
                    <div className="stat-card blue">
                        <div className="stat-value">{stats.totalUsers}</div>
                        <div className="stat-label">Пользователей</div>
                    </div>
                    <div className="stat-card green">
                        <div className="stat-value">{stats.totalProducts}</div>
                        <div className="stat-label">Товаров</div>
                    </div>
                    <div className="stat-card orange">
                        <div className="stat-value">{stats.totalCategories}</div>
                        <div className="stat-label">Категорий</div>
                    </div>
                    <div className="stat-card purple">
                        <div className="stat-value">{stats.totalWarehouses}</div>
                        <div className="stat-label">Складов</div>
                    </div>
                    <div className="stat-card red">
                        <div className="stat-value">{stats.totalOrders}</div>
                        <div className="stat-label">Заказов</div>
                    </div>
                    <div className="stat-card teal">
                        <div className="stat-value">{stats.activeOrders}</div>
                        <div className="stat-label">Активных заказов</div>
                    </div>
                    <div className="stat-card blue span-2">
                        <div className="stat-value large">{formatMoneyRub(stats.totalOrderValue)}</div>
                        <div className="stat-label">Общая стоимость заказов</div>
                    </div>
                </div>

                <div className="stats-cards">
                    <div className="stat-card green"><div className="stat-value">{analytics.products.active}</div><div className="stat-label">Активных товаров</div></div>
                    <div className="stat-card purple"><div className="stat-value">{analytics.products.archived}</div><div className="stat-label">Архивных товаров</div></div>
                    <div className="stat-card teal"><div className="stat-value">{analytics.products.withTags}</div><div className="stat-label">Товаров с тегами</div></div>
                    <div className="stat-card blue"><div className="stat-value">{formatMoneyRub(analytics.products.averagePrice)}</div><div className="stat-label">Средняя цена</div></div>
                </div>
                <div className="charts-grid" style={{ marginBottom: 24 }}>
                    <section className="chart-box"><h2 className="chart-title">Товары по категориям</h2>{analytics.categories.length ? <div className="table-scroll"><table className="data-table"><thead><tr><th>Категория</th><th>Товаров</th></tr></thead><tbody>{analytics.categories.map((category) => <tr key={category.categoryId || 'uncategorized'}><td>{category.categoryName}</td><td>{category.count}</td></tr>)}</tbody></table></div> : <p className="muted">Товаров пока нет</p>}</section>
                    <section className="chart-box"><h2 className="chart-title">Наличие товаров</h2>{analytics.stock.map((item) => <div className="search-row" key={item.level}><span className="grow">{{ OUT_OF_STOCK: 'Нет в наличии', LOW_STOCK: 'Мало: от 1 до 5 шт.', IN_STOCK: 'В наличии: больше 5 шт.' }[item.level]}</span><strong>{item.count}</strong></div>)}</section>
                </div>
                {/* Графики */}
                <div className="charts-grid">
                    <div className="chart-box">
                        <div className="chart-title">Распределение заказов по статусам</div>
                        <div className="chart-container">
                            {!statusData.length ? <p className="muted">Заказов пока нет</p> : <ResponsiveContainer width="100%" height="100%">
                                <PieChart>
                                    <Pie
                                        data={statusData}
                                        dataKey="value"
                                        nameKey="name"
                                        cx="50%"
                                        cy="50%"
                                        outerRadius="70%"
                                        label={({ name, value }) => `${name}: ${value}`}
                                    >
                                        {statusData.map((entry, index) => (
                                            <Cell
                                                key={`cell-${index}`}
                                                fill={STATUS_COLORS[index % STATUS_COLORS.length]}
                                            />
                                        ))}
                                    </Pie>
                                    <Tooltip />
                                    <Legend verticalAlign="bottom" height={36} />
                                </PieChart>
                            </ResponsiveContainer>}
                        </div>
                    </div>

                    <div className="chart-box">
                        <div className="chart-title">Остатки по складам</div>
                        <div className="chart-container">
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart data={warehouseStocks} margin={{ top: 8, right: 8, bottom: 8, left: 0 }}>
                                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                    <XAxis dataKey="warehouseName" tick={{ fontSize: 12 }} />
                                    <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                                    <Tooltip formatter={(value) => [`${value} шт.`, 'Остаток']} />
                                    <Bar dataKey="quantity" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
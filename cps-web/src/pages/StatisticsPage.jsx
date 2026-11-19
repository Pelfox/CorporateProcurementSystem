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
import { getStatistics, getWarehouseStocks } from '../api/statistics';
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
    const [warehouseStocks, setWarehouseStocks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        async function load() {
            setLoading(true);
            setError('');
            try {
                const [s, ws] = await Promise.all([
                    getStatistics(),
                    getWarehouseStocks(),
                ]);
                setStats(s);
                setWarehouseStocks(ws);
            } catch (e) {
                setError(e.message || 'Не удалось загрузить статистику');
            } finally {
                setLoading(false);
            }
        }
        load();
    }, []);

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
                    <div className="state-message danger">{error}</div>
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
                <h1 className="page-title">Статистика</h1>
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

                {/* Графики */}
                <div className="charts-grid">
                    <div className="chart-box">
                        <div className="chart-title">Распределение заказов по статусам</div>
                        <div className="chart-container">
                            <ResponsiveContainer width="100%" height="100%">
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
                            </ResponsiveContainer>
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
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ServicesPage() {
    const { user } = useAuth();
    const role = user?.role;
    const isManager = role === 'MANAGER' || role === 'ADMIN';
    const isAdmin = role === 'ADMIN';

    const cards = [
        {
            title: 'Каталог товаров',
            subtitle: 'Управление товарами и ценами',
            emoji: '📦',
            color: 'card-purple',
            path: '/services/catalog',
        },
        {
            title: 'Заказы',
            subtitle: 'Создание и отслеживание заказов',
            emoji: '📋',
            color: 'card-orange',
            path: '/services/orders',
            visible: isManager,
        },
        {
            title: 'Категории',
            subtitle: 'Категории товаров',
            emoji: '🏷️',
            color: 'card-teal',
            path: '/services/categories',
        },
        {
            title: 'Склады и остатки',
            subtitle: 'Управление складскими запасами',
            emoji: '🏭',
            color: 'card-green',
            path: '/services/warehouses',
        },
        {
            title: 'Статистика',
            subtitle: 'Аналитика и показатели системы',
            emoji: '📊',
            color: 'card-indigo',
            path: '/statistics',
        },
        {
            title: 'Экспорт данных',
            subtitle: 'Выгрузка в CSV',
            emoji: '💾',
            color: 'card-cyan',
            path: '/services/export',
            visible: isManager,
        },
        {
            title: 'Пользователи',
            subtitle: 'Управление пользователями',
            emoji: '👤',
            color: 'card-red',
            path: '/services/users',
            visible: isAdmin,
        },
    ];

    return (
        <div className="page-content">
            <h1 className="page-title">Сервисы</h1>

            <div className="services-block">
                <div className="services-grid">
                    {cards
                        .filter((card) => card.visible !== false)
                        .map((card) => (
                            <Link key={card.path} to={card.path} className={`service-card ${card.color}`}>
                                <div className="service-card-copy">
                                    <div className="service-card-title">{card.title}</div>
                                    <div className="service-card-subtitle">{card.subtitle}</div>
                                </div>
                                <div className="service-card-emoji">{card.emoji}</div>
                            </Link>
                        ))}
                </div>
            </div>
        </div>
    );
}
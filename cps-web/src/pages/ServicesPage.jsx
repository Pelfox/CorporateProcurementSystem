import { Link } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

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
            visible: isManager,
        },
        {
            title: 'Экспорт данных',
            subtitle: 'Выгрузка в CSV',
            emoji: '💾',
            color: 'card-cyan',
            path: '/services/export',
            visible: role === 'MANAGER',
        },
        {
            title: 'Пользователи',
            subtitle: 'Управление пользователями',
            emoji: '👤',
            color: 'card-red',
            path: '/services/users',
            visible: isAdmin,
        },
        { title: 'Теги товаров', subtitle: 'Метки и классификация товаров', emoji: '🔖', color: 'card-purple', path: '/services/tags' },
        { title: 'Импорт товаров', subtitle: 'Проверка и загрузка CSV', emoji: '📥', color: 'card-teal', path: '/services/import', visible: role === 'MANAGER' },
        { title: 'Журнал аудита', subtitle: 'Действия пользователей и история изменений', emoji: '📑', color: 'card-indigo', path: '/services/audit', visible: isAdmin },
        { title: 'Удалённые записи', subtitle: 'Восстановление и окончательное удаление', emoji: '🗂️', color: 'card-orange', path: '/services/deleted', visible: isAdmin },
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

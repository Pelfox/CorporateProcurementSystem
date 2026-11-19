import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

export default function Layout({ children }) {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const { pathname } = useLocation();

    // Определяем активный таб: my-orders | services | statistics
    function activeTab() {
        if (pathname === '/my-orders') return 'my-orders';
        if (pathname === '/statistics') return 'statistics';
        if (pathname === '/profile') return 'profile';
        return 'services';
    }
    const tab = activeTab();

    async function handleLogout() {
        await logout();
        navigate('/login');
    }

    const initial = (user?.username || '?').charAt(0).toUpperCase();

    return (
        <div className="app-shell">
            <header className="top-nav">
                <span className="logo-placeholder">
                    <img src="/logo.png" alt="АРГУС" />
                </span>
                <span className="app-title">АРГУС</span>

                <nav className="nav-menu">
                    <Link
                        to="/my-orders"
                        className={'nav-item' + (tab === 'my-orders' ? ' active' : '')}
                    >
                        Мои заказы
                    </Link>
                    <Link
                        to="/services"
                        className={'nav-item' + (tab === 'services' ? ' active' : '')}
                    >
                        Сервисы
                    </Link>
                    {['MANAGER', 'ADMIN'].includes(user?.role) && <Link
                        to="/statistics"
                        className={'nav-item' + (tab === 'statistics' ? ' active' : '')}
                    >
                        Статистика
                    </Link>}
                </nav>

                <div className="user-info">
                    <div className="avatar-circle">
                        <span className="avatar-initial">{initial}</span>
                    </div>
                    <Link to="/profile" className="username-label" title="Профиль и смена пароля">{user?.fullName || user?.username}</Link>
                    <button className="logout-btn" onClick={handleLogout}>
                        Выйти
                    </button>
                </div>
            </header>

            <main>{children}</main>
        </div>
    );
}
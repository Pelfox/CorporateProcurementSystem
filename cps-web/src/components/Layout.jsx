import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Layout({ children }) {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const { pathname } = useLocation();

    // Определяем активный таб: my-orders | services | statistics
    function activeTab() {
        if (pathname === '/my-orders') return 'my-orders';
        if (pathname === '/statistics') return 'statistics';
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
                    <Link
                        to="/statistics"
                        className={'nav-item' + (tab === 'statistics' ? ' active' : '')}
                    >
                        Статистика
                    </Link>
                </nav>

                <div className="user-info">
                    <div className="avatar-circle">
                        <span className="avatar-initial">{initial}</span>
                    </div>
                    <span className="username-label">{user?.username}</span>
                    <button className="logout-btn" onClick={handleLogout}>
                        Выйти
                    </button>
                </div>
            </header>

            <main>{children}</main>
        </div>
    );
}
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
export default function PrivateRoute({ children, roles, allowPasswordChange = false }) {
    const { user, loading } = useAuth();
    if (loading) return <div className="page-loader" role="status">Загрузка…</div>;
    if (!user) return <Navigate to="/login" replace />;
    if (user.mustChangePassword && !allowPasswordChange) return <Navigate to="/profile" replace />;
    if (roles && !roles.includes(user.role)) return <Navigate to="/services" replace />;
    return children;
}

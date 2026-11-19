import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import PrivateRoute from './components/PrivateRoute';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import ServicesPage from './pages/ServicesPage';
import UsersPage from './pages/UsersPage';
import MyOrdersPage from './pages/MyOrdersPage';
import CatalogPage from './pages/CatalogPage';
import CategoriesPage from './pages/CategoriesPage';
import WarehousesPage from './pages/WarehousesPage';
import ExportPage from './pages/ExportPage';
import OrdersPage from './pages/OrdersPage';
const StatisticsPage = lazy(() => import('./pages/StatisticsPage'));
import ProfilePage from './pages/ProfilePage';
import TagsPage from './pages/TagsPage';
import ImportPage from './pages/ImportPage';
import AuditPage from './pages/AuditPage';
import DeletedRecordsPage from './pages/DeletedRecordsPage';
function ProtectedPage({ children, roles, allowPasswordChange }) {
    return <PrivateRoute roles={roles} allowPasswordChange={allowPasswordChange}><Layout><Suspense fallback={<div className="page-loader" role="status">Загрузка страницы…</div>}>{children}</Suspense></Layout></PrivateRoute>;
}
export default function App() {
    const pages = [
        ['/my-orders', MyOrdersPage], ['/services', ServicesPage],
        ['/services/catalog', CatalogPage], ['/services/categories', CategoriesPage],
        ['/services/warehouses', WarehousesPage], ['/services/tags', TagsPage],
        ['/services/orders', OrdersPage, ['MANAGER', 'ADMIN']],
        ['/statistics', StatisticsPage, ['MANAGER', 'ADMIN']],
        ['/services/export', ExportPage, ['MANAGER']], ['/services/import', ImportPage, ['MANAGER']],
        ['/services/users', UsersPage, ['ADMIN']], ['/services/audit', AuditPage, ['ADMIN']],
        ['/services/deleted', DeletedRecordsPage, ['ADMIN']],
    ];
    return <BrowserRouter><Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/profile" element={<ProtectedPage allowPasswordChange><ProfilePage /></ProtectedPage>} />
        {pages.map(([path, Page, roles]) => <Route key={path} path={path} element={<ProtectedPage roles={roles}><Page /></ProtectedPage>} />)}
        <Route path="/" element={<Navigate to="/my-orders" replace />} />
        <Route path="*" element={<Navigate to="/my-orders" replace />} />
    </Routes></BrowserRouter>;
}

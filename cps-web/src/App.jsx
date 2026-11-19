import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import PrivateRoute from './components/PrivateRoute';
import Layout from './components/Layout';
import Placeholder from './components/Placeholder';
import LoginPage from './pages/LoginPage';
import ServicesPage from './pages/ServicesPage';
import UsersPage from './pages/UsersPage';
import MyOrdersPage from './pages/MyOrdersPage';
import CatalogPage from './pages/CatalogPage';
import CategoriesPage from './pages/CategoriesPage';
import WarehousesPage from './pages/WarehousesPage';
import ExportPage from './pages/ExportPage';
import OrdersPage from './pages/OrdersPage';
import StatisticsPage from './pages/StatisticsPage';

function Protected({ title, roles }) {
    return (
        <PrivateRoute roles={roles}>
            <Layout>
                <Placeholder title={title} />
            </Layout>
        </PrivateRoute>
    );
}

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<LoginPage />} />

                {/* Главные табы */}
                <Route
                    path="/my-orders"
                    element={
                        <PrivateRoute>
                            <Layout>
                                <MyOrdersPage />
                            </Layout>
                        </PrivateRoute>
                    }
                />
                <Route
                    path="/services"
                    element={
                        <PrivateRoute>
                            <Layout>
                                <ServicesPage />
                            </Layout>
                        </PrivateRoute>
                    }
                />
                <Route
                    path="/statistics"
                    element={
                        <PrivateRoute>
                            <Layout>
                                <StatisticsPage />
                            </Layout>
                        </PrivateRoute>
                    }
                />

                {/* Подстраницы сервисов */}
                <Route
                    path="/services/catalog"
                    element={
                        <PrivateRoute>
                            <Layout>
                                <CatalogPage />
                            </Layout>
                        </PrivateRoute>
                    }
                />
                <Route
                    path="/services/orders"
                    element={
                        <PrivateRoute roles={['MANAGER', 'ADMIN']}>
                            <Layout>
                                <OrdersPage />
                            </Layout>
                        </PrivateRoute>
                    }
                />
                <Route
                    path="/services/categories"
                    element={
                        <PrivateRoute>
                            <Layout>
                                <CategoriesPage />
                            </Layout>
                        </PrivateRoute>
                    }
                />
                <Route
                    path="/services/warehouses"
                    element={
                        <PrivateRoute>
                            <Layout>
                                <WarehousesPage />
                            </Layout>
                        </PrivateRoute>
                    }
                />
                <Route
                    path="/services/export"
                    element={
                        <PrivateRoute roles={['MANAGER', 'ADMIN']}>
                            <Layout>
                                <ExportPage />
                            </Layout>
                        </PrivateRoute>
                    }
                />
                <Route
                    path="/services/users"
                    element={
                        <PrivateRoute roles={['ADMIN']}>
                            <Layout>
                                <UsersPage />
                            </Layout>
                        </PrivateRoute>
                    }
                />

                <Route path="/" element={<Navigate to="/my-orders" replace />} />
                <Route path="*" element={<Navigate to="/my-orders" replace />} />
            </Routes>
        </BrowserRouter>
    );
}
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const { login } = useAuth();
    const navigate = useNavigate();

    async function handleSubmit(event) {
        event.preventDefault();
        setError('');
        setLoading(true);

        try {
            await login(username, password);
            navigate('/my-orders');
        } catch (e) {
            setError(e.message || 'Не удалось войти');
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="login-root">
            <div className="login-card">
                <div className="login-logo">
                    <img src="/logo.png" alt="АРГУС" />
                </div>
                <h1 className="login-title">АРГУС</h1>
                <p className="login-subtitle">Корпоративная система управления закупками</p>

                <form onSubmit={handleSubmit} style={{ width: '100%' }}>
                    {error && <div className="login-error">{error}</div>}

                    <div className="login-form-group">
                        <label className="login-form-label">
                            Имя пользователя <span className="login-form-required">*</span>
                        </label>
                        <input
                            className="login-input"
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="Введите имя пользователя"
                            autoComplete="username"
                            autoFocus
                            disabled={loading}
                            required
                        />
                    </div>

                    <div className="login-form-group">
                        <label className="login-form-label">
                            Пароль <span className="login-form-required">*</span>
                        </label>
                        <input
                            className="login-input"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Введите пароль"
                            autoComplete="current-password"
                            disabled={loading}
                            required
                        />
                    </div>

                    <button
                        type="submit"
                        className="btn btn-primary login-submit-btn"
                        disabled={loading}
                    >
                        {loading ? 'Вход...' : 'Войти в систему'}
                    </button>
                </form>
            </div>
        </div>
    );
}
import { createContext, useContext, useEffect, useState } from 'react';
import { clearTokens, getCurrentUser } from '../api/client';
import {
    login as loginApi,
    logout as logoutApi,
    loadCurrentUser,
} from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(getCurrentUser());
    const [loading, setLoading] = useState(true);

    // Жив ли токен
    useEffect(() => {
        async function init() {
            if (localStorage.getItem('accessToken')) {
                try {
                    const fresh = await loadCurrentUser();
                    setUser(fresh);
                } catch (e) {
                    // Токен протух и refresh не сработал — чистим
                    clearTokens();
                    setUser(null);
                }
            }
            setLoading(false);
        }
        init();
    }, []);

    async function login(username, password) {
        const userData = await loginApi(username, password);
        setUser(userData);
        return userData;
    }

    async function logout() {
        await logoutApi();
        setUser(null);
    }

    function updateUser(userData) {
        setUser(userData);
    }

    return (
        <AuthContext.Provider value={{ user, loading, login, logout, updateUser }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}
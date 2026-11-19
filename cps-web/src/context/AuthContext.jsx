import { useEffect, useState } from 'react';
import { clearTokens, getAccessToken, getRefreshToken, getCurrentUser, saveCurrentUser } from '../api/client';
import { login as loginApi, logout as logoutApi, loadCurrentUser } from '../api/auth';
import { AuthContext } from './useAuth';
export function AuthProvider({ children }) {
    const [user, setUser] = useState(getCurrentUser);
    const [loading, setLoading] = useState(true);
    useEffect(() => {
        let active = true;
        const expired = () => setUser(null);
        const storageChanged = (event) => { if (event.key === 'accessToken' && !event.newValue) expired(); };
        window.addEventListener('auth:expired', expired);
        window.addEventListener('storage', storageChanged);
        async function init() {
            try {
                const fresh = getAccessToken() || getRefreshToken() ? await loadCurrentUser() : null;
                if (active) setUser(fresh);
            } catch {
                clearTokens();
                if (active) setUser(null);
            } finally { if (active) setLoading(false); }
        }
        init();
        return () => { active = false; window.removeEventListener('auth:expired', expired); window.removeEventListener('storage', storageChanged); };
    }, []);
    async function login(username, password) {
        const fresh = await loginApi(username, password);
        setUser(fresh);
        return fresh;
    }
    async function logout() {
        try { await logoutApi(); } catch { /* Local sign-out must also work offline. */ }
        finally { setUser(null); }
    }
    function updateUser(fresh) { saveCurrentUser(fresh); setUser(fresh); }
    return <AuthContext.Provider value={{ user, loading, login, logout, updateUser }}>{children}</AuthContext.Provider>;
}

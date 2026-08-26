import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, clearToken, getToken, setToken } from './api.js';

const StoreContext = createContext(null);

export const useStore = () => useContext(StoreContext);

export function StoreProvider({ children }) {
  const [user, setUser] = useState(null);
  // Three states, not two. "No user yet" and "we have not looked" are different
  // things, and treating them the same is what makes a dashboard flash its login
  // screen for a moment on every refresh.
  const [ready, setReady] = useState(false);
  const [toast, setToast] = useState(null);

  const notify = useCallback((type, message) => {
    setToast({ type, message, key: Date.now() });
  }, []);

  const loadUser = useCallback(async () => {
    if (!getToken()) {
      setUser(null);
      setReady(true);
      return;
    }
    try {
      const data = await api.get('/auth/profile');
      setUser(data.user);
    } catch {
      setUser(null);
    } finally {
      setReady(true);
    }
  }, []);

  const signIn = useCallback(
    async (token) => {
      setToken(token);
      setReady(false);
      await loadUser();
    },
    [loadUser]
  );

  const signOut = useCallback(() => {
    clearToken();
    setUser(null);
  }, []);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  return (
    <StoreContext.Provider
      value={{
        user,
        ready,
        loadUser,
        signIn,
        signOut,
        toast,
        notify,
        dismissToast: () => setToast(null),
      }}
    >
      {children}
    </StoreContext.Provider>
  );
}

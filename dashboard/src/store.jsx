import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, clearTokens, getRefreshToken, getToken, setTokens } from './api.js';

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
    async (tokens) => {
      setTokens(tokens);
      setReady(false);
      await loadUser();
    },
    [loadUser]
  );

  const signOut = useCallback(async () => {
    // Telling the server is what actually ends the session - it revokes every token
    // from this sign-in. Clearing storage alone would only stop this browser using
    // tokens that still work everywhere else.
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      try {
        await api.post('/auth/logout', { refresh_token: refreshToken });
      } catch {
        // Offline, or the session was already gone. Sign out locally either way.
      }
    }
    clearTokens();
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

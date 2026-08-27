import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, clearToken, getToken, setToken } from './api.js';

const StoreContext = createContext(null);

export const useStore = () => useContext(StoreContext);

export function StoreProvider({ websiteId, children }) {
  const [user, setUser] = useState(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [comments, setComments] = useState([]);
  const [toast, setToast] = useState(null);

  const notify = useCallback((type, message) => {
    setToast({ type, message, key: Date.now() });
  }, []);

  const loadUser = useCallback(async () => {
    if (!getToken()) {
      setUser(null);
      setAuthChecked(true);
      return;
    }
    try {
      const data = await api.get('/auth/profile');
      setUser(data.user);
    } catch {
      // A token we cannot use is the same as no token. api.js has already
      // discarded it if the server said 401.
      setUser(null);
    } finally {
      setAuthChecked(true);
    }
  }, []);

  const loadComments = useCallback(async () => {
    if (!websiteId) return;
    try {
      const data = await api.get(`/comments/comment/${websiteId}`);
      setComments(data.comments ?? []);
    } catch (err) {
      notify('error', err.message);
    }
  }, [websiteId, notify]);

  const signIn = useCallback(
    async (token) => {
      setToken(token);
      await loadUser();
      // i_liked is per reader, so the list is now wrong for this reader.
      await loadComments();
    },
    [loadUser, loadComments]
  );

  const signOut = useCallback(async () => {
    clearToken();
    setUser(null);
    await loadComments();
  }, [loadComments]);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  useEffect(() => {
    loadComments();
  }, [loadComments]);

  return (
    <StoreContext.Provider
      value={{
        websiteId,
        user,
        authChecked,
        comments,
        setComments,
        loadComments,
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

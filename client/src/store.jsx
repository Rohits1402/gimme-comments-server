import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, clearToken, getToken, setToken } from './api.js';

const StoreContext = createContext(null);

export const useStore = () => useContext(StoreContext);

export function StoreProvider({ websiteId, children }) {
  const [user, setUser] = useState(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [comments, setComments] = useState([]);
  const [total, setTotal] = useState(0);
  const [cursor, setCursor] = useState(null);
  const [loadingMore, setLoadingMore] = useState(false);
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
      setTotal(data.total_comments ?? 0);
      setCursor(data.next_cursor ?? null);
    } catch (err) {
      notify('error', err.message);
    }
  }, [websiteId, notify]);

  /**
   * The next page of threads. The server pages by cursor, so this cannot be
   * expressed as a page number and does not need to be: there is only ever
   * "the batch after the one we already hold".
   */
  const loadMore = useCallback(async () => {
    // Without the loadingMore guard a double click sends the same cursor twice
    // and appends the same page twice.
    if (!websiteId || !cursor || loadingMore) return;
    setLoadingMore(true);
    try {
      const data = await api.get(
        `/comments/comment/${websiteId}?cursor=${encodeURIComponent(cursor)}`
      );
      // Append, never replace. The reader is holding their place in the list.
      setComments((current) => [...current, ...(data.comments ?? [])]);
      setTotal(data.total_comments ?? 0);
      setCursor(data.next_cursor ?? null);
    } catch (err) {
      notify('error', err.message);
    } finally {
      setLoadingMore(false);
    }
  }, [websiteId, cursor, loadingMore, notify]);

  /**
   * The three writes below change the list in place instead of reloading it.
   * loadComments now means "go back to page one", so calling it after a write
   * would throw away every page the reader had loaded — they delete one comment
   * and lose their place in the thread. The like button already worked this way;
   * these three now match it.
   */

  const addComment = useCallback((comment) => {
    setComments((current) => [...current, comment]);
    setTotal((n) => n + 1);
  }, []);

  const replaceComment = useCallback((comment) => {
    // Merged, not swapped: the create and update responses carry no like data,
    // so a straight replace would blank out liked_by and i_liked.
    setComments((current) =>
      current.map((c) => (c.id === comment.id ? { ...c, ...comment } : c))
    );
  }, []);

  /**
   * A deleted comment takes its replies with it, because the database removes
   * them by cascade. Left behind, they would be replies with no parent — which
   * buildTree drops without a word.
   */
  const removeComment = useCallback(
    (id) => {
      const doomed = new Set([id]);
      let grew = true;
      while (grew) {
        grew = false;
        for (const c of comments) {
          if (!doomed.has(c.id) && c.comment_parent && doomed.has(c.comment_parent)) {
            doomed.add(c.id);
            grew = true;
          }
        }
      }
      setComments((current) => current.filter((c) => !doomed.has(c.id)));
      setTotal((n) => Math.max(0, n - doomed.size));
    },
    [comments]
  );

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
        total,
        // Derived, not stored: "is there more" and "where does more start" are
        // one fact, and two copies of one fact eventually disagree.
        hasMore: cursor !== null,
        loadMore,
        loadingMore,
        addComment,
        replaceComment,
        removeComment,
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

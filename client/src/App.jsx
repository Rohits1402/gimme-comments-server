import { useEffect, useState } from 'react';
import { api } from './api.js';
import { StoreProvider, useStore } from './store.jsx';
import { Avatar, Toast } from './ui.jsx';
import Auth from './Auth.jsx';
import Comments from './Comments.jsx';
import Profile from './Profile.jsx';

function Header({ view, setView }) {
  const { user, authChecked } = useStore();

  return (
    <header className="gc-header">
      <span className="gc-brand">Comments</span>
      {authChecked && user ? (
        <button
          type="button"
          className="gc-account"
          onClick={() => setView(view === 'profile' ? 'comments' : 'profile')}
        >
          <Avatar user={user} size={26} />
          <span className="gc-account-name">{user.name}</span>
        </button>
      ) : null}
    </header>
  );
}

function Widget({ theme }) {
  const { user } = useStore();
  const [view, setView] = useState('comments');

  // Signing out while looking at the profile would leave an empty panel.
  useEffect(() => {
    if (!user && view === 'profile') setView('comments');
  }, [user, view]);

  return (
    <div className={`gc-root gc-theme-${theme}`}>
      <Header view={view} setView={setView} />

      {view === 'profile' ? (
        <Profile onClose={() => setView('comments')} />
      ) : view === 'auth' ? (
        <Auth onCancel={() => setView('comments')} />
      ) : (
        <Comments onRequireAuth={() => setView('auth')} />
      )}

      <Toast />

      <footer className="gc-footer">
        Powered by <span className="gc-footer-brand">GimmeComments</span>
      </footer>
    </div>
  );
}

export default function App({ websiteId, theme }) {
  const [check, setCheck] = useState({ status: 'checking' });

  useEffect(() => {
    if (!websiteId) {
      setCheck({
        status: 'error',
        message:
          'This comment box has no website id. Add data-gimme_comments_website_id to the container element.',
      });
      return;
    }

    let cancelled = false;
    api
      .get(`/websites/exists/${websiteId}`)
      .then(() => !cancelled && setCheck({ status: 'ok' }))
      .catch((err) => {
        if (cancelled) return;
        setCheck({
          status: 'error',
          message:
            err.status === 404
              ? 'GimmeComments is not activated for this website.'
              : err.message,
        });
      });

    return () => {
      cancelled = true;
    };
  }, [websiteId]);

  if (check.status === 'checking') {
    return (
      <div className={`gc-root gc-theme-${theme}`}>
        <div className="gc-loading">
          <span className="gc-spinner" /> Loading comments…
        </div>
      </div>
    );
  }

  if (check.status === 'error') {
    return (
      <div className={`gc-root gc-theme-${theme}`}>
        <div className="gc-alert">{check.message}</div>
      </div>
    );
  }

  return (
    <StoreProvider websiteId={websiteId}>
      <Widget theme={theme} />
    </StoreProvider>
  );
}

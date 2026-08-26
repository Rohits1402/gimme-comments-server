import { Navigate, NavLink, Route, Routes, useLocation } from 'react-router-dom';
import { StoreProvider, useStore } from './store.jsx';
import { Avatar, Toast } from './ui.jsx';
import { Logo, LogoMark } from './Logo.jsx';
import Landing from './pages/Landing.jsx';
import Auth from './pages/Auth.jsx';
import Websites from './pages/Websites.jsx';
import WebsiteDetail from './pages/WebsiteDetail.jsx';
import Profile from './pages/Profile.jsx';

function Booting() {
  return (
    <div className="gc-boot">
      <span className="gc-spinner" />
    </div>
  );
}

function Shell({ children }) {
  const { user } = useStore();

  return (
    <div className="gc-app">
      <header className="gc-topbar">
        <NavLink to="/websites" className="gc-logo">
          <Logo size={24} />
        </NavLink>
        <nav className="gc-nav">
          <NavLink to="/" end className="gc-nav-link">
            Home
          </NavLink>
          <NavLink to="/websites" className="gc-nav-link">
            Websites
          </NavLink>
          <NavLink to="/account" className="gc-nav-link gc-nav-account">
            <Avatar user={user} size={24} />
            <span>{user?.name}</span>
          </NavLink>
        </nav>
      </header>

      <main className="gc-main">{children}</main>

      <footer className="gc-appfoot">
        <span className="gc-wordmark">
          <LogoMark size={16} />
          <span>GimmeComments</span>
        </span>
        <div>
          <a href="/swagger-ui.html">API docs</a>
          <a
            href="https://github.com/Rohits1402/gimme-comments-server"
            target="_blank"
            rel="noopener noreferrer"
          >
            GitHub
          </a>
          <span>MIT</span>
        </div>
      </footer>

      <Toast />
    </div>
  );
}

/** Waits for the profile call before deciding. Without that, a refresh would bounce
 *  a signed-in reader to the sign-in screen for a frame. */
function Protected({ children }) {
  const { user, ready } = useStore();
  const location = useLocation();

  if (!ready) return <Booting />;
  if (!user) return <Navigate to="/sign-in" replace state={{ from: location }} />;
  return <Shell>{children}</Shell>;
}

function PublicOnly({ children }) {
  const { user, ready } = useStore();

  if (!ready) return <Booting />;
  if (user) return <Navigate to="/websites" replace />;
  return (
    <>
      {children}
      <Toast />
    </>
  );
}

export default function App() {
  return (
    <StoreProvider>
      <Routes>
        {/* Reachable signed in or out. Somebody who clicks the logo wants the
            home page, not to be bounced to a list of their websites. */}
        <Route path="/" element={<Landing />} />
        <Route path="/sign-in" element={<PublicOnly><Auth /></PublicOnly>} />
        <Route path="/websites" element={<Protected><Websites /></Protected>} />
        <Route path="/websites/:id" element={<Protected><WebsiteDetail /></Protected>} />
        <Route path="/account" element={<Protected><Profile /></Protected>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </StoreProvider>
  );
}

import { Navigate, Route, Routes, NavLink, useLocation } from 'react-router-dom';
import { StoreProvider, useStore } from './store.jsx';
import { Avatar, Toast } from './ui.jsx';
import Auth from './pages/Auth.jsx';
import Websites from './pages/Websites.jsx';
import WebsiteDetail from './pages/WebsiteDetail.jsx';
import Profile from './pages/Profile.jsx';

function Shell({ children }) {
  const { user } = useStore();

  return (
    <div className="gc-app">
      <header className="gc-topbar">
        <NavLink to="/websites" className="gc-logo">
          GimmeComments
        </NavLink>
        <nav className="gc-nav">
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
      <Toast />
    </div>
  );
}

/** Waits for the profile call before deciding. Without that, a refresh would bounce
 *  a signed-in user to the login screen for a frame. */
function Protected({ children }) {
  const { user, ready } = useStore();
  const location = useLocation();

  if (!ready) {
    return (
      <div className="gc-boot">
        <span className="gc-spinner" />
      </div>
    );
  }
  if (!user) return <Navigate to="/" replace state={{ from: location }} />;
  return <Shell>{children}</Shell>;
}

function Landing() {
  const { user, ready } = useStore();

  if (!ready) {
    return (
      <div className="gc-boot">
        <span className="gc-spinner" />
      </div>
    );
  }
  if (user) return <Navigate to="/websites" replace />;
  return (
    <>
      <Auth />
      <Toast />
    </>
  );
}

function Router() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/websites" element={<Protected><Websites /></Protected>} />
      <Route path="/websites/:id" element={<Protected><WebsiteDetail /></Protected>} />
      <Route path="/account" element={<Protected><Profile /></Protected>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <StoreProvider>
      <Router />
    </StoreProvider>
  );
}

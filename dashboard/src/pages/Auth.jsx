import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useStore } from '../store.jsx';
import { Button, Field } from '../ui.jsx';
import { Logo, LogoMark } from '../Logo.jsx';

function useSubmit() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const run = async (fn) => {
    setBusy(true);
    setError(null);
    try {
      await fn();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };
  return { busy, error, setError, run };
}

const POINTS = [
  'Threaded replies, likes and moderation',
  "Matches your site's light or dark design",
  'No trackers, no third-party cookies',
];

function Shell({ title, subtitle, onSubmit, error, children, footer }) {
  return (
    <div className="gc-auth-page">
      <header className="gc-auth-nav">
        <Link to="/" className="gc-auth-home">
          <Logo size={22} />
        </Link>
      </header>

      <div className="gc-auth-body">
        <aside className="gc-auth-aside">
          <h2>
            Comments on your site
            <br />
            in two lines of HTML.
          </h2>
          <ul>
            {POINTS.map((p) => (
              <li key={p}>
                <span className="gc-tick" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                </span>
                {p}
              </li>
            ))}
          </ul>
        </aside>

        <form
          className="gc-auth-card"
          onSubmit={(e) => {
            e.preventDefault();
            onSubmit();
          }}
        >
          <h1 className="gc-auth-title">{title}</h1>
          {subtitle ? <p className="gc-auth-sub">{subtitle}</p> : null}
          {children}
          {error ? <p className="gc-form-error">{error}</p> : null}
          {footer}
        </form>
      </div>

      <footer className="gc-auth-foot">
        <span className="gc-wordmark">
          <LogoMark size={15} />
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
    </div>
  );
}

export default function Auth() {
  const navigate = useNavigate();
  const { signIn, notify } = useStore();

  const [mode, setMode] = useState('login');
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [resetStep, setResetStep] = useState('request');

  const { busy, error, setError, run } = useSubmit();
  const resend = useSubmit();

  const login = () =>
    run(async () => {
      try {
        const data = await api.post('/auth/login', { email, password });
        await signIn(data.token);
        navigate('/websites');
      } catch (err) {
        // The server tells these apart deliberately, so send them somewhere useful.
        if (/not verified/i.test(err.message)) {
          setError(null);
          setMode('verify');
          return;
        }
        throw err;
      }
    });

  const signUp = () =>
    run(async () => {
      await api.post('/auth/register', { name, email, password });
      await api.post('/auth/account-verification/generate-otp', { email });
      setMode('verify');
    });

  const verify = () =>
    run(async () => {
      // Text, never Number(otp): a code beginning with a zero would lose it.
      await api.post('/auth/account-verification/verify-account', { email, otp });
      notify('success', 'Email confirmed. Sign in to continue.');
      setMode('login');
    });

  const reset = () =>
    run(async () => {
      if (resetStep === 'request') {
        await api.post('/auth/forget-password/generate-otp', { email });
        setResetStep('code');
        return;
      }
      if (resetStep === 'code') {
        await api.post('/auth/forget-password/verify-otp', { email, otp });
        setResetStep('reset');
        return;
      }
      await api.patch('/auth/forget-password/change-password', {
        email,
        otp,
        new_password: newPassword,
      });
      notify('success', 'Password changed.');
      setResetStep('request');
      setMode('login');
    });

  const emailField = (id, extra = {}) => (
    <Field
      id={id}
      label="Email"
      type="email"
      autoComplete="email"
      required
      value={email}
      onChange={(e) => setEmail(e.target.value)}
      {...extra}
    />
  );

  const otpField = (id, extra = {}) => (
    <Field
      id={id}
      label="Six digit code"
      inputMode="numeric"
      autoComplete="one-time-code"
      required
      maxLength={6}
      placeholder="000000"
      value={otp}
      onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
      {...extra}
    />
  );

  if (mode === 'signup') {
    return (
      <Shell
        title="Create your account"
        subtitle="You will get a six digit code by email to confirm the address."
        onSubmit={signUp}
        error={error}
        footer={
          <>
            <Button variant="primary" type="submit" busy={busy}>
              Create account
            </Button>
            <p className="gc-auth-alt">
              Already have one?{' '}
              <button type="button" className="gc-link" onClick={() => setMode('login')}>
                Sign in
              </button>
            </p>
          </>
        }
      >
        <Field
          id="name"
          label="Name"
          autoComplete="name"
          required
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        {emailField('signup-email')}
        <Field
          id="signup-password"
          label="Password"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          hint="At least 8 characters."
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      </Shell>
    );
  }

  if (mode === 'verify') {
    return (
      <Shell
        title="Confirm your email"
        subtitle={`Enter the code sent to ${email || 'your email address'}.`}
        onSubmit={verify}
        error={error || resend.error}
        footer={
          <>
            <Button variant="primary" type="submit" busy={busy}>
              Confirm
            </Button>
            <p className="gc-auth-alt">
              <button
                type="button"
                className="gc-link"
                disabled={resend.busy}
                onClick={() =>
                  resend.run(async () => {
                    await api.post('/auth/account-verification/generate-otp', { email });
                    notify('success', 'A new code is on its way.');
                  })
                }
              >
                Send another code
              </button>
              {' · '}
              <button type="button" className="gc-link" onClick={() => setMode('login')}>
                Back to sign in
              </button>
            </p>
          </>
        }
      >
        {emailField('verify-email')}
        {otpField('verify-otp')}
      </Shell>
    );
  }

  if (mode === 'forgot') {
    return (
      <Shell
        title="Reset your password"
        subtitle={
          resetStep === 'request'
            ? 'We will email a code if this address has an account.'
            : resetStep === 'code'
              ? 'Enter the code from the email.'
              : 'Choose a new password.'
        }
        onSubmit={reset}
        error={error}
        footer={
          <>
            <Button variant="primary" type="submit" busy={busy}>
              {resetStep === 'request'
                ? 'Send code'
                : resetStep === 'code'
                  ? 'Check code'
                  : 'Change password'}
            </Button>
            <p className="gc-auth-alt">
              <button type="button" className="gc-link" onClick={() => setMode('login')}>
                Back to sign in
              </button>
            </p>
          </>
        }
      >
        {emailField('forgot-email', { readOnly: resetStep !== 'request' })}
        {resetStep !== 'request' ? otpField('forgot-otp', { readOnly: resetStep === 'reset' }) : null}
        {resetStep === 'reset' ? (
          <Field
            id="forgot-new"
            label="New password"
            type="password"
            autoComplete="new-password"
            required
            minLength={8}
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />
        ) : null}
      </Shell>
    );
  }

  return (
    <Shell
      title="Sign in"
      subtitle="Manage the websites using your comment widget."
      onSubmit={login}
      error={error}
      footer={
        <>
          <Button variant="primary" type="submit" busy={busy}>
            Sign in
          </Button>
          <p className="gc-auth-alt">
            <button type="button" className="gc-link" onClick={() => setMode('signup')}>
              Create an account
            </button>
            {' · '}
            <button type="button" className="gc-link" onClick={() => setMode('forgot')}>
              Forgot password
            </button>
          </p>
        </>
      }
    >
      {emailField('login-email')}
      <Field
        id="login-password"
        label="Password"
        type="password"
        autoComplete="current-password"
        required
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
    </Shell>
  );
}

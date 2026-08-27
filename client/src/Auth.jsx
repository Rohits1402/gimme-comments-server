import { useState } from 'react';
import { api } from './api.js';
import { useStore } from './store.jsx';
import { Button, Field } from './ui.jsx';

/** Shared by every form here: run the call, surface the failure, never leave a spinner running. */
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

function Panel({ title, subtitle, onSubmit, error, children, footer }) {
  return (
    <form
      className="gc-panel"
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit();
      }}
    >
      <div className="gc-panel-head">
        <h3 className="gc-panel-title">{title}</h3>
        {subtitle ? <p className="gc-panel-sub">{subtitle}</p> : null}
      </div>
      {children}
      {error ? <p className="gc-form-error">{error}</p> : null}
      {footer}
    </form>
  );
}

function Login({ go, email, setEmail }) {
  const { signIn, notify } = useStore();
  const [password, setPassword] = useState('');
  const { busy, error, setError, run } = useSubmit();

  const submit = () =>
    run(async () => {
      try {
        const data = await api.post('/auth/login', { email, password });
        await signIn(data.token);
        notify('success', 'Signed in.');
      } catch (err) {
        // The server distinguishes "not verified" from "wrong password" on
        // purpose, so send the reader somewhere useful instead of just saying no.
        if (/not verified/i.test(err.message)) {
          setError(null);
          go('verify');
          return;
        }
        throw err;
      }
    });

  return (
    <Panel
      title="Sign in to comment"
      onSubmit={submit}
      error={error}
      footer={
        <>
          <Button variant="primary" type="submit" busy={busy}>
            Sign in
          </Button>
          <div className="gc-panel-links">
            <button type="button" className="gc-link" onClick={() => go('signup')}>
              Create an account
            </button>
            <button type="button" className="gc-link" onClick={() => go('forgot')}>
              Forgot password
            </button>
          </div>
        </>
      }
    >
      <Field
        id="gc-login-email"
        label="Email"
        type="email"
        autoComplete="email"
        required
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <Field
        id="gc-login-password"
        label="Password"
        type="password"
        autoComplete="current-password"
        required
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
    </Panel>
  );
}

function SignUp({ go, email, setEmail }) {
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const { busy, error, run } = useSubmit();

  const submit = () =>
    run(async () => {
      await api.post('/auth/register', { name, email, password });
      await api.post('/auth/account-verification/generate-otp', { email });
      go('verify');
    });

  return (
    <Panel
      title="Create an account"
      subtitle="You will get a six digit code by email to confirm the address."
      onSubmit={submit}
      error={error}
      footer={
        <>
          <Button variant="primary" type="submit" busy={busy}>
            Create account
          </Button>
          <div className="gc-panel-links">
            <button type="button" className="gc-link" onClick={() => go('login')}>
              I already have an account
            </button>
          </div>
        </>
      }
    >
      <Field
        id="gc-signup-name"
        label="Name"
        autoComplete="name"
        required
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      <Field
        id="gc-signup-email"
        label="Email"
        type="email"
        autoComplete="email"
        required
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <Field
        id="gc-signup-password"
        label="Password"
        type="password"
        autoComplete="new-password"
        required
        minLength={8}
        hint="At least 8 characters."
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
    </Panel>
  );
}

function Verify({ go, email, setEmail }) {
  const { notify } = useStore();
  const [otp, setOtp] = useState('');
  const { busy, error, run } = useSubmit();
  const resend = useSubmit();

  const submit = () =>
    run(async () => {
      // Sent as text, never Number(otp): a code such as 034512 would lose its
      // leading zero and be rejected, one time in ten.
      await api.post('/auth/account-verification/verify-account', { email, otp });
      notify('success', 'Email confirmed. You can sign in now.');
      go('login');
    });

  return (
    <Panel
      title="Confirm your email"
      subtitle={`Enter the six digit code sent to ${email || 'your email address'}.`}
      onSubmit={submit}
      error={error || resend.error}
      footer={
        <>
          <Button variant="primary" type="submit" busy={busy}>
            Confirm
          </Button>
          <div className="gc-panel-links">
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
            <button type="button" className="gc-link" onClick={() => go('login')}>
              Back to sign in
            </button>
          </div>
        </>
      }
    >
      <Field
        id="gc-verify-email"
        label="Email"
        type="email"
        required
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      <Field
        id="gc-verify-otp"
        label="Code"
        inputMode="numeric"
        autoComplete="one-time-code"
        required
        maxLength={6}
        placeholder="000000"
        value={otp}
        onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
      />
    </Panel>
  );
}

function Forgot({ go, email, setEmail }) {
  const { notify } = useStore();
  const [step, setStep] = useState('request');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const { busy, error, run } = useSubmit();

  const submit = () =>
    run(async () => {
      if (step === 'request') {
        await api.post('/auth/forget-password/generate-otp', { email });
        setStep('code');
        return;
      }
      if (step === 'code') {
        await api.post('/auth/forget-password/verify-otp', { email, otp });
        setStep('reset');
        return;
      }
      await api.patch('/auth/forget-password/change-password', {
        email,
        otp,
        new_password: newPassword,
      });
      notify('success', 'Password changed. Sign in with the new one.');
      go('login');
    });

  const label =
    step === 'request' ? 'Send code' : step === 'code' ? 'Check code' : 'Change password';

  return (
    <Panel
      title="Reset your password"
      subtitle={
        step === 'request'
          ? 'We will email a six digit code if this address has an account.'
          : step === 'code'
            ? 'Enter the code from the email.'
            : 'Choose a new password.'
      }
      onSubmit={submit}
      error={error}
      footer={
        <>
          <Button variant="primary" type="submit" busy={busy}>
            {label}
          </Button>
          <div className="gc-panel-links">
            <button type="button" className="gc-link" onClick={() => go('login')}>
              Back to sign in
            </button>
          </div>
        </>
      }
    >
      <Field
        id="gc-forgot-email"
        label="Email"
        type="email"
        required
        readOnly={step !== 'request'}
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      {step !== 'request' ? (
        <Field
          id="gc-forgot-otp"
          label="Code"
          inputMode="numeric"
          autoComplete="one-time-code"
          required
          maxLength={6}
          readOnly={step === 'reset'}
          value={otp}
          onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
        />
      ) : null}
      {step === 'reset' ? (
        <Field
          id="gc-forgot-new"
          label="New password"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          hint="At least 8 characters."
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
        />
      ) : null}
    </Panel>
  );
}

export default function Auth({ onCancel }) {
  const [mode, setMode] = useState('login');
  // Shared across modes so the address survives sign up to confirm to sign in.
  const [email, setEmail] = useState('');

  const props = { go: setMode, email, setEmail };

  return (
    <div className="gc-auth">
      {onCancel ? (
        <button type="button" className="gc-link gc-auth-cancel" onClick={onCancel}>
          Cancel
        </button>
      ) : null}
      {mode === 'login' ? <Login {...props} /> : null}
      {mode === 'signup' ? <SignUp {...props} /> : null}
      {mode === 'verify' ? <Verify {...props} /> : null}
      {mode === 'forgot' ? <Forgot {...props} /> : null}
    </div>
  );
}

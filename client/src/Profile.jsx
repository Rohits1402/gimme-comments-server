import { useRef, useState } from 'react';
import { api } from './api.js';
import { useStore } from './store.jsx';
import { Avatar, Button, Field } from './ui.jsx';

const GENDERS = ['MALE', 'FEMALE', 'OTHERS'];

function Section({ title, children }) {
  return (
    <section className="gc-profile-section">
      <h4 className="gc-profile-title">{title}</h4>
      {children}
    </section>
  );
}

export default function Profile({ onClose }) {
  const { user, loadUser, loadComments, signOut, notify } = useStore();

  const [name, setName] = useState(user?.name ?? '');
  const [gender, setGender] = useState(user?.gender ?? 'MALE');
  const [birthday, setBirthday] = useState(user?.birthday ?? '');
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [busy, setBusy] = useState(null);
  const fileInput = useRef(null);

  const run = async (key, fn, successMessage) => {
    setBusy(key);
    try {
      await fn();
      if (successMessage) notify('success', successMessage);
    } catch (err) {
      notify('error', err.message);
    } finally {
      setBusy(null);
    }
  };

  const saveDetails = (e) => {
    e.preventDefault();
    run(
      'details',
      async () => {
        await api.patch('/auth/profile/update-profile', { name, gender, birthday });
        await loadUser();
        // The name shown on this reader's existing comments is now stale.
        await loadComments();
      },
      'Profile updated.'
    );
  };

  const savePassword = (e) => {
    e.preventDefault();
    run(
      'password',
      async () => {
        await api.patch('/auth/profile/update-password', {
          old_password: oldPassword,
          new_password: newPassword,
        });
        setOldPassword('');
        setNewPassword('');
      },
      'Password changed.'
    );
  };

  const uploadImage = (file) => {
    if (!file) return;
    const body = new FormData();
    body.append('profile_image', file);
    run(
      'image',
      async () => {
        await api.patch('/auth/profile/update-profile-image', body);
        await loadUser();
        await loadComments();
      },
      'Picture updated.'
    );
  };

  const deleteAccount = () => {
    run(
      'delete',
      async () => {
        await api.delete('/auth/profile/delete-profile');
        await signOut();
        onClose();
      },
      'Account deleted.'
    );
  };

  return (
    <div className="gc-panel gc-profile">
      <div className="gc-panel-head gc-row gc-row-between">
        <h3 className="gc-panel-title">Your account</h3>
        <button type="button" className="gc-link" onClick={onClose}>
          Back to comments
        </button>
      </div>

      <div className="gc-profile-identity">
        <Avatar user={user} size={56} />
        <div>
          <div className="gc-name">{user?.name}</div>
          <div className="gc-muted gc-small">{user?.email}</div>
        </div>
        <Button
          size="sm"
          busy={busy === 'image'}
          onClick={() => fileInput.current?.click()}
        >
          Change picture
        </Button>
        <input
          ref={fileInput}
          type="file"
          accept="image/*"
          hidden
          onChange={(e) => {
            uploadImage(e.target.files?.[0]);
            e.target.value = '';
          }}
        />
      </div>

      <Section title="Details">
        <form onSubmit={saveDetails}>
          <Field
            id="gc-profile-name"
            label="Name"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <label className="gc-field" htmlFor="gc-profile-gender">
            <span className="gc-field-label">Gender</span>
            <select
              id="gc-profile-gender"
              className="gc-input"
              value={gender}
              onChange={(e) => setGender(e.target.value)}
            >
              {GENDERS.map((g) => (
                <option key={g} value={g}>
                  {g.charAt(0) + g.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </label>
          <Field
            id="gc-profile-birthday"
            label="Birthday"
            type="date"
            value={birthday}
            onChange={(e) => setBirthday(e.target.value)}
          />
          <Button variant="primary" size="sm" type="submit" busy={busy === 'details'}>
            Save details
          </Button>
        </form>
      </Section>

      <Section title="Password">
        <form onSubmit={savePassword}>
          <Field
            id="gc-profile-old"
            label="Current password"
            type="password"
            autoComplete="current-password"
            required
            value={oldPassword}
            onChange={(e) => setOldPassword(e.target.value)}
          />
          <Field
            id="gc-profile-new"
            label="New password"
            type="password"
            autoComplete="new-password"
            required
            minLength={8}
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />
          <Button variant="primary" size="sm" type="submit" busy={busy === 'password'}>
            Change password
          </Button>
        </form>
      </Section>

      <Section title="Danger zone">
        <p className="gc-muted gc-small">
          Deleting your account removes your profile and your websites. Comments you
          have written stay on the page, shown as written by a deleted user.
        </p>
        <div className="gc-row">
          <Button size="sm" onClick={signOut}>
            Sign out
          </Button>
          <Button
            variant="danger"
            size="sm"
            busy={busy === 'delete'}
            onClick={deleteAccount}
          >
            Delete account
          </Button>
        </div>
      </Section>
    </div>
  );
}

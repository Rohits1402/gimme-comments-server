import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useStore } from '../store.jsx';
import { Avatar, Button, Card, ConfirmButton, Field, IconTrash } from '../ui.jsx';

const GENDERS = ['MALE', 'FEMALE', 'OTHERS'];

export default function Profile() {
  const navigate = useNavigate();
  const { user, loadUser, signOut, notify } = useStore();

  const [name, setName] = useState(user?.name ?? '');
  const [gender, setGender] = useState(user?.gender ?? 'MALE');
  const [birthday, setBirthday] = useState(user?.birthday ?? '');
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [busy, setBusy] = useState(null);
  const fileInput = useRef(null);

  const run = async (key, fn, message) => {
    setBusy(key);
    try {
      await fn();
      if (message) notify('success', message);
    } catch (err) {
      notify('error', err.message);
    } finally {
      setBusy(null);
    }
  };

  return (
    <>
      <div className="gc-page-head">
        <div>
          <h1 className="gc-page-title">Account</h1>
          <p className="gc-page-sub">{user?.email}</p>
        </div>
        <Button onClick={() => { signOut(); navigate('/'); }}>Sign out</Button>
      </div>

      <Card title="Picture">
        <div className="gc-row">
          <Avatar user={user} size={64} />
          <Button size="sm" busy={busy === 'image'} onClick={() => fileInput.current?.click()}>
            Change picture
          </Button>
          <input
            ref={fileInput}
            type="file"
            accept="image/*"
            hidden
            onChange={(e) => {
              const file = e.target.files?.[0];
              e.target.value = '';
              if (!file) return;
              const body = new FormData();
              body.append('profile_image', file);
              run('image', async () => {
                await api.patch('/auth/profile/update-profile-image', body);
                await loadUser();
              }, 'Picture updated.');
            }}
          />
        </div>
        <p className="gc-card-note">
          This is what readers see beside your comments.
        </p>
      </Card>

      <Card title="Details">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            run('details', async () => {
              await api.patch('/auth/profile/update-profile', { name, gender, birthday });
              await loadUser();
            }, 'Details saved.');
          }}
        >
          <Field id="p-name" label="Name" required value={name} onChange={(e) => setName(e.target.value)} />
          <div className="gc-field-row">
            <label className="gc-field" htmlFor="p-gender">
              <span className="gc-field-label">Gender</span>
              <select id="p-gender" className="gc-input" value={gender} onChange={(e) => setGender(e.target.value)}>
                {GENDERS.map((g) => (
                  <option key={g} value={g}>
                    {g.charAt(0) + g.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            </label>
            <Field
              id="p-birthday"
              label="Birthday"
              type="date"
              value={birthday}
              onChange={(e) => setBirthday(e.target.value)}
            />
          </div>
          <Button variant="primary" type="submit" busy={busy === 'details'}>
            Save details
          </Button>
        </form>
      </Card>

      <Card title="Password">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            run('password', async () => {
              await api.patch('/auth/profile/update-password', {
                old_password: oldPassword,
                new_password: newPassword,
              });
              setOldPassword('');
              setNewPassword('');
            }, 'Password changed.');
          }}
        >
          <div className="gc-field-row">
            <Field
              id="p-old"
              label="Current password"
              type="password"
              autoComplete="current-password"
              required
              value={oldPassword}
              onChange={(e) => setOldPassword(e.target.value)}
            />
            <Field
              id="p-new"
              label="New password"
              type="password"
              autoComplete="new-password"
              required
              minLength={8}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />
          </div>
          <Button variant="primary" type="submit" busy={busy === 'password'}>
            Change password
          </Button>
        </form>
      </Card>

      <Card title="Danger zone">
        <p className="gc-card-note">
          Deleting your account removes your profile and every website you registered,
          along with the comments on them. Comments you wrote on other people&rsquo;s
          sites stay, shown as written by a deleted user.
        </p>
        <ConfirmButton
          question="Delete your account permanently?"
          busy={busy === 'delete'}
          onConfirm={() =>
            run('delete', async () => {
              await api.delete('/auth/profile/delete-profile');
              signOut();
              navigate('/');
            })
          }
        >
          <IconTrash />
          Delete account
        </ConfirmButton>
      </Card>
    </>
  );
}

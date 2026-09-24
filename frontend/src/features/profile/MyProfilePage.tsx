import { useMutation, useQuery } from '@tanstack/react-query';
import { KeyRound } from 'lucide-react';
import { useState } from 'react';
import { api } from '@/api/client';
import { systemApi } from '@/api/system';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useWorkspace } from '@/context/workspaceContext';
import { formatAmount, formatDateTime, humanize } from '@/utils/format';
import { passwordProblems } from './passwordRules';

interface PasswordForm {
  current: string;
  next: string;
  confirm: string;
}

const EMPTY: PasswordForm = { current: '', next: '', confirm: '' };

function ChangePassword() {
  const toast = useToast();
  const [form, setForm] = useState<PasswordForm>(EMPTY);
  const problems = passwordProblems(form.next, form.confirm);
  const change = useMutation({
    mutationFn: (f: PasswordForm) =>
      api.post<undefined>('/auth/change-password', {
        currentPassword: f.current,
        newPassword: f.next,
      }),
    onSuccess: () => {
      setForm(EMPTY);
      toast.success('Password changed');
    },
  });
  const fields: [keyof PasswordForm, string, string][] = [
    ['current', 'Current password', 'current-password'],
    ['next', 'New password', 'new-password'],
    ['confirm', 'Confirm new password', 'new-password'],
  ];
  return (
    <Card title="Change password">
      <div className="stack">
        <ErrorAlert error={change.error} />
        <div className="form-grid">
          {fields.map(([key, label, autoComplete]) => (
            <Field key={key} label={label} required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type="password"
                  autoComplete={autoComplete}
                  value={form[key]}
                  onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                />
              )}
            </Field>
          ))}
        </div>
        {form.next !== '' && problems.length > 0 && (
          <ul className="muted">
            {problems.map((p) => (
              <li key={p}>{p}</li>
            ))}
          </ul>
        )}
        <div>
          <Button
            variant="accent"
            icon={<KeyRound size={16} />}
            busy={change.isPending}
            disabled={form.current === '' || problems.length > 0}
            onClick={() => change.mutate(form)}
          >
            Change Password
          </Button>
        </div>
      </div>
    </Card>
  );
}

/** The signed-in user's profile: details, roles, permissions, session policy and password. */
export default function MyProfilePage() {
  const { user } = useAuth();
  const { branches } = useWorkspace();
  const policy = useQuery({ queryKey: ['session-policy'], queryFn: systemApi.sessionPolicy });
  if (user === null) {
    return null;
  }
  const home = branches.find((b) => b.id === user.homeBranchId);
  const details: [string, string][] = [
    ['User name', user.username],
    ['Full name', user.fullName],
    ['Email', user.email ?? '—'],
    ['Home branch', home === undefined ? '—' : `${home.code} – ${home.name}`],
    [
      'Authorization limit',
      user.authorizationLimit === undefined ? 'Unlimited' : formatAmount(user.authorizationLimit),
    ],
    ['Last sign-in', formatDateTime(user.lastLoginAt) || '—'],
    ['Automatic sign-out', `After ${policy.data?.timeoutMinutes ?? 30} minutes of inactivity`],
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Account"
        title="My Profile"
        description="Your account, access rights and password."
      />
      <div className="grid-2">
        <Card title="Details">
          <dl className="form-grid" style={{ margin: 0 }}>
            {details.map(([label, value]) => (
              <div key={label}>
                <dt className="muted">{label}</dt>
                <dd style={{ margin: 0, fontWeight: 600 }}>{value}</dd>
              </div>
            ))}
          </dl>
        </Card>
        <Card title="Roles and permissions">
          <div className="stack">
            <div className="row">
              {user.roles.map((r) => (
                <span key={r} className="badge">
                  {humanize(r)}
                </span>
              ))}
            </div>
            <div className="row" style={{ flexWrap: 'wrap' }}>
              {[...user.permissions]
                .sort((a, b) => a.localeCompare(b))
                .map((p) => (
                  <span key={p} className="badge neutral">
                    {humanize(p)}
                  </span>
                ))}
            </div>
          </div>
        </Card>
      </div>
      <ChangePassword />
    </div>
  );
}

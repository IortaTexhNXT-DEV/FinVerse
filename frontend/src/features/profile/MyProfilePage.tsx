import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { authApi } from '@/api/auth';
import type { PasswordStatus } from '@/api/auth';
import { systemApi } from '@/api/system';
import { useAuth } from '@/auth/authContext';
import { Card } from '@/components/ui/Card';
import { PageFooter } from '@/components/ui/Pager';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useWorkspace } from '@/context/workspaceContext';
import { formatAmount, formatDateTime, humanize } from '@/utils/format';
import { ContactDetailsCard } from './ContactDetailsCard';
import { PasswordChangeForm } from './PasswordChangeForm';
import { policyHint } from './passwordRules';
import { SessionsTable } from './SessionsTable';

const SESSIONS_PAGE = 10;

function ChangePassword({ status }: Readonly<{ status: PasswordStatus | undefined }>) {
  const toast = useToast();
  const queries = useQueryClient();
  const change = useMutation({
    mutationFn: ({ current, next }: { current: string; next: string }) =>
      authApi.changePassword(current, next),
    onSuccess: () => {
      toast.success('Password changed');
      void queries.invalidateQueries({ queryKey: ['password-status'] });
    },
  });
  if (status?.authMode === 'DIRECTORY') {
    return (
      <Card title="Password">
        <div className="alert">
          You sign in with your BDO network password. Change it through the BDO directory, not in
          BrokerVerse.
        </div>
      </Card>
    );
  }
  const dates = [
    status?.passwordChangedAt === undefined
      ? undefined
      : `Last changed ${formatDateTime(status.passwordChangedAt)}.`,
    status?.passwordExpiresAt === undefined
      ? undefined
      : `Expires ${formatDateTime(status.passwordExpiresAt)}.`,
  ].filter((t): t is string => t !== undefined);
  return (
    <Card title="Change password">
      <div className="stack">
        {dates.length > 0 && <p className="muted">{dates.join(' ')}</p>}
        <PasswordChangeForm
          requireCurrent
          grid
          submitLabel="Change Password"
          busy={change.isPending}
          error={change.error}
          hint={status === undefined ? undefined : policyHint(status) || undefined}
          onSubmit={(current, next) => change.mutateAsync({ current, next })}
        />
      </div>
    </Card>
  );
}

function MySessions() {
  const [page, setPage] = useState(0);
  const sessions = useQuery({
    queryKey: ['my-sessions', page],
    queryFn: () => authApi.mySessions(page, SESSIONS_PAGE),
  });
  return (
    <Card title="Recent sessions">
      <div className="stack">
        <p className="muted">
          Your sign-ins and how each session ended. If you see a session you do not recognise,
          change your password and tell your System Administrator.
        </p>
        <SessionsTable rows={sessions.data?.content ?? []} loading={sessions.isLoading} />
        <PageFooter data={sessions.data} noun="sessions" onPage={setPage} />
      </div>
    </Card>
  );
}

/**
 * The signed-in user's profile (FR-UA-004, FR-UA-005): details, roles and permissions, contact
 * details (UQ17), the password with its rules (UAM-NFR-36) and the recent sessions (UAM-NFR-35).
 */
export default function MyProfilePage() {
  const { user } = useAuth();
  const { branches } = useWorkspace();
  const [saved, setSaved] = useState<typeof user>(null);
  const policy = useQuery({ queryKey: ['session-policy'], queryFn: systemApi.sessionPolicy });
  const status = useQuery({ queryKey: ['password-status'], queryFn: authApi.passwordStatus });
  if (user === null) {
    return null;
  }
  const profile = saved ?? user;
  const home = branches.find((b) => b.id === profile.homeBranchId);
  const details: [string, string][] = [
    ['User name', profile.username],
    ['Full name', profile.fullName],
    ['Windows ID', profile.windowsId ?? '—'],
    ['Home branch', home === undefined ? '—' : `${home.code} – ${home.name}`],
    [
      'Authorization limit',
      profile.authorizationLimit === undefined
        ? 'Unlimited'
        : formatAmount(profile.authorizationLimit),
    ],
    ['Last sign-in', formatDateTime(profile.lastLoginAt) || '—'],
    ['Last sign-out', formatDateTime(profile.lastLogoutAt) || '—'],
    ['Automatic sign-out', `After ${policy.data?.timeoutMinutes ?? 30} minutes of inactivity`],
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Account"
        title="My Profile"
        description="Your account, contact details, access rights, password and sessions."
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
        <ContactDetailsCard user={profile} onSaved={setSaved} />
      </div>
      <Card title="Roles and permissions">
        <div className="stack">
          <div className="row">
            {profile.roles.map((r) => (
              <span key={r} className="badge">
                {humanize(r)}
              </span>
            ))}
          </div>
          <div className="row" style={{ flexWrap: 'wrap' }}>
            {[...profile.permissions]
              .sort((a, b) => a.localeCompare(b))
              .map((p) => (
                <span key={p} className="badge neutral">
                  {humanize(p)}
                </span>
              ))}
          </div>
        </div>
      </Card>
      <ChangePassword status={status.data} />
      <MySessions />
    </div>
  );
}

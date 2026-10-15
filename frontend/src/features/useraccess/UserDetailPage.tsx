import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { LogOut, Unlock } from 'lucide-react';
import { useState } from 'react';
import type { ComponentType } from 'react';
import { useParams } from 'react-router-dom';
import { adminApi } from '@/api/admin';
import { authApi } from '@/api/auth';
import { identityApi, DIRECTORY_STATUS_LABELS } from '@/api/identity';
import { userAccessApi } from '@/api/userAccess';
import type { AccessChange, HeldGroupProfile } from '@/api/userAccess';
import type { UserProfile } from '@/api/types';
import { useLovLabel } from '@/components/broking/useLabels';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { LoadingPanel } from '@/components/ui/LoadingPanel';
import { PageHeader } from '@/components/ui/PageHeader';
import { PermissionName } from '@/components/ui/PermissionName';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useTabParam } from '@/components/ui/useTabParam';
import { SessionsTable } from '@/features/profile/SessionsTable';
import { formatDateTime, humanize } from '@/utils/format';
import { DataAccessPanel } from './DataAccessPanel';
import { USER_STATUS_LABELS, statusOf } from './userStatus';
import { useAuth } from '@/auth/authContext';

const TABS = [
  { id: 'profile', label: 'Profile' },
  { id: 'profiles', label: 'Group Profiles' },
  { id: 'permissions', label: 'Effective Permissions' },
  { id: 'data', label: 'Data Access' },
  { id: 'sessions', label: 'Sessions' },
  { id: 'history', label: 'Access History' },
] as const;
type Tab = (typeof TABS)[number]['id'];

function ProfileTab({ user }: Readonly<{ user: UserProfile }>) {
  const businessUnit = useLovLabel('UAM_BUSINESS_UNIT');
  const userLevel = useLovLabel('UAM_USER_LEVEL');
  const directory = useQuery({
    queryKey: ['identity', 'profile', user.username],
    queryFn: () => identityApi.profile(user.username),
  });
  const d = directory.data;
  return (
    <div className="stack">
      <Card title="Account">
        <DefinitionGrid
          columns={2}
          items={[
            { label: 'User ID', value: user.username },
            { label: 'Windows ID', value: user.windowsId },
            { label: 'Name', value: user.fullName },
            { label: 'E-mail', value: user.email },
            { label: 'Business Unit', value: businessUnit(user.businessUnitCode) },
            { label: 'User Level', value: userLevel(user.userLevel) },
            { label: 'Status', value: USER_STATUS_LABELS[statusOf(user)] },
            { label: 'Last Login', value: formatDateTime(user.lastLoginAt) },
          ]}
        />
      </Card>
      <Card title="From UIDM-ISC (SCIM)">
        <ErrorAlert error={directory.error} />
        {d === undefined ? (
          <EmptyState message="No details received from UIDM-ISC for this user yet" />
        ) : (
          <DefinitionGrid
            columns={2}
            collapseEmpty
            items={[
              { label: 'Display name', value: d.displayName },
              {
                label: 'Directory status',
                value: d.adStatus && DIRECTORY_STATUS_LABELS[d.adStatus],
              },
              { label: 'Department', value: d.department },
              { label: 'Unit / segment', value: d.unitSegment },
              { label: 'Location', value: d.location },
              { label: "Team leader's name", value: d.teamLeaderName },
              { label: 'UIDM request no.', value: d.uidmRequestNo },
              { label: 'Last synchronised', value: formatDateTime(d.lastEventAt ?? d.adSyncAt) },
            ]}
          />
        )}
      </Card>
    </div>
  );
}

function GroupProfilesTab({ user }: Readonly<{ user: UserProfile }>) {
  const held = useQuery({
    queryKey: ['user-access', 'held', user.id],
    queryFn: () => userAccessApi.groupProfilesOf(user.id),
  });
  return (
    <Card flush>
      <ErrorAlert error={held.error} />
      <DataTable<HeldGroupProfile>
        caption="Group profiles of the user"
        loading={held.isLoading}
        rows={held.data ?? []}
        rowKey={(p) => p.code}
        emptyMessage="The user holds no group profile"
        columns={[
          { key: 'c', header: 'Code', kind: 'code', render: (p) => p.code },
          { key: 'n', header: 'Group Profile', render: (p) => p.name },
          {
            key: 'a',
            header: 'Status',
            kind: 'status',
            render: (p) => <StatusBadge status={p.active ? 'ACTIVE' : 'INACTIVE'} />,
          },
          { key: 's', header: 'Source', render: (p) => p.source ?? 'Not recorded' },
          { key: 'd', header: 'Since', kind: 'datetime', render: (p) => formatDateTime(p.since) },
          { key: 'r', header: 'Request No.', render: (p) => p.requestNo ?? '' },
        ]}
      />
    </Card>
  );
}

function PermissionsTab({ user }: Readonly<{ user: UserProfile }>) {
  const codes = [...user.permissions].sort((a, b) => a.localeCompare(b));
  if (codes.length === 0) {
    return <EmptyState message="The user's group profiles grant no permission" />;
  }
  return (
    <Card title={`${String(codes.length)} permissions through the active group profiles`}>
      <ul className="tag-list" aria-label="Effective permissions">
        {codes.map((c) => (
          <li key={c}>
            <PermissionName code={c} />
          </li>
        ))}
      </ul>
    </Card>
  );
}

function SessionsTab({ user }: Readonly<{ user: UserProfile }>) {
  const sessions = useQuery({
    queryKey: ['admin-sessions', user.username, 0],
    queryFn: () => authApi.sessions({ username: user.username, page: 0, size: 20 }),
  });
  return (
    <Card flush>
      <ErrorAlert error={sessions.error} />
      <SessionsTable rows={sessions.data?.content ?? []} loading={sessions.isLoading} />
    </Card>
  );
}

function HistoryTab({ user }: Readonly<{ user: UserProfile }>) {
  const history = useQuery({
    queryKey: ['user-access', 'history', user.id],
    queryFn: () => userAccessApi.accessHistory(user.id),
  });
  return (
    <Card flush>
      <ErrorAlert error={history.error} />
      <DataTable<AccessChange>
        caption="Access history"
        loading={history.isLoading}
        rows={history.data ?? []}
        rowKey={(c) => c.id}
        emptyMessage="No change of this user's access is recorded"
        columns={[
          {
            key: 'o',
            header: 'Date / Time',
            kind: 'datetime',
            render: (c) => formatDateTime(c.occurredAt),
          },
          { key: 'a', header: 'Activity', render: (c) => humanize(c.activity) },
          { key: 't', header: 'Attribute', render: (c) => humanize(c.attribute) },
          { key: 'f', header: 'From', render: (c) => c.fromValue ?? '' },
          { key: 'v', header: 'To', render: (c) => c.toValue ?? '' },
          { key: 'r', header: 'Request No.', render: (c) => c.requestNo ?? '' },
          { key: 'd', header: 'Done By', render: (c) => c.doneBy },
          { key: 'p', header: 'Approved By', render: (c) => c.approvedBy ?? '' },
        ]}
      />
    </Card>
  );
}

type Pending = 'unlock' | 'end' | null;

const TAB_BODIES: Record<Tab, ComponentType<{ user: UserProfile }>> = {
  profile: ProfileTab,
  profiles: GroupProfilesTab,
  permissions: PermissionsTab,
  data: DataAccessPanel,
  sessions: SessionsTab,
  history: HistoryTab,
};

/**
 * One user of User Access: the profile received from UIDM-ISC, the group profiles held with their
 * source and date, the effective permissions, the data access (kept here), the sessions and the
 * access history; Unlock and End Sessions after a confirmation.
 */
export default function UserDetailPage() {
  const { id } = useParams();
  const toast = useToast();
  const queries = useQueryClient();
  const [tab, setTab] = useTabParam<Tab>(
    TABS.map((t) => t.id),
    'profile',
  );
  const [pending, setPending] = useState<Pending>(null);
  const { can } = useAuth();
  const manage = can('USER_MANAGE');
  const users = useQuery({ queryKey: ['users'], queryFn: adminApi.users });
  const user = users.data?.find((u) => String(u.id) === id);
  const done = async (text: string) => {
    toast.success(text);
    setPending(null);
    await queries.invalidateQueries({ queryKey: ['users'] });
    await queries.invalidateQueries({ queryKey: ['admin-sessions'] });
  };
  const unlock = useMutation({
    mutationFn: (u: UserProfile) => adminApi.unlockUser(u.id),
    onSuccess: (u) => done(`${u.username} unlocked`),
  });
  const end = useMutation({
    mutationFn: (u: UserProfile) => userAccessApi.endSessions(u.username),
    onSuccess: (r) => done(`${String(r.ended)} session(s) ended`),
  });

  if (users.isLoading) {
    return <LoadingPanel />;
  }
  if (user === undefined) {
    return (
      <div className="stack">
        <PageHeader section="User Access" title="User" backTo="/access/users" />
        <ErrorAlert error={users.error} />
        <EmptyState message="This user does not exist or is not visible to you" />
      </div>
    );
  }
  const Body = TAB_BODIES[tab];
  return (
    <div className="stack">
      <PageHeader
        section="User Access"
        title={`${user.fullName} (${user.username})`}
        description="Users and their group profiles are maintained in UIDM-ISC."
        backTo="/access/users"
        actions={
          manage && (
            <>
              <Button
                variant="secondary"
                icon={<LogOut size={16} />}
                onClick={() => setPending('end')}
              >
                End Sessions
              </Button>
              {user.locked && (
                <Button
                  variant="accent"
                  icon={<Unlock size={16} />}
                  onClick={() => setPending('unlock')}
                >
                  Unlock
                </Button>
              )}
            </>
          )
        }
      />
      <div className="row" style={{ gap: 'var(--space-2)' }}>
        <StatusBadge status={statusOf(user)} label={USER_STATUS_LABELS[statusOf(user)]} />
      </div>
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <Body user={user} />
      {pending === 'unlock' && (
        <ConfirmDialog
          title={`Unlock ${user.username}`}
          record={user.fullName}
          effect="The user can sign in again; the count of failed sign-ins starts again."
          confirmLabel="Unlock"
          busy={unlock.isPending}
          error={unlock.error}
          onConfirm={() => unlock.mutate(user)}
          onClose={() => setPending(null)}
        />
      )}
      {pending === 'end' && (
        <ConfirmDialog
          title={`End the sessions of ${user.username}`}
          record={user.fullName}
          effect="Every open session of the user ends at once on every server; the user signs in again."
          confirmLabel="End Sessions"
          destructive
          busy={end.isPending}
          error={end.error}
          onConfirm={() => end.mutate(user)}
          onClose={() => setPending(null)}
        />
      )}
    </div>
  );
}

import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessRequest, RoleInfo } from '@/api/nbadmin';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { LoadingPanel } from '@/components/ui/LoadingPanel';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useTabParam } from '@/components/ui/useTabParam';
import { humanize } from '@/utils/format';
import { fromRequest, isOpen, newChange } from './groupProfileChange';
import type { ChangeForm, ChangeType } from './groupProfileChange';
import { HistoryTab, MembersTab, PermissionsTab } from './GroupProfileTabs';
import { PendingTab } from './PendingChangeTab';
import { ProposeChangeDialog } from './ProposeChangeDialog';

const TABS = [
  { id: 'definition', label: 'Definition' },
  { id: 'permissions', label: 'Permissions' },
  { id: 'members', label: 'Members' },
  { id: 'pending', label: 'Pending Change' },
  { id: 'history', label: 'History' },
] as const;
type Tab = (typeof TABS)[number]['id'];

/** The proposals the maker may start on an existing profile without an open change. */
function ProposeActions({
  role,
  onPropose,
}: Readonly<{ role: RoleInfo; onPropose: (type: ChangeType) => void }>) {
  return (
    <>
      <Button
        variant="secondary"
        onClick={() => onPropose(role.active ? 'DEACTIVATE_ROLE' : 'REACTIVATE_ROLE')}
      >
        {role.active ? 'Propose Deactivation' : 'Propose Reactivation'}
      </Button>
      {role.active && (
        <Button variant="accent" onClick={() => onPropose('MODIFY_ROLE_PERMISSIONS')}>
          Propose Change
        </Button>
      )}
    </>
  );
}

function DefinitionCard({ role, members }: Readonly<{ role: RoleInfo; members: number }>) {
  return (
    <Card>
      <DefinitionGrid
        columns={2}
        items={[
          { label: 'Code', value: role.code },
          { label: 'Name', value: role.name },
          { label: 'Description', value: role.description, wide: true },
          { label: 'Privilege Level', value: humanize(role.privilegeLevel) },
          { label: 'Status', value: <StatusBadge status={role.active ? 'ACTIVE' : 'INACTIVE'} /> },
          { label: 'Members', value: members },
          { label: 'Permissions', value: role.permissions.length },
        ]}
      />
    </Card>
  );
}

/** The tab of an existing profile; a proposed new profile has only its pending change. */
function ProfileTab({
  tab,
  role,
  members,
}: Readonly<{ tab: Tab; role?: RoleInfo; members: number }>) {
  if (role === undefined) {
    return <EmptyState message="The group profile exists once its creation is approved" />;
  }
  if (tab === 'permissions') {
    return <PermissionsTab role={role} />;
  }
  return tab === 'members' ? (
    <MembersTab role={role} />
  ) : (
    <DefinitionCard role={role} members={members} />
  );
}

function ProfileHeader({
  code,
  role,
  mayPropose,
  onPropose,
}: Readonly<{
  code: string;
  role?: RoleInfo;
  mayPropose: boolean;
  onPropose: (type: ChangeType) => void;
}>) {
  return (
    <PageHeader
      section="User Access"
      title={role === undefined ? `${code} (proposed)` : `${role.name} (${role.code})`}
      description={role?.description}
      backTo="/access/group-profiles"
      actions={
        mayPropose && role !== undefined ? (
          <ProposeActions role={role} onPropose={onPropose} />
        ) : undefined
      }
    />
  );
}

function WaitingNotice({
  change,
  onOpen,
}: Readonly<{ change: AccessRequest; onOpen: () => void }>) {
  return (
    <p className="muted" role="status">
      A change of this group profile is waiting ({humanize(change.status)}).{' '}
      <Button variant="ghost" size="sm" onClick={onOpen}>
        Open the pending change
      </Button>
    </p>
  );
}

const placeholdersOf = (role?: RoleInfo) =>
  role === undefined ? undefined : { name: role.name, description: role.description ?? '' };

function TabBody({
  tab,
  code,
  role,
  members,
  open,
  onEdit,
}: Readonly<{
  tab: Tab;
  code: string;
  role?: RoleInfo;
  members: number;
  open?: AccessRequest;
  onEdit: (r: AccessRequest) => void;
}>) {
  if (tab === 'pending') {
    return <PendingTab change={open} onEdit={onEdit} />;
  }
  if (tab === 'history') {
    return <HistoryTab code={code} />;
  }
  return <ProfileTab tab={tab} role={role} members={members} />;
}

/**
 * One group profile: its definition, permissions (by area and action), members (from UIDM-ISC,
 * read-only), the change waiting for approval with the checker's decision, and the history of its
 * changes. The maker proposes a change, deactivation or reactivation; another user approves it.
 */
/** The profile, its open change and its active members. */
function useGroupProfile(code: string) {
  const roles = useQuery({ queryKey: ['nbadmin', 'roles'], queryFn: nbadminApi.roles });
  const users = useQuery({ queryKey: ['nbadmin', 'users'], queryFn: nbadminApi.users });
  const changes = useQuery({
    queryKey: ['nbadmin', 'requests', 'history', code],
    queryFn: () => nbadminApi.requests({ scope: 'ALL', groupProfiles: true, text: code }),
  });
  return {
    role: roles.data?.find((r) => r.code === code),
    open: (changes.data?.content ?? []).find((r) => r.roleCode === code && isOpen(r.status)),
    members: (users.data ?? []).filter((u) => u.enabled && u.roleCodes.includes(code)).length,
    loading: roles.isLoading || changes.isLoading,
    error: roles.error ?? changes.error,
  };
}

export default function GroupProfileDetailPage() {
  const { code = '' } = useParams();
  const navigate = useNavigate();
  const { can } = useAuth();
  const [tab, setTab] = useTabParam<Tab>(
    TABS.map((t) => t.id),
    'definition',
  );
  const [proposal, setProposal] = useState<{ form: ChangeForm; saved?: AccessRequest } | null>(
    null,
  );
  const { role, open, members, loading, error } = useGroupProfile(code);
  const maker = can('UAM_GROUP_REQUEST') || can('ACCESS_REQUEST');

  if (loading) {
    return <LoadingPanel />;
  }
  if (role === undefined && open === undefined) {
    return (
      <div className="stack">
        <PageHeader section="User Access" title="Group Profile" backTo="/access/group-profiles" />
        <ErrorAlert error={error} />
        <EmptyState message={`There is no group profile ${code}`} />
      </div>
    );
  }
  const afterProposal = () => {
    setProposal(null);
    void navigate(`/access/group-profiles/${encodeURIComponent(code)}?tab=pending`, {
      replace: true,
    });
  };
  return (
    <div className="stack">
      <ProfileHeader
        code={code}
        role={role}
        mayPropose={maker && open === undefined}
        onPropose={(type) => setProposal({ form: newChange(type, role) })}
      />
      {open !== undefined && tab !== 'pending' && (
        <WaitingNotice change={open} onOpen={() => setTab('pending')} />
      )}
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabBody
        tab={tab}
        code={code}
        role={role}
        members={members}
        open={open}
        onEdit={(r) => setProposal({ form: fromRequest(r, role), saved: r })}
      />
      {proposal !== null && (
        <ProposeChangeDialog
          initial={proposal.form}
          saved={proposal.saved}
          members={members}
          placeholders={placeholdersOf(role)}
          onClose={() => setProposal(null)}
          onDone={afterProposal}
        />
      )}
    </div>
  );
}

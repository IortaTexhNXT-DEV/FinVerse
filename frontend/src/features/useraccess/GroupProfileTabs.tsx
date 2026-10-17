import { useQuery } from '@tanstack/react-query';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessRequest, RoleInfo } from '@/api/nbadmin';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { countOf, formatDateTime } from '@/utils/format';
import { CHANGE_LABELS } from './groupProfileChange';
import { PermissionTable } from './PermissionTable';

/** The permissions of a group profile with their area and the action classes they give. */
export function PermissionsTab({ role }: Readonly<{ role: RoleInfo }>) {
  const matrix = useQuery({ queryKey: ['nbadmin', 'matrix'], queryFn: nbadminApi.matrix });
  const classified = new Map((matrix.data?.permissions ?? []).map((p) => [p.permission, p]));
  const rows = role.permissions.map((code) => ({
    permission: code,
    area: classified.get(code)?.area,
    actions: classified.get(code)?.actions ?? [],
  }));
  return (
    <PermissionTable
      title={countOf(rows.length, 'permission')}
      rows={rows}
      loading={matrix.isLoading}
      error={matrix.error}
      fileName={`group_profile_permissions_${role.code}.csv`}
      caption={`Permissions of ${role.name}`}
      emptyMessage="The group profile grants no permission"
      actionClasses
    />
  );
}

/** The members of a group profile (assigned by UIDM-ISC), read-only. */
export function MembersTab({ role }: Readonly<{ role: RoleInfo }>) {
  const users = useQuery({ queryKey: ['nbadmin', 'users'], queryFn: nbadminApi.users });
  const rows = (users.data ?? []).filter((u) => u.roleCodes.includes(role.code));
  return (
    <Card flush>
      <ErrorAlert error={users.error} />
      <p className="muted" style={{ padding: 'var(--space-3) var(--space-4)', margin: 0 }}>
        UIDM-ISC assigns the members of a group profile; they are shown here read-only.
      </p>
      <DataTable
        caption={`Members of ${role.name}`}
        loading={users.isLoading}
        rows={rows}
        rowKey={(u) => u.username}
        emptyMessage="The group profile has no members"
        columns={[
          { key: 'u', header: 'User ID', kind: 'code', render: (u) => u.username },
          { key: 'w', header: 'Windows ID', render: (u) => u.windowsId ?? '' },
          { key: 'n', header: 'Name', render: (u) => u.fullName },
          {
            key: 's',
            header: 'Status',
            kind: 'status',
            render: (u) => <StatusBadge status={u.enabled ? 'ACTIVE' : 'INACTIVE'} />,
          },
        ]}
      />
    </Card>
  );
}

/** The changes of a group profile, newest first. */
export function HistoryTab({ code }: Readonly<{ code: string }>) {
  const history = useQuery({
    queryKey: ['nbadmin', 'requests', 'history', code],
    queryFn: () => nbadminApi.requests({ scope: 'ALL', groupProfiles: true, text: code }),
  });
  const rows = (history.data?.content ?? []).filter((r) => r.roleCode === code);
  return (
    <Card flush>
      <ErrorAlert error={history.error} />
      <DataTable<AccessRequest>
        caption="Changes of the group profile"
        loading={history.isLoading}
        rows={rows}
        rowKey={(r) => r.id}
        emptyMessage="No change of this group profile is recorded"
        columns={[
          { key: 'n', header: 'Change No.', kind: 'code', render: (r) => r.requestNo },
          { key: 't', header: 'Change', render: (r) => CHANGE_LABELS[r.type] },
          { key: 'd', header: 'Details', render: (r) => r.summary },
          { key: 'm', header: 'Maker', render: (r) => <UserName login={r.requestedBy} /> },
          {
            key: 'c',
            header: 'Checker',
            render: (r) => (r.decidedBy ? <UserName login={r.decidedBy} /> : ''),
          },
          {
            key: 'a',
            header: 'Date',
            kind: 'datetime',
            render: (r) => formatDateTime(r.decidedAt ?? r.requestedAt),
          },
          {
            key: 's',
            header: 'Status',
            kind: 'status',
            render: (r) => <StatusBadge status={r.status} />,
          },
        ]}
      />
    </Card>
  );
}

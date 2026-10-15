import { useQuery } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminApi } from '@/api/admin';
import type { UserProfile } from '@/api/types';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { useLovLabel } from '@/components/broking/useLabels';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';
import { saveCsv } from './csv';
import {
  NO_USER_FILTER,
  USER_STATUSES,
  USER_STATUS_LABELS,
  matchesUser,
  statusOf,
} from './userStatus';
import type { UserFilter, UserStatus } from './userStatus';

/**
 * Users (read-only): the accounts UIDM-ISC provisions, with their Windows ID, business unit, status,
 * group profiles, last sign-in and lock, filtered by status, group profile and business unit and
 * exported as CSV. A row opens the user with the data access, unlock and session actions.
 */
export default function UsersPage() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState<UserFilter>(NO_USER_FILTER);
  const users = useQuery({ queryKey: ['users'], queryFn: adminApi.users });
  const roles = useQuery({ queryKey: ['roles'], queryFn: adminApi.roles });
  const businessUnit = useLovLabel('UAM_BUSINESS_UNIT');
  const profileName = (code: string) => roles.data?.find((r) => r.code === code)?.name ?? code;
  const set = (p: Partial<UserFilter>) => setFilter((f) => ({ ...f, ...p }));
  const rows = (users.data ?? []).filter((u) => matchesUser(u, filter));
  const units = [...new Set((users.data ?? []).flatMap((u) => u.businessUnitCode ?? []))].sort(
    (a, b) => a.localeCompare(b),
  );

  const exportRows = () =>
    saveCsv(
      'users.csv',
      [
        'User ID',
        'Windows ID',
        'Name',
        'Business Unit',
        'Status',
        'Group Profiles',
        'Last Login',
        'Locked',
      ],
      rows.map((u) => [
        u.username,
        u.windowsId ?? '',
        u.fullName,
        businessUnit(u.businessUnitCode),
        USER_STATUS_LABELS[statusOf(u)],
        u.roles.map(profileName).join('; '),
        u.lastLoginAt ?? '',
        u.locked ? 'Yes' : 'No',
      ]),
    );

  return (
    <div className="stack">
      <PageHeader
        section="User Access"
        title="Users"
        description="Users and their group profiles come from UIDM-ISC and cannot be changed here."
        actions={
          <Button
            variant="secondary"
            icon={<Download size={16} />}
            disabled={rows.length === 0}
            onClick={exportRows}
          >
            Export
          </Button>
        }
      />
      <ErrorAlert error={users.error} />
      <Card flush>
        <WorklistToolbar
          placeholder="Search User ID, Name or Windows ID"
          onSearch={(text) => set({ text })}
          extra={
            <>
              <select
                className="select"
                aria-label="Status"
                value={filter.status}
                onChange={(e) => set({ status: e.target.value as UserStatus | '' })}
              >
                <option value="">All statuses</option>
                {USER_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {USER_STATUS_LABELS[s]}
                  </option>
                ))}
              </select>
              <select
                className="select"
                aria-label="Group profile"
                value={filter.profile}
                onChange={(e) => set({ profile: e.target.value })}
              >
                <option value="">All group profiles</option>
                {(roles.data ?? []).map((r) => (
                  <option key={r.code} value={r.code}>
                    {r.name}
                  </option>
                ))}
              </select>
              <select
                className="select"
                aria-label="Business unit"
                value={filter.businessUnit}
                onChange={(e) => set({ businessUnit: e.target.value })}
              >
                <option value="">All business units</option>
                {units.map((code) => (
                  <option key={code} value={code}>
                    {businessUnit(code)}
                  </option>
                ))}
              </select>
            </>
          }
        />
        <DataTable<UserProfile>
          caption="Users"
          loading={users.isLoading}
          rows={rows}
          rowKey={(u) => u.id}
          emptyMessage="No user matches the search and filters"
          onRowClick={(u) => void navigate(`/access/users/${String(u.id)}`)}
          columns={[
            {
              key: 'u',
              header: 'User ID',
              kind: 'code',
              render: (u) => <strong>{u.username}</strong>,
            },
            { key: 'w', header: 'Windows ID', render: (u) => u.windowsId ?? '' },
            { key: 'n', header: 'Name', render: (u) => u.fullName },
            { key: 'bu', header: 'Business Unit', render: (u) => businessUnit(u.businessUnitCode) },
            {
              key: 's',
              header: 'Status',
              kind: 'status',
              render: (u) => (
                <StatusBadge status={statusOf(u)} label={USER_STATUS_LABELS[statusOf(u)]} />
              ),
            },
            {
              key: 'r',
              header: 'Group Profiles',
              render: (u) => u.roles.map(profileName).join(', '),
            },
            {
              key: 'll',
              header: 'Last Login',
              kind: 'datetime',
              render: (u) => formatDateTime(u.lastLoginAt),
            },
            {
              key: 'l',
              header: 'Locked',
              kind: 'center',
              render: (u) => (u.locked ? 'Yes' : 'No'),
            },
          ]}
        />
      </Card>
    </div>
  );
}

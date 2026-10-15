import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { nbadminApi } from '@/api/nbadmin';
import type { RoleInfo } from '@/api/nbadmin';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { humanize } from '@/utils/format';
import { CHANGE_LABELS, newChange } from './groupProfileChange';
import { ProposeChangeDialog } from './ProposeChangeDialog';
import { useOpenChanges } from './useGroupProfileChanges';

const MAKERS = ['UAM_GROUP_REQUEST', 'ACCESS_REQUEST'];

/**
 * Group Profiles: every group profile with its privilege level, members, status and the change
 * waiting for approval. A row opens the profile (definition, permissions, members, pending change
 * and history); New Group Profile proposes one for approval by a checker.
 */
export default function GroupProfilesPage() {
  const navigate = useNavigate();
  const { can } = useAuth();
  const [text, setText] = useState('');
  const [proposing, setProposing] = useState(false);
  const roles = useQuery({ queryKey: ['nbadmin', 'roles'], queryFn: nbadminApi.roles });
  const users = useQuery({ queryKey: ['nbadmin', 'users'], queryFn: nbadminApi.users });
  const open = useOpenChanges();
  const members = (code: string) =>
    (users.data ?? []).filter((u) => u.enabled && u.roleCodes.includes(code)).length;
  const needle = text.trim().toLocaleLowerCase();
  const rows = (roles.data ?? []).filter(
    (r) =>
      needle === '' ||
      r.code.toLocaleLowerCase().includes(needle) ||
      r.name.toLocaleLowerCase().includes(needle),
  );
  const maker = MAKERS.some((p) => can(p));
  return (
    <div className="stack">
      <PageHeader
        section="User Access"
        title="Group Profiles"
        description="Definitions and permissions of the group profiles; every change is approved by a second user."
        actions={
          maker && (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => setProposing(true)}>
              New Group Profile
            </Button>
          )
        }
      />
      <ErrorAlert error={roles.error ?? open.error} />
      <Card flush>
        <WorklistToolbar placeholder="Search code or name" onSearch={setText} />
        <DataTable<RoleInfo>
          caption="Group profiles"
          loading={roles.isLoading}
          rows={rows}
          rowKey={(r) => r.code}
          emptyMessage="No group profile matches the search"
          onRowClick={(r) => void navigate(`/access/group-profiles/${encodeURIComponent(r.code)}`)}
          columns={[
            { key: 'c', header: 'Code', kind: 'code', render: (r) => <strong>{r.code}</strong> },
            { key: 'n', header: 'Name', render: (r) => r.name },
            { key: 'p', header: 'Privilege Level', render: (r) => humanize(r.privilegeLevel) },
            { key: 'm', header: 'Members', numeric: true, render: (r) => members(r.code) },
            {
              key: 's',
              header: 'Status',
              kind: 'status',
              render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} />,
            },
            {
              key: 'x',
              header: 'Pending Change',
              render: (r) => {
                const c = open.byProfile.get(r.code);
                return c === undefined ? (
                  ''
                ) : (
                  <span>
                    {CHANGE_LABELS[c.type]} · <StatusBadge status={c.status} />
                  </span>
                );
              },
            },
          ]}
        />
      </Card>
      {open.changes.some((c) => c.type === 'CREATE_ROLE') && (
        <Card title="Proposed new group profiles">
          <ul className="stack" aria-label="Proposed new group profiles">
            {open.changes
              .filter((c) => c.type === 'CREATE_ROLE' && c.roleCode !== undefined)
              .map((c) => (
                <li key={c.id}>
                  <Button
                    variant="ghost"
                    onClick={() =>
                      void navigate(
                        `/access/group-profiles/${encodeURIComponent(c.roleCode ?? '')}?tab=pending`,
                      )
                    }
                  >
                    {c.roleCode} · {c.details.roleName ?? ''}
                  </Button>{' '}
                  <StatusBadge status={c.status} />
                </li>
              ))}
          </ul>
        </Card>
      )}
      {proposing && (
        <ProposeChangeDialog
          initial={newChange('CREATE_ROLE')}
          onClose={() => setProposing(false)}
          onDone={() => setProposing(false)}
        />
      )}
    </div>
  );
}

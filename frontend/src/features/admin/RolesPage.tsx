import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Wrench } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminApi } from '@/api/admin';
import type { Role } from '@/api/admin';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessRequest } from '@/api/nbadmin';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';

function roleHeader(r: Role): string {
  const level = r.privilegeLevel ? ` · ${humanize(r.privilegeLevel)}` : '';
  return `${r.name}${level}${r.active === false ? ' (inactive)' : ''}`;
}

/**
 * Approved group-profile requests waiting for the System Administrator (BRD-11 p.6; FR-UA-045):
 * "Implement Request" applies the approved change and the request becomes IMPLEMENTED.
 */
function ToImplement() {
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState<AccessRequest>();
  const pending = useQuery({
    queryKey: ['nbadmin', 'requests', 'implementation'],
    queryFn: () => nbadminApi.requests({ scope: 'IMPLEMENTATION' }),
  });
  const implement = useMutation({
    mutationFn: (id: number) => nbadminApi.implement(id),
    onSuccess: async (r) => {
      setChosen(undefined);
      toast.success(`${r.requestNo} implemented`);
      await queryClient.invalidateQueries({ queryKey: ['nbadmin'] });
      await queryClient.invalidateQueries({ queryKey: ['roles'] });
    },
  });
  return (
    <Card title="Approved Requests to Implement" flush>
      <ErrorAlert error={pending.error} />
      <DataTable<AccessRequest>
        caption="Approved group-profile requests to implement"
        loading={pending.isLoading}
        rows={pending.data?.content ?? []}
        rowKey={(r) => r.id}
        onRowClick={(r) => void navigate(`/user-access/requests/${String(r.id)}`)}
        emptyMessage="No approved group-profile request waits for implementation."
        columns={[
          {
            key: 'n',
            header: 'Request',
            render: (r) => <strong className="mono">{r.requestNo}</strong>,
          },
          { key: 'c', header: 'Change', render: (r) => r.summary },
          { key: 'b', header: 'Requested By', render: (r) => r.requestedBy },
          {
            key: 'd',
            header: 'Approved',
            render: (r) => `${r.decidedBy ?? ''} · ${formatDateTime(r.decidedAt)}`,
          },
          {
            key: 'a',
            header: 'Actions',
            render: (r) => (
              <Button
                size="sm"
                icon={<Wrench size={14} />}
                onClick={(e) => {
                  e.stopPropagation();
                  setChosen(r);
                }}
              >
                Implement Request
              </Button>
            ),
          },
        ]}
      />
      {chosen && (
        <Modal
          open
          title={`Implement ${chosen.requestNo}`}
          onClose={() => setChosen(undefined)}
          footer={
            <>
              <Button variant="secondary" onClick={() => setChosen(undefined)}>
                Cancel
              </Button>
              <Button
                variant="accent"
                busy={implement.isPending}
                onClick={() => implement.mutate(chosen.id)}
              >
                Implement Request
              </Button>
            </>
          }
        >
          <div className="stack">
            <ErrorAlert error={implement.error} />
            <p>{chosen.summary}</p>
            <dl className="detail-list">
              <dt>Permissions added</dt>
              <dd className="mono">{chosen.permissionsAdded.join(', ') || '—'}</dd>
              <dt>Permissions removed</dt>
              <dd className="mono">{chosen.permissionsRemoved.join(', ') || '—'}</dd>
              <dt>Approved by</dt>
              <dd>{chosen.decidedBy}</dd>
            </dl>
          </div>
        </Modal>
      )}
    </Card>
  );
}

/**
 * Roles & Permissions (BRD 4.002; FR-UA-045, FR-UA-050): each role is a bundle of permissions, with
 * its privilege level and active flag. Roles change by implementing approved group-profile
 * requests; the direct edit is the audited emergency path UAM_DIRECT_ROLE_EDIT.
 */
export default function RolesPage() {
  const toast = useToast();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const roles = useQuery({ queryKey: ['roles'], queryFn: adminApi.roles });
  const permissions = useQuery({ queryKey: ['permissions'], queryFn: adminApi.permissions });
  const settings = useQuery({ queryKey: ['nbadmin', 'settings'], queryFn: nbadminApi.settings });
  const [draft, setDraft] = useState<Record<number, string[]>>({});
  const direct = settings.data?.directRoleEdit === true;

  const save = useMutation({
    mutationFn: (role: Role) =>
      adminApi.updateRole(role.id, {
        code: role.code,
        name: role.name,
        permissions: draft[role.id] ?? role.permissions,
      }),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['roles'] });
      toast.success(`Role ${r.name} updated (emergency edit, audited)`);
    },
  });

  const toggle = (role: Role, permission: string) => {
    const current = draft[role.id] ?? role.permissions;
    setDraft({
      ...draft,
      [role.id]: current.includes(permission)
        ? current.filter((p) => p !== permission)
        : [...current, permission],
    });
  };

  const list = roles.data ?? [];
  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Roles & Permissions"
        description="Group profiles and the permissions they grant. A profile changes only by implementing an approved group-profile request."
      />
      {direct && (
        <div className="alert warning" role="status">
          Emergency path UAM_DIRECT_ROLE_EDIT is open: every direct edit is audited and raises the
          alert UAM_DIRECT_ROLE_EDIT.
        </div>
      )}
      {can('ROLE_MANAGE') && <ToImplement />}
      <ErrorAlert error={save.error} />
      <Card flush>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Permission</th>
                {list.map((r) => (
                  <th key={r.id} style={{ textAlign: 'center' }}>
                    {roleHeader(r)}
                    {r.active === false && <StatusBadge status="INACTIVE" />}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {(permissions.data ?? []).map((p) => (
                <tr key={p}>
                  <td>{humanize(p)}</td>
                  {list.map((r) => (
                    <td key={r.id} style={{ textAlign: 'center' }}>
                      <input
                        type="checkbox"
                        aria-label={`${humanize(p)} for ${r.name}`}
                        disabled={!direct}
                        checked={(draft[r.id] ?? r.permissions).includes(p)}
                        onChange={() => toggle(r, p)}
                      />
                    </td>
                  ))}
                </tr>
              ))}
              {direct && (
                <tr>
                  <td />
                  {list.map((r) => (
                    <td key={r.id} style={{ textAlign: 'center' }}>
                      <Button
                        size="sm"
                        variant="secondary"
                        disabled={draft[r.id] === undefined}
                        onClick={() => save.mutate(r)}
                      >
                        Save
                      </Button>
                    </td>
                  ))}
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}

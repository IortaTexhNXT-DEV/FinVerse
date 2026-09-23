import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { adminApi } from '@/api/admin';
import type { UserInput } from '@/api/admin';
import type { UserProfile } from '@/api/types';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDateTime } from '@/utils/format';

interface Editing {
  id?: number;
  user: UserInput;
  password: string;
}

function statusOf(u: UserProfile): string {
  if (u.locked) {
    return 'LOCKED';
  }
  return u.enabled ? 'ACTIVE' : 'INACTIVE';
}

const EMPTY: UserInput = { username: '', fullName: '', email: '', roleCodes: [], enabled: true };

/** User administration: roles, authorization limits, lockout and password reset. */
export default function UsersPage() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Editing | null>(null);
  const users = useQuery({ queryKey: ['users'], queryFn: adminApi.users });
  const roles = useQuery({ queryKey: ['roles'], queryFn: adminApi.roles });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['users'] });
  const save = useMutation({
    mutationFn: (e: Editing) =>
      e.id === undefined
        ? adminApi.createUser(e.user, e.password)
        : adminApi.updateUser(e.id, e.user),
    onSuccess: async (u) => {
      await refresh();
      setEditing(null);
      toast.success(`User ${u.username} saved`);
    },
  });
  const unlock = useMutation({
    mutationFn: (id: number) => adminApi.unlockUser(id),
    onSuccess: refresh,
  });

  const edit = (u: UserProfile) =>
    setEditing({
      id: u.id,
      password: '',
      user: {
        username: u.username,
        fullName: u.fullName,
        email: u.email,
        homeBranchId: u.homeBranchId,
        authorizationLimit: u.authorizationLimit,
        roleCodes: u.roles,
        enabled: u.enabled,
      },
    });

  const toggleRole = (code: string) =>
    editing &&
    setEditing({
      ...editing,
      user: {
        ...editing.user,
        roleCodes: editing.user.roleCodes.includes(code)
          ? editing.user.roleCodes.filter((r) => r !== code)
          : [...editing.user.roleCodes, code],
      },
    });

  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Users"
        description="Accounts lock after five failed sign-ins. Authorization limits cap the journal value a user may approve."
        actions={
          <Button
            variant="accent"
            icon={<Plus size={16} />}
            onClick={() => setEditing({ user: EMPTY, password: '' })}
          >
            New user
          </Button>
        }
      />
      <ErrorAlert error={unlock.error} />
      <Card flush>
        <DataTable<UserProfile>
          loading={users.isLoading}
          rows={users.data ?? []}
          rowKey={(u) => u.id}
          onRowClick={edit}
          columns={[
            { key: 'u', header: 'User name', render: (u) => <strong>{u.username}</strong> },
            { key: 'n', header: 'Full name', render: (u) => u.fullName },
            { key: 'r', header: 'Roles', render: (u) => u.roles.join(', ') },
            {
              key: 'l',
              header: 'Authorization limit',
              numeric: true,
              render: (u) =>
                u.authorizationLimit === undefined
                  ? 'Unlimited'
                  : formatAmount(u.authorizationLimit),
            },
            { key: 'll', header: 'Last login', render: (u) => formatDateTime(u.lastLoginAt) },
            {
              key: 's',
              header: 'Status',
              render: (u) => <StatusBadge status={statusOf(u)} />,
            },
            {
              key: 'a',
              header: 'Actions',
              render: (u) =>
                u.locked && (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={(e) => {
                      e.stopPropagation();
                      unlock.mutate(u.id);
                    }}
                  >
                    Unlock
                  </Button>
                ),
            },
          ]}
        />
      </Card>
      <Modal
        title={editing?.id === undefined ? 'New user' : `Edit ${editing.user.username}`}
        open={editing !== null}
        onClose={() => setEditing(null)}
        footer={
          <Button
            variant="accent"
            busy={save.isPending}
            onClick={() => editing && save.mutate(editing)}
          >
            Save
          </Button>
        }
      >
        <ErrorAlert error={save.error} />
        {editing !== null && (
          <div className="stack">
            <div className="form-grid">
              <Field label="User name" required>
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    disabled={editing.id !== undefined}
                    value={editing.user.username}
                    onChange={(e) =>
                      setEditing({
                        ...editing,
                        user: { ...editing.user, username: e.target.value },
                      })
                    }
                  />
                )}
              </Field>
              <Field label="Full name" required>
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    value={editing.user.fullName}
                    onChange={(e) =>
                      setEditing({
                        ...editing,
                        user: { ...editing.user, fullName: e.target.value },
                      })
                    }
                  />
                )}
              </Field>
              <Field label="Email">
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    type="email"
                    value={editing.user.email ?? ''}
                    onChange={(e) =>
                      setEditing({ ...editing, user: { ...editing.user, email: e.target.value } })
                    }
                  />
                )}
              </Field>
              <Field label="Authorization limit" hint="Blank = unlimited">
                {(id) => (
                  <input
                    id={id}
                    className="input num"
                    type="number"
                    value={editing.user.authorizationLimit ?? ''}
                    onChange={(e) =>
                      setEditing({
                        ...editing,
                        user: {
                          ...editing.user,
                          authorizationLimit:
                            e.target.value === '' ? undefined : Number(e.target.value),
                        },
                      })
                    }
                  />
                )}
              </Field>
              {editing.id === undefined && (
                <Field
                  label="Initial password"
                  required
                  hint="10+ chars, upper, lower, digit and symbol"
                >
                  {(id) => (
                    <input
                      id={id}
                      className="input"
                      type="password"
                      autoComplete="new-password"
                      value={editing.password}
                      onChange={(e) => setEditing({ ...editing, password: e.target.value })}
                    />
                  )}
                </Field>
              )}
            </div>
            <fieldset className="row" style={{ border: 'none', padding: 0 }}>
              <legend className="kpi-label">Roles</legend>
              {(roles.data ?? []).map((r) => (
                <label key={r.code} className="checkbox">
                  <input
                    type="checkbox"
                    checked={editing.user.roleCodes.includes(r.code)}
                    onChange={() => toggleRole(r.code)}
                  />
                  {r.name}
                </label>
              ))}
            </fieldset>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={editing.user.enabled}
                onChange={(e) =>
                  setEditing({ ...editing, user: { ...editing.user, enabled: e.target.checked } })
                }
              />
              Enabled
            </label>
          </div>
        )}
      </Modal>
    </div>
  );
}

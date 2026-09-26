import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, UserCheck } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminApi } from '@/api/admin';
import { nbadminApi } from '@/api/nbadmin';
import type { UserInput } from '@/api/admin';
import type { UserProfile } from '@/api/types';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
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
  return u.enabled ? 'ACTIVE' : 'DISABLED';
}

const STATUS_FILTERS = ['ACTIVE', 'DISABLED', 'LOCKED'] as const;

function matches(u: UserProfile, text: string, status: string): boolean {
  const needle = text.toLowerCase();
  const found =
    needle === '' ||
    u.username.toLowerCase().includes(needle) ||
    u.fullName.toLowerCase().includes(needle) ||
    (u.windowsId ?? '').toLowerCase().includes(needle);
  return found && (status === '' || statusOf(u) === status);
}

const EMPTY: UserInput = { username: '', fullName: '', email: '', roleCodes: [], enabled: true };

/**
 * User administration (FR-UA-052): users with their Windows ID, business unit, user level and
 * status, unlock and password reset. Creating and changing users is done through access requests
 * ("Raise Request"); the direct edit stays for the System Administrator in an emergency
 * (UAM_DIRECT_ROLE_EDIT).
 */
export default function UsersPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Editing | null>(null);
  const [text, setText] = useState('');
  const [status, setStatus] = useState('');
  const users = useQuery({ queryKey: ['users'], queryFn: adminApi.users });
  const roles = useQuery({ queryKey: ['roles'], queryFn: adminApi.roles });
  const settings = useQuery({ queryKey: ['nbadmin', 'settings'], queryFn: nbadminApi.settings });
  const direct = settings.data?.directRoleEdit === true && can('USER_MANAGE');

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
        description="Accounts lock after three failed sign-ins. Users are enrolled and changed through access requests; unlock and password reset stay here."
        actions={
          <>
            <Button
              variant="secondary"
              icon={<UserCheck size={16} />}
              onClick={() => void navigate('/user-access/requests/new')}
            >
              Raise Request
            </Button>
            {direct && (
              <Button
                variant="accent"
                icon={<Plus size={16} />}
                onClick={() => setEditing({ user: EMPTY, password: '' })}
              >
                New User (Emergency)
              </Button>
            )}
          </>
        }
      />
      <ErrorAlert error={unlock.error} />
      <Card flush>
        <WorklistToolbar
          placeholder="Search User ID, Name or Windows ID"
          onSearch={setText}
          extra={
            <select
              className="select"
              aria-label="Status"
              value={status}
              onChange={(e) => setStatus(e.target.value)}
            >
              <option value="">All statuses</option>
              {STATUS_FILTERS.map((s) => (
                <option key={s} value={s}>
                  {s.charAt(0) + s.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          }
        />
        <DataTable<UserProfile>
          loading={users.isLoading}
          rows={(users.data ?? []).filter((u) => matches(u, text, status))}
          rowKey={(u) => u.id}
          onRowClick={(u) =>
            direct
              ? edit(u)
              : void navigate(`/user-access/requests/new?type=MODIFY_USER&user=${u.username}`)
          }
          columns={[
            { key: 'u', header: 'User ID', render: (u) => <strong>{u.username}</strong> },
            { key: 'w', header: 'Windows ID', render: (u) => u.windowsId ?? '' },
            { key: 'n', header: 'Full Name', render: (u) => u.fullName },
            { key: 'r', header: 'Group Profiles', render: (u) => u.roles.join(', ') },
            { key: 'bu', header: 'Business Unit', render: (u) => u.businessUnitCode ?? '' },
            { key: 'lv', header: 'User Level', render: (u) => u.userLevel ?? '' },
            {
              key: 'l',
              header: 'Authorization Limit',
              numeric: true,
              render: (u) =>
                u.authorizationLimit === undefined
                  ? 'Unlimited'
                  : formatAmount(u.authorizationLimit),
            },
            { key: 'll', header: 'Last Login', render: (u) => formatDateTime(u.lastLoginAt) },
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

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { adminApi } from '@/api/admin';
import type { Role } from '@/api/admin';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';

/** Role / permission matrix. Each role is a bundle of fine-grained permissions. */
export default function RolesPage() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const roles = useQuery({ queryKey: ['roles'], queryFn: adminApi.roles });
  const permissions = useQuery({ queryKey: ['permissions'], queryFn: adminApi.permissions });
  const [draft, setDraft] = useState<Record<number, string[]>>({});

  const save = useMutation({
    mutationFn: (role: Role) =>
      adminApi.updateRole(role.id, {
        code: role.code,
        name: role.name,
        permissions: draft[role.id] ?? role.permissions,
      }),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['roles'] });
      toast.success(`Role ${r.name} updated`);
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
        description="Segregation of duties: makers, checkers, administrators and auditors hold different permissions."
      />
      <ErrorAlert error={save.error} />
      <Card flush>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Permission</th>
                {list.map((r) => (
                  <th key={r.id} style={{ textAlign: 'center' }}>
                    {r.name}
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
                        checked={(draft[r.id] ?? r.permissions).includes(p)}
                        onChange={() => toggle(r, p)}
                      />
                    </td>
                  ))}
                </tr>
              ))}
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
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}

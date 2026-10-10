import { useQuery } from '@tanstack/react-query';
import { nbadminApi } from '@/api/nbadmin';
import { Field } from '@/components/ui/Field';
import { PermissionPicker as GroupedPermissionPicker } from '@/components/broking/PermissionPicker';
import { useSodRules } from '@/components/broking/useSodRules';
import type { AccessRequestErrors, AccessRequestForm } from './accessRequest';

/**
 * Permission picker of the group-profile requests (PMADD05; BRD 3.002 "tasks by module"): the
 * permissions by business area with search, the selection, separation-of-duties and privilege
 * warnings and, for a change, the permissions added and removed against the current ones.
 */
export function PermissionPicker({
  legend,
  current,
  selected,
  onChange,
  error,
  offerCopy = false,
}: Readonly<{
  legend: string;
  current: string[];
  selected: string[];
  onChange: (permissions: string[]) => void;
  error?: string;
  offerCopy?: boolean;
}>) {
  const matrix = useQuery({ queryKey: ['nbadmin', 'matrix'], queryFn: nbadminApi.matrix });
  const roles = useQuery({ queryKey: ['nbadmin', 'roles'], queryFn: nbadminApi.roles });
  const rules = useSodRules();
  const catalog = matrix.data?.permissions ?? [];
  const approving = new Set(
    catalog.filter((p) => p.actions.includes('APPROVE')).map((p) => p.permission),
  );
  return (
    <GroupedPermissionPicker
      legend={legend}
      required
      permissions={catalog.map((p) => p.permission)}
      approving={approving}
      current={current}
      selected={selected}
      onChange={onChange}
      profiles={roles.data ?? []}
      rules={rules}
      offerCopy={offerCopy}
      error={error}
    />
  );
}

/**
 * Role and permission picker of a role-permission change request (PMADD05): choose the role, then
 * tick the permissions it should hold, grouped by functional area.
 */
export function RolePermissionFields({
  form,
  set,
  errors,
}: Readonly<{
  form: AccessRequestForm;
  set: (p: Partial<AccessRequestForm>) => void;
  errors: AccessRequestErrors;
}>) {
  const roles = useQuery({ queryKey: ['nbadmin', 'roles'], queryFn: nbadminApi.roles });
  const pickRole = (code: string) => {
    const current = roles.data?.find((r) => r.code === code)?.permissions ?? [];
    set({ roleCode: code, currentPermissions: current, permissions: current });
  };
  return (
    <>
      <Field label="Group Profile" required error={errors.roleCode}>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.roleCode}
            onChange={(e) => pickRole(e.target.value)}
          >
            <option value="">Select a group profile…</option>
            {(roles.data ?? [])
              .filter((r) => r.active)
              .map((r) => (
                <option key={r.code} value={r.code}>
                  {r.name}
                </option>
              ))}
          </select>
        )}
      </Field>
      {form.roleCode !== '' && (
        <PermissionPicker
          legend="Permissions of the profile after the change"
          current={form.currentPermissions}
          selected={form.permissions}
          onChange={(permissions) => set({ permissions })}
          error={errors.permissions}
        />
      )}
    </>
  );
}

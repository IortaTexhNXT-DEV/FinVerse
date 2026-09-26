import { useQuery } from '@tanstack/react-query';
import { nbadminApi } from '@/api/nbadmin';
import { Field } from '@/components/ui/Field';
import { areaLabel, groupByArea } from './accessMatrix';
import type { AccessRequestErrors, AccessRequestForm } from './accessRequest';
import { roleChanges } from './accessRequest';

/**
 * Permission picker grouped by functional area (PMADD05; BRD 3.002 "tasks by module"), with the
 * permissions added and removed against the current ones.
 */
export function PermissionPicker({
  legend,
  current,
  selected,
  onChange,
  error,
}: Readonly<{
  legend: string;
  current: string[];
  selected: string[];
  onChange: (permissions: string[]) => void;
  error?: string;
}>) {
  const matrix = useQuery({ queryKey: ['nbadmin', 'matrix'], queryFn: nbadminApi.matrix });
  const groups = groupByArea(matrix.data?.permissions ?? []);
  const { added, removed } = roleChanges(current, selected);
  const toggle = (permission: string, on: boolean) =>
    onChange(on ? [...selected, permission] : selected.filter((p) => p !== permission));
  return (
    <fieldset className="stack permission-picker">
      <legend className="required">{legend}</legend>
      {groups.map((g) => (
        <div key={g.area}>
          <div className="permission-area">{areaLabel(g.area)}</div>
          <div className="form-grid">
            {g.permissions.map((p) => (
              <label key={p} className="checkbox-field mono" htmlFor={`perm-${p}`}>
                <input
                  id={`perm-${p}`}
                  type="checkbox"
                  checked={selected.includes(p)}
                  onChange={(e) => toggle(p, e.target.checked)}
                />
                {p}
              </label>
            ))}
          </div>
        </div>
      ))}
      {error && (
        <span className="field-error" role="alert">
          {error}
        </span>
      )}
      <dl className="detail-list">
        <dt>Added</dt>
        <dd>{added.join(', ') || '—'}</dd>
        <dt>Removed</dt>
        <dd>{removed.join(', ') || '—'}</dd>
      </dl>
    </fieldset>
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
                  {r.code} – {r.name}
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

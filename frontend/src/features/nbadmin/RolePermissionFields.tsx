import { useQuery } from '@tanstack/react-query';
import { nbadminApi } from '@/api/nbadmin';
import { Field } from '@/components/ui/Field';
import { areaLabel, groupByArea } from './accessMatrix';
import type { AccessRequestErrors, AccessRequestForm } from './accessRequest';
import { roleChanges } from './accessRequest';

/**
 * Role and permission picker of a role-permission change request (PMADD05): choose the role, then
 * tick the permissions it should hold, grouped by functional area. The permissions added and
 * removed are summarised below the picker.
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
  const matrix = useQuery({ queryKey: ['nbadmin', 'matrix'], queryFn: nbadminApi.matrix });
  const groups = groupByArea(matrix.data?.permissions ?? []);
  const { added, removed } = roleChanges(form.currentPermissions, form.permissions);
  const pickRole = (code: string) => {
    const current = roles.data?.find((r) => r.code === code)?.permissions ?? [];
    set({ roleCode: code, currentPermissions: current, permissions: current });
  };
  const toggle = (permission: string, on: boolean) =>
    set({
      permissions: on
        ? [...form.permissions, permission]
        : form.permissions.filter((p) => p !== permission),
    });
  return (
    <>
      <Field label="Role" required error={errors.roleCode}>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.roleCode}
            onChange={(e) => pickRole(e.target.value)}
          >
            <option value="">Select a role…</option>
            {(roles.data ?? []).map((r) => (
              <option key={r.code} value={r.code}>
                {r.code} – {r.name}
              </option>
            ))}
          </select>
        )}
      </Field>
      {form.roleCode !== '' && (
        <fieldset className="stack permission-picker">
          <legend className="required">Permissions of the role after the change</legend>
          {groups.map((g) => (
            <div key={g.area}>
              <div className="permission-area">{areaLabel(g.area)}</div>
              <div className="form-grid">
                {g.permissions.map((p) => (
                  <label key={p} className="checkbox-field mono" htmlFor={`perm-${p}`}>
                    <input
                      id={`perm-${p}`}
                      type="checkbox"
                      checked={form.permissions.includes(p)}
                      onChange={(e) => toggle(p, e.target.checked)}
                    />
                    {p}
                  </label>
                ))}
              </div>
            </div>
          ))}
          {errors.permissions && (
            <span className="field-error" role="alert">
              {errors.permissions}
            </span>
          )}
          <dl className="detail-list">
            <dt>Added</dt>
            <dd>{added.join(', ') || '—'}</dd>
            <dt>Removed</dt>
            <dd>{removed.join(', ') || '—'}</dd>
          </dl>
        </fieldset>
      )}
    </>
  );
}

import { useQuery } from '@tanstack/react-query';
import { nbadminApi } from '@/api/nbadmin';
import type { PrivilegeLevel } from '@/api/nbadmin';
import { Field } from '@/components/ui/Field';
import { humanize } from '@/utils/format';
import { PRIVILEGE_LEVELS } from './accessRequest';
import type { AccessRequestErrors, AccessRequestForm } from './accessRequest';
import { PermissionPicker, RolePermissionFields } from './RolePermissionFields';

interface FieldsProps {
  form: AccessRequestForm;
  set: (p: Partial<AccessRequestForm>) => void;
  errors: AccessRequestErrors;
}

function ProfileData({
  form,
  set,
  errors,
  creating,
}: Readonly<FieldsProps & { creating: boolean }>) {
  return (
    <div className="form-grid">
      {creating && (
        <Field label="Profile Code" required error={errors.roleCode} hint="A-Z, 0-9 and _">
          {(id) => (
            <input
              id={id}
              className="input mono"
              maxLength={40}
              value={form.roleCode}
              onChange={(e) => set({ roleCode: e.target.value.toUpperCase() })}
            />
          )}
        </Field>
      )}
      <Field label={creating ? 'Name' : 'New Name'} required={creating} error={errors.roleName}>
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={120}
            value={form.roleName}
            onChange={(e) => set({ roleName: e.target.value })}
          />
        )}
      </Field>
      <Field label="Description">
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={500}
            value={form.roleDescription}
            onChange={(e) => set({ roleDescription: e.target.value })}
          />
        )}
      </Field>
      <Field label="Privilege Level" hint="HIGH and ADMIN profiles need a second approval">
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.privilegeLevel}
            onChange={(e) => set({ privilegeLevel: e.target.value as PrivilegeLevel | '' })}
          >
            <option value="">{creating ? 'Standard' : 'Unchanged'}</option>
            {PRIVILEGE_LEVELS.map((l) => (
              <option key={l} value={l}>
                {humanize(l)}
              </option>
            ))}
          </select>
        )}
      </Field>
    </div>
  );
}

function Activation({ form, set, errors }: Readonly<FieldsProps>) {
  const deactivate = form.type === 'DEACTIVATE_ROLE';
  const roles = useQuery({ queryKey: ['nbadmin', 'roles'], queryFn: nbadminApi.roles });
  const users = useQuery({ queryKey: ['nbadmin', 'users'], queryFn: nbadminApi.users });
  const offered = (roles.data ?? []).filter(
    (r) => r.active === deactivate && !(deactivate && r.code === 'SYSADMIN'),
  );
  const members = (users.data ?? []).filter((u) => u.roleCodes.includes(form.roleCode));
  return (
    <>
      <Field label="Group Profile" required error={errors.roleCode}>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.roleCode}
            onChange={(e) => set({ roleCode: e.target.value })}
          >
            <option value="">
              {deactivate ? 'Select an active profile…' : 'Select an inactive profile…'}
            </option>
            {offered.map((r) => (
              <option key={r.code} value={r.code}>
                {r.code} – {r.name}
              </option>
            ))}
          </select>
        )}
      </Field>
      {deactivate && form.roleCode !== '' && (
        <div className="alert warning" role="status">
          {members.length === 0
            ? 'The profile has no members.'
            : `Members who lose the profile's screens: ${members.map((m) => m.username).join(', ')}`}
        </div>
      )}
    </>
  );
}

/**
 * The fields of a group-profile request (BRD 3.002.1-4; FR-UA-040 to FR-UA-043): a new profile
 * with its code, name, level and permissions; a change of the permissions, name or level; or the
 * deactivation (members listed) or reactivation of a profile.
 */
export function GroupProfileFields({ form, set, errors }: Readonly<FieldsProps>) {
  if (form.type === 'DEACTIVATE_ROLE' || form.type === 'REACTIVATE_ROLE') {
    return <Activation form={form} set={set} errors={errors} />;
  }
  if (form.type === 'CREATE_ROLE') {
    return (
      <>
        <ProfileData form={form} set={set} errors={errors} creating />
        <PermissionPicker
          legend="Permissions of the new profile"
          current={[]}
          selected={form.permissions}
          onChange={(permissions) => set({ permissions })}
          error={errors.permissions}
        />
      </>
    );
  }
  return (
    <>
      <RolePermissionFields form={form} set={set} errors={errors} />
      {form.roleCode !== '' && (
        <ProfileData form={form} set={set} errors={errors} creating={false} />
      )}
    </>
  );
}

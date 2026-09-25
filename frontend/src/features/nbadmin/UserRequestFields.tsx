import { useQuery } from '@tanstack/react-query';
import { nbadminApi } from '@/api/nbadmin';
import type { UserAccess } from '@/api/nbadmin';
import { LovSelect } from '@/components/broking/LovSelect';
import { Field } from '@/components/ui/Field';
import { useWorkspace } from '@/context/workspaceContext';
import { usersFor } from './accessRequest';
import type { AccessRequestErrors, AccessRequestForm } from './accessRequest';

interface FieldsProps {
  form: AccessRequestForm;
  set: (p: Partial<AccessRequestForm>) => void;
  errors: AccessRequestErrors;
  users: UserAccess[];
}

function UserField({ form, set, errors, users }: Readonly<FieldsProps>) {
  const typed = form.type === 'CREATE_USER' || form.userType === 'EXTERNAL';
  return (
    <Field
      label="User ID"
      required
      error={errors.username}
      hint={typed ? 'BDOI format, e.g. a013000196' : undefined}
    >
      {(id) =>
        typed ? (
          <input
            id={id}
            className="input mono"
            maxLength={50}
            value={form.username}
            onChange={(e) => set({ username: e.target.value })}
          />
        ) : (
          <select
            id={id}
            className="select"
            value={form.username}
            onChange={(e) => {
              const user = users.find((u) => u.username === e.target.value);
              set({ username: e.target.value, roleCodes: user?.roleCodes ?? [] });
            }}
          >
            <option value="">Select a user…</option>
            {usersFor(form, users).map((u) => (
              <option key={u.username} value={u.username}>
                {u.username} – {u.fullName}
                {u.windowsId ? ` (${u.windowsId})` : ''}
              </option>
            ))}
          </select>
        )
      }
    </Field>
  );
}

function current(value: string | undefined): string {
  return `Current: ${value ?? '—'}`;
}

function DataFields({ form, set, errors, users }: Readonly<FieldsProps>) {
  const { branches } = useWorkspace();
  const modify = form.type === 'MODIFY_USER';
  const user = modify ? users.find((u) => u.username === form.username) : undefined;
  const hint = (value: string | undefined) => (modify ? current(value) : undefined);
  const text = (label: string, key: 'fullName' | 'email' | 'windowsId', value?: string) => (
    <Field
      label={label}
      required={key === 'fullName' && !modify}
      error={errors[key]}
      hint={hint(value)}
    >
      {(id) => (
        <input
          id={id}
          className="input"
          maxLength={120}
          type={key === 'email' ? 'email' : 'text'}
          value={form[key]}
          onChange={(e) => set({ [key]: e.target.value })}
        />
      )}
    </Field>
  );
  return (
    <div className="form-grid">
      {text('Full Name', 'fullName', user?.fullName)}
      {text('E-mail', 'email', user?.email)}
      {text('Windows ID', 'windowsId', user?.windowsId)}
      <Field label="Home Branch">
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.homeBranchId}
            onChange={(e) => set({ homeBranchId: e.target.value })}
          >
            <option value="">{modify ? 'Unchanged' : 'Not specified'}</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>
                {b.code} – {b.name}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Business Unit Group" hint={hint(user?.businessUnitCode)}>
        {(id) => (
          <LovSelect
            id={id}
            type="UAM_BUSINESS_UNIT"
            value={form.businessUnitCode}
            onChange={(businessUnitCode) => set({ businessUnitCode })}
          />
        )}
      </Field>
      <Field label="User Level" hint={hint(user?.userLevel)}>
        {(id) => (
          <LovSelect
            id={id}
            type="UAM_USER_LEVEL"
            value={form.userLevel}
            onChange={(userLevel) => set({ userLevel })}
          />
        )}
      </Field>
    </div>
  );
}

function RolePicker({ form, set, errors, users }: Readonly<FieldsProps>) {
  const roles = useQuery({ queryKey: ['nbadmin', 'roles'], queryFn: nbadminApi.roles });
  const held = users.find((u) => u.username === form.username)?.roleCodes ?? [];
  const toggle = (code: string, on: boolean) =>
    set({ roleCodes: on ? [...form.roleCodes, code] : form.roleCodes.filter((r) => r !== code) });
  return (
    <fieldset className="stack permission-picker">
      <legend className={form.type === 'CREATE_USER' ? 'required' : undefined}>
        Group Profiles
      </legend>
      <div className="form-grid">
        {(roles.data ?? [])
          .filter((r) => r.active)
          .map((r) => (
            <label key={r.code} className="checkbox-field" htmlFor={`role-${r.code}`}>
              <input
                id={`role-${r.code}`}
                type="checkbox"
                checked={form.roleCodes.includes(r.code)}
                onChange={(e) => toggle(r.code, e.target.checked)}
              />
              {r.name}
              {held.includes(r.code) ? ' (current)' : ''}
            </label>
          ))}
      </div>
      {errors.roleCodes && (
        <span className="field-error" role="alert">
          {errors.roleCodes}
        </span>
      )}
    </fieldset>
  );
}

function ExternalParty({ form, set, errors }: Readonly<FieldsProps>) {
  return (
    <div className="form-grid">
      <Field label="Party" required error={errors.partyKind}>
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.partyKind}
            onChange={(e) => set({ partyKind: e.target.value as AccessRequestForm['partyKind'] })}
          >
            <option value="">Select…</option>
            <option value="INSURER">Insurer</option>
            <option value="CLIENT">Client</option>
          </select>
        )}
      </Field>
      <Field label="Party Code" required error={errors.partyCode}>
        {(id) => (
          <input
            id={id}
            className="input mono"
            maxLength={40}
            value={form.partyCode}
            onChange={(e) => set({ partyCode: e.target.value })}
          />
        )}
      </Field>
      <Field label="Portal Role">
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.portalRole}
            onChange={(e) => set({ portalRole: e.target.value })}
          >
            <option value="">Select…</option>
            <option value="INSURER_USER">Insurer user</option>
            <option value="CLIENT_HR">Client HR</option>
          </select>
        )}
      </Field>
    </div>
  );
}

function StateFields({ form, set, users }: Readonly<FieldsProps>) {
  if (form.type === 'DISABLE_USER') {
    return (
      <Field label="Reason">
        {(id) => (
          <LovSelect
            id={id}
            type="UAM_DEACTIVATION_REASON"
            value={form.reasonCode}
            onChange={(reasonCode) => set({ reasonCode })}
          />
        )}
      </Field>
    );
  }
  const locked = users.find((u) => u.username === form.username)?.locked === true;
  return locked ? (
    <label className="checkbox-field" htmlFor="unlock">
      <input
        id="unlock"
        type="checkbox"
        checked={form.unlock}
        onChange={(e) => set({ unlock: e.target.checked })}
      />
      Also unlock the account
    </label>
  ) : null;
}

/**
 * The fields of a user request (BRD 1.002-1.005; FR-UA-011 to FR-UA-014): the user, the new data
 * with the current values, the group profiles, the reason of a deactivation, the unlock of a
 * reactivation, or the party of an external (portal) user.
 */
export function UserRequestFields(props: Readonly<FieldsProps>) {
  const { form } = props;
  const withData = form.type === 'CREATE_USER' || form.type === 'MODIFY_USER';
  const internal = form.userType === 'INTERNAL';
  return (
    <>
      <div className="form-grid">
        <UserField {...props} />
      </div>
      {!internal && <ExternalParty {...props} />}
      {withData && <DataFields {...props} />}
      {withData && internal && <RolePicker {...props} />}
      {(form.type === 'DISABLE_USER' || form.type === 'ENABLE_USER') && internal && (
        <StateFields {...props} />
      )}
    </>
  );
}

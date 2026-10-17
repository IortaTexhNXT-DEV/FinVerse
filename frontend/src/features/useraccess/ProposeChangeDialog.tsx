import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessRequest, PrivilegeLevel } from '@/api/nbadmin';
import { PermissionPicker } from '@/components/broking/PermissionPicker';
import { useSodRules } from '@/components/broking/useSodRules';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';
import {
  CHANGE_LABELS,
  PRIVILEGE_LEVELS,
  hasErrors,
  toInput,
  validateChange,
} from './groupProfileChange';
import type { ChangeErrors, ChangeForm } from './groupProfileChange';

function Definition({
  form,
  set,
  errors,
  placeholders,
}: Readonly<{
  form: ChangeForm;
  set: (p: Partial<ChangeForm>) => void;
  errors: ChangeErrors;
  placeholders?: { name: string; description: string };
}>) {
  const creating = form.type === 'CREATE_ROLE';
  return (
    <div className="form-grid">
      {creating && (
        <Field label="Code" required error={errors.roleCode} hint="Capital letters, digits and _">
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
      <Field
        label={creating ? 'Name' : 'New Name'}
        required={creating}
        error={errors.roleName}
        hint={creating ? undefined : 'Blank keeps the current name'}
      >
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={120}
            placeholder={placeholders?.name}
            value={form.roleName}
            onChange={(e) => set({ roleName: e.target.value })}
          />
        )}
      </Field>
      <Field
        label="Description"
        hint={creating ? undefined : 'Blank keeps the current description'}
      >
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={500}
            placeholder={placeholders?.description}
            value={form.roleDescription}
            onChange={(e) => set({ roleDescription: e.target.value })}
          />
        )}
      </Field>
      <Field label="Privilege Level" hint="High and Admin profiles need a second approval">
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

function Permissions({
  form,
  set,
  error,
}: Readonly<{ form: ChangeForm; set: (p: Partial<ChangeForm>) => void; error?: string }>) {
  const matrix = useQuery({ queryKey: ['nbadmin', 'matrix'], queryFn: nbadminApi.matrix });
  const roles = useQuery({ queryKey: ['nbadmin', 'roles'], queryFn: nbadminApi.roles });
  const rules = useSodRules();
  const catalog = matrix.data?.permissions ?? [];
  const creating = form.type === 'CREATE_ROLE';
  return (
    <PermissionPicker
      legend={
        creating ? 'Permissions of the new profile' : 'Permissions of the profile after the change'
      }
      required
      permissions={catalog.map((p) => p.permission)}
      approving={
        new Set(catalog.filter((p) => p.actions.includes('APPROVE')).map((p) => p.permission))
      }
      current={form.current}
      selected={form.permissions}
      onChange={(permissions) => set({ permissions })}
      profiles={roles.data ?? []}
      rules={rules}
      offerCopy={creating}
      error={error}
    />
  );
}

function MembersNotice({ members }: Readonly<{ members?: number }>) {
  return (
    <Notice tone="warning" title="Members lose the profile's access">
      {members === undefined || members === 0
        ? 'The profile has no members.'
        : `${String(members)} member(s) lose the permissions of this profile once the change applies; UIDM-ISC keeps their membership.`}
    </Notice>
  );
}

function TextField({
  label,
  value,
  error,
  onChange,
}: Readonly<{ label: string; value: string; error?: string; onChange: (v: string) => void }>) {
  return (
    <Field label={label} required error={error}>
      {(id) => (
        <textarea
          id={id}
          className="textarea"
          rows={2}
          maxLength={1000}
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

/** Saves a proposal: a new one, or the draft or returned change, and submits it when asked. */
async function saveChange(
  form: ChangeForm,
  saved: AccessRequest | undefined,
  submit: boolean,
  remarks: string,
): Promise<AccessRequest> {
  const body = toInput(form);
  if (saved === undefined) {
    return nbadminApi.create(body, !submit);
  }
  const edited = await nbadminApi.edit(saved.id, body);
  return submit ? nbadminApi.submitSaved(edited.id, remarks.trim() || undefined) : edited;
}

/**
 * The maker's proposal of a group-profile change: a new profile, a change of its definition or
 * permissions, its deactivation or reactivation. Save Draft keeps it for later; Submit sends it to
 * the checkers (any holder of the approval right other than the maker decides it).
 */
export function ProposeChangeDialog({
  initial,
  saved,
  members,
  placeholders,
  onClose,
  onDone,
}: Readonly<{
  initial: ChangeForm;
  /** The draft or returned change being corrected. */
  saved?: AccessRequest;
  /** Members of the profile (a deactivation removes their access). */
  members?: number;
  placeholders?: { name: string; description: string };
  onClose: () => void;
  onDone: (r: AccessRequest) => void;
}>) {
  const toast = useToast();
  const queries = useQueryClient();
  const [form, setForm] = useState(initial);
  const [tried, setTried] = useState<'draft' | 'submit' | null>(null);
  const [remarks, setRemarks] = useState('');
  const set = (p: Partial<ChangeForm>) => setForm((f) => ({ ...f, ...p }));
  const errors = tried === null ? {} : validateChange(form, tried === 'submit');
  const returned = saved?.status === 'RETURNED';
  const remarkMissing = returned && remarks.trim() === '';
  const save = useMutation({
    mutationFn: (submit: boolean) => saveChange(form, saved, submit, remarks),
    onSuccess: async (r, submit) => {
      toast.success(
        submit ? `${r.requestNo} sent for approval` : `${r.requestNo} saved as a draft`,
      );
      await queries.invalidateQueries({ queryKey: ['nbadmin'] });
      onDone(r);
    },
  });
  const run = (submit: boolean) => {
    setTried(submit ? 'submit' : 'draft');
    if (!hasErrors(validateChange(form, submit)) && !(submit && remarkMissing)) {
      save.mutate(submit);
    }
  };
  const activation = form.type === 'DEACTIVATE_ROLE' || form.type === 'REACTIVATE_ROLE';
  return (
    <Modal
      title={`Propose: ${CHANGE_LABELS[form.type]}`}
      size="lg"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          {!returned && (
            <Button variant="secondary" busy={save.isPending} onClick={() => run(false)}>
              Save Draft
            </Button>
          )}
          <Button variant="accent" busy={save.isPending} onClick={() => run(true)}>
            Submit for Approval
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} title="The change was not saved" />
        {form.type === 'DEACTIVATE_ROLE' && <MembersNotice members={members} />}
        {!activation && (
          <>
            <Definition form={form} set={set} errors={errors} placeholders={placeholders} />
            <Permissions form={form} set={set} error={errors.permissions} />
          </>
        )}
        <TextField
          label="Reason"
          value={form.justification}
          error={errors.justification}
          onChange={(justification) => set({ justification })}
        />
        {returned && (
          <TextField
            label="What was corrected"
            value={remarks}
            error={tried === 'submit' && remarkMissing ? 'Enter what was corrected' : undefined}
            onChange={setRemarks}
          />
        )}
      </div>
    </Modal>
  );
}

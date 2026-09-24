import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessRequest, AccessRequestType } from '@/api/nbadmin';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useWorkspace } from '@/context/workspaceContext';
import {
  EMPTY_ACCESS_REQUEST,
  REQUEST_TYPE_LABELS,
  toAccessRequest,
  validateAccessRequest,
} from './accessRequest';
import type { AccessRequestErrors, AccessRequestForm } from './accessRequest';

const TYPES = Object.keys(REQUEST_TYPE_LABELS) as AccessRequestType[];

function RolePicker({
  form,
  set,
  error,
}: Readonly<{
  form: AccessRequestForm;
  set: (p: Partial<AccessRequestForm>) => void;
  error?: string;
}>) {
  const roles = useQuery({ queryKey: ['nbadmin', 'roles'], queryFn: nbadminApi.roles });
  const toggle = (code: string, on: boolean) =>
    set({ roleCodes: on ? [...form.roleCodes, code] : form.roleCodes.filter((r) => r !== code) });
  return (
    <fieldset className="stack" style={{ border: 0, padding: 0, margin: 0 }}>
      <legend className="required">Roles</legend>
      <div className="form-grid">
        {(roles.data ?? []).map((r) => (
          <label key={r.code} className="checkbox-field" htmlFor={`role-${r.code}`}>
            <input
              id={`role-${r.code}`}
              type="checkbox"
              checked={form.roleCodes.includes(r.code)}
              onChange={(e) => toggle(r.code, e.target.checked)}
            />
            {r.name}
          </label>
        ))}
      </div>
      {error && (
        <span className="field-error" role="alert">
          {error}
        </span>
      )}
    </fieldset>
  );
}

function CreateFields({
  form,
  set,
  errors,
}: Readonly<{
  form: AccessRequestForm;
  set: (p: Partial<AccessRequestForm>) => void;
  errors: AccessRequestErrors;
}>) {
  const { branches } = useWorkspace();
  return (
    <div className="form-grid">
      <Field label="Full name" required error={errors.fullName}>
        {(id) => (
          <input
            id={id}
            className="input"
            value={form.fullName}
            onChange={(e) => set({ fullName: e.target.value })}
          />
        )}
      </Field>
      <Field label="E-mail">
        {(id) => (
          <input
            id={id}
            className="input"
            type="email"
            value={form.email}
            onChange={(e) => set({ email: e.target.value })}
          />
        )}
      </Field>
      <Field label="Home branch">
        {(id) => (
          <select
            id={id}
            className="select"
            value={form.homeBranchId}
            onChange={(e) => set({ homeBranchId: e.target.value })}
          >
            <option value="">Not specified</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>
                {b.code} – {b.name}
              </option>
            ))}
          </select>
        )}
      </Field>
    </div>
  );
}

/**
 * New user access request (BRNB.085, BRD 3.3.5): create a user, change roles, disable or enable a
 * user, with a justification. Nothing changes until the Approver approves the request.
 */
export function AccessRequestFormDialog({
  onClose,
  onSubmitted,
}: Readonly<{ onClose: () => void; onSubmitted: (r: AccessRequest) => void }>) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<AccessRequestForm>(EMPTY_ACCESS_REQUEST);
  const [submitted, setSubmitted] = useState(false);
  const users = useQuery({ queryKey: ['nbadmin', 'users'], queryFn: nbadminApi.users });
  const set = (patch: Partial<AccessRequestForm>) => setForm((f) => ({ ...f, ...patch }));
  const errors = submitted ? validateAccessRequest(form, users.data ?? []) : {};
  const submit = useMutation({
    mutationFn: () => nbadminApi.submit(toAccessRequest(form)),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['nbadmin'] });
      onSubmitted(r);
    },
  });
  const send = () => {
    setSubmitted(true);
    if (Object.keys(validateAccessRequest(form, users.data ?? [])).length === 0) {
      submit.mutate();
    }
  };
  const existingUser = form.type !== 'CREATE_USER';
  return (
    <Modal
      open
      title="New Access Request"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={submit.isPending} onClick={send}>
            Submit for Approval
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={submit.error ?? users.error} />
        <div className="form-grid">
          <Field label="Request" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.type}
                onChange={(e) => set({ type: e.target.value as AccessRequestType, username: '' })}
              >
                {TYPES.map((t) => (
                  <option key={t} value={t}>
                    {REQUEST_TYPE_LABELS[t]}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="User name" required error={errors.username}>
            {(id) =>
              existingUser ? (
                <select
                  id={id}
                  className="select"
                  value={form.username}
                  onChange={(e) => {
                    const user = users.data?.find((u) => u.username === e.target.value);
                    set({ username: e.target.value, roleCodes: user?.roleCodes ?? [] });
                  }}
                >
                  <option value="">Select a user…</option>
                  {(users.data ?? []).map((u) => (
                    <option key={u.username} value={u.username}>
                      {u.username} – {u.fullName}
                      {u.enabled ? '' : ' (disabled)'}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  id={id}
                  className="input"
                  value={form.username}
                  onChange={(e) => set({ username: e.target.value })}
                />
              )
            }
          </Field>
        </div>
        {form.type === 'CREATE_USER' && <CreateFields form={form} set={set} errors={errors} />}
        {(form.type === 'CREATE_USER' || form.type === 'MODIFY_ROLES') && (
          <RolePicker form={form} set={set} error={errors.roleCodes} />
        )}
        <Field label="Justification" required error={errors.justification}>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={1000}
              value={form.justification}
              onChange={(e) => set({ justification: e.target.value })}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

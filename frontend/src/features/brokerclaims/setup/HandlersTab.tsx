import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { claimsSetupApi } from './api';
import type { Handler } from './api';

type HandlerForm = Omit<Handler, 'id'>;

function handlerForm(initial: Handler | undefined): HandlerForm {
  if (initial === undefined) {
    return { username: '', unitCode: '', team: '', active: true };
  }
  return {
    username: initial.username,
    unitCode: initial.unitCode,
    team: initial.team ?? '',
    active: initial.active,
  };
}

function handlerErrors(form: HandlerForm, submitted: boolean) {
  return {
    username: submitted && form.username === '' ? 'Select the claims user' : undefined,
    unitCode: submitted && form.unitCode === '' ? 'Select the claims unit' : undefined,
  };
}

function HandlerDialog({
  initial,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  initial?: Handler;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (form: HandlerForm) => void;
}>) {
  const users = useQuery({
    queryKey: ['broker-claims', 'setup', 'users'],
    queryFn: claimsSetupApi.users,
  });
  const [form, setForm] = useState<HandlerForm>(() => handlerForm(initial));
  const [submitted, setSubmitted] = useState(false);
  const errors = handlerErrors(form, submitted);
  return (
    <Modal
      open
      title={initial === undefined ? 'Register Handler' : `Handler · ${initial.username}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={busy}
            onClick={() => {
              setSubmitted(true);
              if (form.username !== '' && form.unitCode !== '') {
                onSave(form);
              }
            }}
          >
            Save Handler
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error ?? users.error} />
        <Field label="Claims user" required error={errors.username}>
          {(id) => (
            <select
              id={id}
              className="select"
              value={form.username}
              disabled={initial !== undefined}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
            >
              <option value="">Select…</option>
              {(users.data ?? []).map((u) => (
                <option key={u} value={u}>
                  {u}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Claims unit" required error={errors.unitCode}>
          {(id) => (
            <LovSelect
              id={id}
              type="BCL_UNIT"
              value={form.unitCode}
              onChange={(v) => setForm({ ...form, unitCode: v })}
              required
            />
          )}
        </Field>
        <Field label="Team">
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={60}
              value={form.team ?? ''}
              onChange={(e) => setForm({ ...form, team: e.target.value })}
            />
          )}
        </Field>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.active}
            onChange={(e) => setForm({ ...form, active: e.target.checked })}
          />
          Active
        </label>
      </div>
    </Modal>
  );
}

/**
 * Claims handler register (BRCLM.012, NFR p.37): the unit and team of each claims user; the unit
 * drives the status access matrix. A user outside the register sets no status.
 */
export function HandlersTab() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Handler | 'new'>();
  const handlers = useQuery({
    queryKey: ['broker-claims', 'setup', 'handlers'],
    queryFn: claimsSetupApi.handlers,
  });
  const save = useMutation({
    mutationFn: claimsSetupApi.saveHandler,
    onSuccess: async (h) => {
      setEditing(undefined);
      await queryClient.invalidateQueries({ queryKey: ['broker-claims', 'setup'] });
      toast.success(`${h.username} saved`);
    },
  });
  return (
    <>
      <div className="worklist-toolbar">
        <div className="worklist-actions">
          <Button icon={<Plus size={16} />} onClick={() => setEditing('new')}>
            Register Handler
          </Button>
        </div>
      </div>
      <ErrorAlert error={handlers.error} />
      <DataTable<Handler>
        caption="Claims handler register"
        loading={handlers.isLoading}
        rows={handlers.data ?? []}
        rowKey={(h) => h.id}
        emptyMessage="No handlers registered"
        onRowClick={(h) => setEditing(h)}
        columns={[
          { key: 'user', header: 'User', render: (h) => <strong>{h.username}</strong> },
          { key: 'unit', header: 'Unit', render: (h) => h.unitCode },
          { key: 'team', header: 'Team', render: (h) => h.team ?? '' },
          {
            key: 'active',
            header: 'Status',
            render: (h) => <StatusBadge status={h.active ? 'ACTIVE' : 'INACTIVE'} />,
          },
        ]}
      />
      {editing !== undefined && (
        <HandlerDialog
          initial={editing === 'new' ? undefined : editing}
          busy={save.isPending}
          error={save.error}
          onClose={() => setEditing(undefined)}
          onSave={(form) => save.mutate(form)}
        />
      )}
    </>
  );
}

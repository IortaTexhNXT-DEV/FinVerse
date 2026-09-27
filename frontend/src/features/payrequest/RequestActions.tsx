import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { ActionNote, WorkAction } from '@/api/workflow';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { workflowKey } from '@/components/broking/workflowKey';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';
import { payRequestApi, REQUEST_ENTITY } from './api';
import type { PayRequest } from './api';
import { ACTION_LABELS, approveLabel, businessActions, submitLabel } from './requestForm';
import type { BusinessAction } from './requestForm';

function AssignDialog({
  request,
  onDone,
  onClose,
}: Readonly<{ request: PayRequest; onDone: (r: PayRequest) => void; onClose: () => void }>) {
  const [username, setUsername] = useState('');
  const assign = useMutation({
    mutationFn: () => payRequestApi.assign(request.id, username.trim()),
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title={`Assign ${request.requestNo}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={assign.isPending}
            disabled={username.trim() === ''}
            onClick={() => assign.mutate()}
          >
            Assign Preparer
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={assign.error} />
        <Field label="Preparer (User ID)" required hint="A Marketing user who prepares refunds">
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={50}
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

function labelOf(action: BusinessAction, r: PayRequest): string {
  if (action === 'submit') {
    return submitLabel(r.kind, r.validationRequired);
  }
  if (action === 'approve') {
    return approveLabel(r.stage, r.kind);
  }
  return ACTION_LABELS[action];
}

const CALLS: Record<
  Exclude<BusinessAction, 'assign'>,
  (id: number, comment?: string) => Promise<PayRequest>
> = {
  submit: payRequestApi.submit,
  endorse: payRequestApi.endorse,
  approve: payRequestApi.approve,
};

/**
 * Business actions of a request offered by the workflow panel (MKT 1.9.0-1.16.3): assign the
 * preparer, submit (for validation or review), endorse and approve. Returns and cancellations are
 * the generic actions of the panel.
 */
export function RequestActions({
  request,
  actions,
}: Readonly<{ request: PayRequest; actions: WorkAction[] }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<BusinessAction | null>(null);
  const refresh = async (r: PayRequest, label: string) => {
    setPending(null);
    queryClient.setQueryData(['payrequest', 'request', r.id], r);
    await queryClient.invalidateQueries({ queryKey: ['payrequest'] });
    await queryClient.invalidateQueries({ queryKey: workflowKey(REQUEST_ENTITY, r.id) });
    toast.success(`${label}: ${r.requestNo} is ${humanize(r.stage).toLowerCase()}`);
  };
  const act = useMutation({
    mutationFn: ({
      action,
      note,
    }: {
      action: Exclude<BusinessAction, 'assign'>;
      note: ActionNote;
    }) => CALLS[action](request.id, note.comment),
    onSuccess: (r, { action }) => refresh(r, labelOf(action, request)),
  });
  const offered = businessActions(actions.map((a) => a.action));
  return (
    <>
      {offered.map((a) => (
        <Button
          key={a}
          variant={a === 'approve' ? 'accent' : 'primary'}
          onClick={() => setPending(a)}
        >
          {labelOf(a, request)}
        </Button>
      ))}
      {pending === 'assign' && (
        <AssignDialog
          request={request}
          onDone={(r) => void refresh(r, 'Assigned')}
          onClose={() => setPending(null)}
        />
      )}
      {pending !== null && pending !== 'assign' && (
        <ActionDialog
          title={`${labelOf(pending, request)} · ${request.requestNo}`}
          confirmLabel={labelOf(pending, request)}
          busy={act.isPending}
          error={act.error}
          onConfirm={(note) => act.mutate({ action: pending, note })}
          onClose={() => setPending(null)}
        />
      )}
    </>
  );
}

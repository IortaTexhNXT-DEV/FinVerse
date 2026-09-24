import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link2 } from 'lucide-react';
import { useState } from 'react';
import type { ActionNote, WorkAction } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { workflowKey } from '@/components/broking/workflowKey';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { adjustmentApi, REQUEST_ENTITY } from './api';
import type { EndorsementRequest } from './api';
import { stageLabel } from './requestForm';

type Business = 'submit' | 'resubmit' | 'validate' | 'approve' | 'post' | 'reapply';

/** Workflow actions of a request and the one button each shows (validate and post have two routes). */
const ACTIONS: Record<string, Business> = {
  submit: 'submit',
  resubmit: 'resubmit',
  validate: 'validate',
  validate_for_posting: 'validate',
  approve: 'approve',
  post: 'post',
  post_pending: 'post',
  reapply: 'reapply',
};

const LABELS: Record<Business, string> = {
  submit: 'Submit for Validation',
  resubmit: 'Resubmit',
  validate: 'Validate',
  approve: 'Approve',
  post: 'Post',
  reapply: 'Re-apply Payments',
};

const CALLS: Record<Business, (id: number, comment?: string) => Promise<EndorsementRequest>> = {
  submit: adjustmentApi.submit,
  resubmit: adjustmentApi.resubmit,
  validate: adjustmentApi.validate,
  approve: adjustmentApi.approve,
  post: adjustmentApi.post,
  reapply: (id) => adjustmentApi.reapply(id),
};

function QuotationLink({
  request,
  onDone,
}: Readonly<{ request: EndorsementRequest; onDone: (r: EndorsementRequest) => void }>) {
  const [open, setOpen] = useState(false);
  const [ref, setRef] = useState('');
  const link = useMutation({
    mutationFn: () => adjustmentApi.linkQuotation(request.id, ref.trim()),
    onSuccess: (r) => {
      setOpen(false);
      onDone(r);
    },
  });
  return (
    <>
      <Button variant="secondary" icon={<Link2 size={16} />} onClick={() => setOpen(true)}>
        Link Quotation
      </Button>
      <Modal
        open={open}
        title="Link Quotation"
        onClose={() => setOpen(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="accent"
              busy={link.isPending}
              disabled={ref.trim() === ''}
              onClick={() => link.mutate()}
            >
              Link Quotation
            </Button>
          </>
        }
      >
        <div className="stack">
          <ErrorAlert error={link.error} />
          <Field
            label="Quotation No."
            required
            hint="The quotation Marketing prepared for the TSI increase (ADJID.008)."
          >
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={40}
                value={ref}
                onChange={(e) => setRef(e.target.value)}
              />
            )}
          </Field>
        </div>
      </Modal>
    </>
  );
}

/**
 * Business actions of an endorsement request offered by the workflow panel (OPERATIONS_DESIGN
 * section 7): submit or resubmit, validate, approve (four eyes), post as a batch of one, re-apply
 * the payments once Cashiering is available, and link the quotation of a TSI increase.
 */
export function RequestActions({
  request,
  actions,
}: Readonly<{ request: EndorsementRequest; actions: WorkAction[] }>) {
  const toast = useToast();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<Business | null>(null);
  const refresh = async (r: EndorsementRequest, label: string) => {
    setPending(null);
    queryClient.setQueryData(['adjustment', 'request', r.id], r);
    await queryClient.invalidateQueries({ queryKey: ['adjustment'] });
    await queryClient.invalidateQueries({ queryKey: workflowKey(REQUEST_ENTITY, r.id) });
    toast.success(`${label}: ${r.requestNo} is ${stageLabel(r.stage).toLowerCase()}`);
  };
  const act = useMutation({
    mutationFn: ({ action, note }: { action: Business; note: ActionNote }) =>
      CALLS[action](request.id, note.comment),
    onSuccess: (r, { action }) => refresh(r, LABELS[action]),
  });
  const offered = [
    ...new Set(actions.map((a) => ACTIONS[a.action]).filter((a): a is Business => a !== undefined)),
  ];
  const needsQuotation =
    request.control.quotationRequired &&
    request.control.quotationRef === undefined &&
    (can('ADJ_REQUEST') || can('ADJ_PROCESS'));
  return (
    <>
      {needsQuotation && (
        <QuotationLink request={request} onDone={(r) => void refresh(r, 'Quotation linked')} />
      )}
      {offered.map((a) => (
        <Button
          key={a}
          variant={a === 'post' || a === 'approve' ? 'accent' : 'primary'}
          onClick={() => setPending(a)}
        >
          {LABELS[a]}
        </Button>
      ))}
      {pending && (
        <ActionDialog
          title={`${LABELS[pending]} ${request.requestNo}`}
          confirmLabel={LABELS[pending]}
          busy={act.isPending}
          error={act.error}
          onConfirm={(note) => act.mutate({ action: pending, note })}
          onClose={() => setPending(null)}
        />
      )}
    </>
  );
}

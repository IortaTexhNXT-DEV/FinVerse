import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Ban, CheckCircle2, CornerDownRight, Printer, Undo2, XCircle } from 'lucide-react';
import { useState } from 'react';
import { receivablesApi } from '@/api/receivables';
import type { Receipt } from '@/api/receivables';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { humanize, today } from '@/utils/format';
import { applicationSummary, receiptActions } from './receivablesMath';

type Dialog = 'reject' | 'cancel' | 'bounce' | null;

/**
 * Workflow actions of a receipt: approve / reject (checker, not the maker), apply money on account
 * (FIFO), cancel or record a bounced cheque (reversal with a reason), print.
 */
export function ReceiptActions({ receipt }: Readonly<{ receipt: Receipt }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const { user, can } = useAuth();
  const [dialog, setDialog] = useState<Dialog>(null);
  const [reason, setReason] = useState('');
  const [date, setDate] = useState(today());
  const r = receipt.summary;
  const allowed = receiptActions(r, user?.username, can);

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['receipts'] });
    await queryClient.invalidateQueries({ queryKey: ['receipt', r.id] });
    await queryClient.invalidateQueries({ queryKey: ['open-items'] });
  };
  const action = useMutation({
    mutationFn: (run: () => Promise<Receipt>) => run(),
    onSuccess: async (result) => {
      setDialog(null);
      setReason('');
      await refresh();
      toast.success(`${result.summary.receiptNo}: ${humanize(result.summary.status)}`);
    },
  });
  const apply = useMutation({
    mutationFn: () => receivablesApi.applyReceipt(r.id, today(), 'FIFO', []),
    onSuccess: async (result) => {
      await refresh();
      toast.success(applicationSummary(receipt, result));
    },
  });
  const confirm = () =>
    action.mutate(() => {
      if (dialog === 'reject') {
        return receivablesApi.rejectReceipt(r.id, date, reason);
      }
      return dialog === 'bounce'
        ? receivablesApi.bounceReceipt(r.id, date, reason)
        : receivablesApi.cancelReceipt(r.id, date, reason);
    });

  return (
    <>
      {allowed.approve && (
        <Button
          variant="accent"
          icon={<CheckCircle2 size={16} />}
          busy={action.isPending}
          onClick={() => action.mutate(() => receivablesApi.approveReceipt(r.id))}
        >
          Approve
        </Button>
      )}
      {allowed.reject && (
        <Button variant="danger" icon={<XCircle size={16} />} onClick={() => setDialog('reject')}>
          Reject
        </Button>
      )}
      {allowed.apply && (
        <Button
          variant="secondary"
          icon={<CornerDownRight size={16} />}
          busy={apply.isPending}
          onClick={() => apply.mutate()}
        >
          Apply On-account (FIFO)
        </Button>
      )}
      {allowed.cancel && (
        <Button variant="secondary" icon={<Undo2 size={16} />} onClick={() => setDialog('cancel')}>
          Cancel Receipt
        </Button>
      )}
      {allowed.bounce && (
        <Button variant="danger" icon={<Ban size={16} />} onClick={() => setDialog('bounce')}>
          Cheque Bounced
        </Button>
      )}
      <Button variant="ghost" icon={<Printer size={16} />} onClick={() => window.print()}>
        Print
      </Button>
      {dialog === null && <ErrorAlert error={action.error ?? apply.error} />}
      <Modal
        title={dialog === null ? '' : `${humanize(dialog)} ${r.receiptNo}`}
        open={dialog !== null}
        onClose={() => setDialog(null)}
        footer={
          <Button
            variant="danger"
            busy={action.isPending}
            disabled={reason.trim() === ''}
            onClick={confirm}
          >
            Confirm
          </Button>
        }
      >
        <div className="stack">
          <ErrorAlert error={action.error} />
          {dialog !== 'reject' && (
            <p className="muted">
              A reversal journal is posted on this date and the debit notes settled by the receipt
              are re-opened.
            </p>
          )}
          <Field label="Date" required>
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={date}
                onChange={(e) => setDate(e.target.value)}
              />
            )}
          </Field>
          <Field label="Reason" required>
            {(id) => (
              <input
                id={id}
                className="input"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            )}
          </Field>
        </div>
      </Modal>
    </>
  );
}

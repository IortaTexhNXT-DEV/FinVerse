import { useMutation, useQueryClient } from '@tanstack/react-query';
import { FilePlus2, Undo2, Unlock } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';
import { disbursementApi } from './api';
import type { PaymentRequest } from './api';
import { requestActions } from './labels';
import './disbursement.css';

type RequestDialogMode = 'details' | 'return';

/**
 * A payment request (DIS 2.6.2, 3.25.0): its facts, and the processor's actions: create the
 * voucher, return it to the source with a reason, or release BIR 2307 documents.
 */
export function RequestDialog({
  request,
  canProcess,
  onClose,
}: Readonly<{ request: PaymentRequest; canProcess: boolean; onClose: () => void }>) {
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [mode, setMode] = useState<RequestDialogMode>('details');
  const done = async (message: string) => {
    await queryClient.invalidateQueries({ queryKey: ['disbursement'] });
    toast.success(message);
    onClose();
  };
  const voucher = useMutation({
    mutationFn: () => disbursementApi.createVoucher(request.id),
    onSuccess: (r) => {
      toast.success(`Voucher created for ${r.requestNo}`);
      void queryClient.invalidateQueries({ queryKey: ['disbursement'] });
      if (r.voucherId !== undefined) {
        void navigate(`/disbursement/vouchers/${r.voucherId}`);
      }
    },
  });
  const release = useMutation({
    mutationFn: () => disbursementApi.releaseRequest(request.id),
    onSuccess: () => done(`${request.requestNo} released`),
  });
  const back = useMutation({
    mutationFn: (note: { reasonCode?: string; comment?: string }) =>
      disbursementApi.returnRequest(request.id, note.reasonCode ?? '', note.comment),
    onSuccess: () => done(`${request.requestNo} returned to ${humanize(request.sourceModule)}`),
  });
  if (mode === 'return') {
    return (
      <ActionDialog
        title={`Return ${request.requestNo}`}
        reasonLov="DISB_RETURN_REASON"
        confirmLabel="Return to Source"
        busy={back.isPending}
        error={back.error}
        onClose={onClose}
        onConfirm={(note) => back.mutate(note)}
      />
    );
  }
  const actions = canProcess ? requestActions(request.status, request.disbursementType) : [];
  return (
    <Modal
      open
      title={`Payment Request ${request.requestNo}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
          {actions.includes('return') && (
            <Button
              variant="secondary"
              icon={<Undo2 size={16} />}
              onClick={() => setMode('return')}
            >
              Return to Source
            </Button>
          )}
          {actions.includes('release') && (
            <Button
              icon={<Unlock size={16} />}
              busy={release.isPending}
              onClick={() => release.mutate()}
            >
              Release Documents
            </Button>
          )}
          {actions.includes('voucher') && (
            <Button
              icon={<FilePlus2 size={16} />}
              busy={voucher.isPending}
              onClick={() => voucher.mutate()}
            >
              Create Voucher
            </Button>
          )}
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={voucher.error ?? release.error} />
        <dl className="detail-list">
          <dt>Source</dt>
          <dd>
            {humanize(request.sourceModule)} · {request.sourceRef}
          </dd>
          <dt>RFP No.</dt>
          <dd>{request.rfpNo ?? '—'}</dd>
          <dt>Payee</dt>
          <dd>
            {request.payeeCode} {request.payeeName ?? ''}
          </dd>
          <dt>Type</dt>
          <dd>{humanize(request.disbursementType)}</dd>
          <dt>Amount</dt>
          <dd>
            {request.currency} <Amount value={request.amount} />
          </dd>
          <dt>Purpose</dt>
          <dd>{request.purpose ?? '—'}</dd>
          <dt>Root invoice</dt>
          <dd>{request.rootInvoiceNo ?? '—'}</dd>
          <dt>Status</dt>
          <dd>
            <StatusBadge status={request.status} /> {request.statusReason ?? ''}
          </dd>
        </dl>
      </div>
    </Modal>
  );
}

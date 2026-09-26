import { useMutation } from '@tanstack/react-query';
import { CheckCircle2, XCircle } from 'lucide-react';
import { useState } from 'react';
import { claimsApi } from '@/api/claims';
import type { Approval, ClaimDocumentKind } from '@/api/claims';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';
import { canDecide } from './claimWorkflow';

type Mode = 'approve' | 'reject' | null;

interface Props {
  kind: ClaimDocumentKind;
  documentId: number;
  label: string;
  approval: Approval;
  onDone: () => Promise<void>;
}

/**
 * Checker actions on a pending claim document (reserve change, settlement, recovery): approve with
 * an accounting date, or reject with a reason. Hidden for the maker and for users without
 * CLAIM_AUTHORIZE; the server also enforces the authorization limit.
 */
export function DocumentActions({ kind, documentId, label, approval, onDone }: Readonly<Props>) {
  const { user, can } = useAuth();
  const toast = useToast();
  const [mode, setMode] = useState<Mode>(null);
  const [reason, setReason] = useState('');
  const [date, setDate] = useState(today());
  const decide = useMutation({
    mutationFn: () =>
      mode === 'approve'
        ? claimsApi.approve(kind, documentId, date)
        : claimsApi.reject(kind, documentId, reason),
    onSuccess: async () => {
      toast.success(`${label} ${mode === 'approve' ? 'approved and posted' : 'rejected'}`);
      setMode(null);
      setReason('');
      await onDone();
    },
  });
  if (!canDecide(approval, user?.username, can)) {
    return null;
  }
  const approving = mode === 'approve';
  return (
    <div className="row">
      <Button
        size="sm"
        variant="accent"
        icon={<CheckCircle2 size={14} />}
        onClick={() => setMode('approve')}
      >
        Approve
      </Button>
      <Button
        size="sm"
        variant="danger"
        icon={<XCircle size={14} />}
        onClick={() => setMode('reject')}
      >
        Reject
      </Button>
      <Modal
        title={approving ? `Approve ${label}` : `Reject ${label}`}
        open={mode !== null}
        onClose={() => setMode(null)}
        footer={
          <Button
            variant={approving ? 'accent' : 'danger'}
            disabled={!approving && reason.trim() === ''}
            busy={decide.isPending}
            onClick={() => decide.mutate()}
          >
            Confirm
          </Button>
        }
      >
        <div className="stack">
          <ErrorAlert error={decide.error} />
          {approving ? (
            <Field label="Accounting date" required hint="The movement is posted on this date">
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type="date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                />
              )}
            </Field>
          ) : (
            <Field label="Reason" required>
              {(id) => (
                <textarea
                  id={id}
                  className="textarea"
                  maxLength={200}
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                />
              )}
            </Field>
          )}
        </div>
      </Modal>
    </div>
  );
}

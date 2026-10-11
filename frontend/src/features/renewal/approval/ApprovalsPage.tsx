import { CheckCheck, Send, Tag, Undo2 } from 'lucide-react';
import { useState } from 'react';
import { renewalApprovalApi } from '@/api/renewalApproval';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { CandidateList } from '../common/CandidateList';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import { useBatchAction } from '../common/useBatchAction';
import { useListDialogs } from '../common/useListDialogs';
import '../renewal.css';

type Open = 'approve' | 'return';

function ApproveDialog({
  count,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<{
  count: number;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onConfirm: (confirmation: string) => void;
}>) {
  const [confirmation, setConfirmation] = useState('');
  return (
    <Modal
      open
      title={`Approve (${String(count)})`}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={() => onConfirm(confirmation)}>
            Approve
          </Button>
        </>
      }
    >
      <ErrorAlert error={error} />
      <p className="muted">
        A CBG Motor account proceeds to placement once its payment is posted; a Non-CBG account
        needs the client&apos;s payment confirmation.
      </p>
      <Field label="Client payment confirmation">
        {(id) => (
          <LovSelect
            id={id}
            type="CLIENT_CONFIRMATION_CHANNEL"
            value={confirmation}
            onChange={setConfirmation}
          />
        )}
      </Field>
    </Modal>
  );
}

function ReturnDialog({
  count,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<{
  count: number;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onConfirm: (remarks: string) => void;
}>) {
  const [remarks, setRemarks] = useState('');
  return (
    <Modal
      open
      title={`Return for Correction (${String(count)})`}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} disabled={remarks.trim() === ''} onClick={() => onConfirm(remarks)}>
            Return
          </Button>
        </>
      }
    >
      <ErrorAlert error={error} />
      <Field label="Reason or remarks" required>
        {(id) => (
          <textarea
            id={id}
            className="input"
            rows={3}
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}

/**
 * Submitted for Approval (FRRN.26.01, FRRN.014.04): the accepted renewal accounts to review;
 * Approve submits them for placement (with the client's payment confirmation for Non-CBG accounts),
 * Return sends them back for correction, Submit for Placement proceeds once an account For Booking
 * Only is ready or overridden, and accounts can be tagged For Booking Only.
 */
export default function ApprovalsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const { open, refs, close, done, show } = useListDialogs<Open>();
  const approve = useBatchAction<string>(
    'Approve',
    'approved and submitted for Placement',
    (confirmation) => renewalApprovalApi.approve(companyId, refs, confirmation),
    done,
  );
  const reject = useBatchAction<string>(
    'Return for Correction',
    'returned for correction',
    (remarks) => renewalApprovalApi.reject(companyId, refs, remarks),
    done,
  );
  const submit = useBatchAction<string[]>('Submit for Placement', 'submitted for placement', (r) =>
    renewalApprovalApi.submitForPlacement(companyId, r),
  );
  const tag = useBatchAction<string[]>('For Booking Only', 'tagged For Booking Only', (r) =>
    renewalApprovalApi.tagBookingOnly(companyId, r, true),
  );
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Submitted for Approval"
        description="Accepted renewal accounts to review before their placement."
      />
      <CandidateList
        base={{ stage: 'ACCEPTED' }}
        emptyMessage="No renewal account is Submitted for Approval"
        actions={(selected, selection) => {
          const none = selected.length === 0;
          return (
            <span className="rnw-actions">
              {(can('RNW_REVIEW') || can('RNW_PROCESS_ASSIGN')) && (
                <>
                  <Button
                    icon={<CheckCheck size={16} />}
                    disabled={none}
                    onClick={() => show('approve', selected, selection.clear)}
                  >
                    Approve
                  </Button>
                  <Button
                    variant="secondary"
                    icon={<Undo2 size={16} />}
                    disabled={none}
                    onClick={() => show('return', selected, selection.clear)}
                  >
                    Return
                  </Button>
                </>
              )}
              {can('RNW_DISPOSE') && (
                <>
                  <Button
                    variant="secondary"
                    icon={<Send size={16} />}
                    disabled={none}
                    busy={submit.mutation.isPending}
                    onClick={() => submit.mutation.mutate(selected, { onSuccess: selection.clear })}
                  >
                    Submit for Placement
                  </Button>
                  <Button
                    variant="ghost"
                    icon={<Tag size={16} />}
                    disabled={none}
                    busy={tag.mutation.isPending}
                    onClick={() => tag.mutation.mutate(selected, { onSuccess: selection.clear })}
                  >
                    Tag For Booking Only
                  </Button>
                </>
              )}
            </span>
          );
        }}
      />
      {open === 'approve' && (
        <ApproveDialog
          count={refs.length}
          busy={approve.mutation.isPending}
          error={approve.mutation.error}
          onClose={close}
          onConfirm={(c) => approve.mutation.mutate(c)}
        />
      )}
      {open === 'return' && (
        <ReturnDialog
          count={refs.length}
          busy={reject.mutation.isPending}
          error={reject.mutation.error}
          onClose={close}
          onConfirm={(r) => reject.mutation.mutate(r)}
        />
      )}
      {approve.dialog}
      {reject.dialog}
      {submit.dialog}
      {tag.dialog}
    </div>
  );
}

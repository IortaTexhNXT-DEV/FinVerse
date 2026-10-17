import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { renewalReferralsApi } from '@/api/renewalReferrals';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useCompanyId } from '@/context/workspaceContext';
import { countOf } from '@/utils/format';

/**
 * Submit for Posting (FRRN.016.01): the officer picks the Team Leader who approves the posting;
 * without one, every Team Leader of the unit sees the account.
 */
export function SubmitPostingDialog({
  refs,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<{
  refs: string[];
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onConfirm: (approver: string | undefined) => void;
}>) {
  const companyId = useCompanyId();
  const [approver, setApprover] = useState('');
  const options = useQuery({
    queryKey: ['renewal', 'posting-approvers', companyId, refs.length === 1 ? refs[0] : ''],
    queryFn: () =>
      renewalReferralsApi.postingApprovers(companyId, refs.length === 1 ? refs[0] : undefined),
  });
  const mode = options.data?.mode ?? 'OPTIONAL';
  const required = mode === 'REQUIRED';
  return (
    <Modal
      open
      size="md"
      title="Submit for Posting"
      onClose={onClose}
      helper={`${countOf(refs.length, 'renewal account')} to submit for posting.`}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={busy}
            disabled={required && approver === ''}
            onClick={() => onConfirm(approver === '' ? undefined : approver)}
          >
            Submit for Posting
          </Button>
        </>
      }
    >
      <ErrorAlert error={error ?? options.error} />
      {mode !== 'NONE' && (
        <Field label="Approver" required={required}>
          {(id) => (
            <select
              id={id}
              className="input"
              value={approver}
              onChange={(e) => setApprover(e.target.value)}
            >
              <option value="">
                {required ? 'Select the approver' : 'Every Team Leader of the unit'}
              </option>
              {(options.data?.approvers ?? []).map((a) => (
                <option key={a.username} value={a.username}>
                  {a.fullName}
                </option>
              ))}
            </select>
          )}
        </Field>
      )}
    </Modal>
  );
}

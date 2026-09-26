import { useMutation, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, RefreshCw, XCircle } from 'lucide-react';
import { useState } from 'react';
import { issuanceApi } from '@/api/issuance';
import type { Review } from '@/api/issuance';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { numbersError, proposedNumbers } from './issuanceLogic';

/**
 * Confirmation of the extracted policy data (BRNB.074/104): one policy number per policy year
 * (BRNB.112) and the issue date; confirming records them on the account (policy issued). The
 * document can be extracted again or rejected with a reason.
 */
export function ConfirmPolicyCard({ review }: Readonly<{ review: Review }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [numbers, setNumbers] = useState(() => proposedNumbers(review));
  const [issueDate, setIssueDate] = useState('');
  const [rejecting, setRejecting] = useState(false);
  const id = review.epolicy.id;
  const store = (next: Review) => {
    queryClient.setQueryData(['issuance', 'review', id], next);
    void queryClient.invalidateQueries({ queryKey: ['issuance'] });
  };
  const confirm = useMutation({
    mutationFn: () =>
      issuanceApi.confirm(
        id,
        numbers.map((n) => n.trim()),
        issueDate || undefined,
      ),
    onSuccess: (next) => {
      store(next);
      toast.success(
        `Policy ${next.epolicy.policyNumbers.join(', ')} recorded on ${next.account.arn}`,
      );
    },
  });
  const extract = useMutation({
    mutationFn: () => issuanceApi.extract(id),
    onSuccess: (next) => {
      store(next);
      setNumbers(proposedNumbers(next));
      toast.success('Policy data extracted again');
    },
  });
  const reject = useMutation({
    mutationFn: ({ reason, remarks }: { reason: string; remarks?: string }) =>
      issuanceApi.reject(id, reason, remarks),
    onSuccess: () => {
      setRejecting(false);
      void queryClient.invalidateQueries({ queryKey: ['issuance'] });
      toast.success('E-policy rejected');
    },
  });
  const error = numbersError(numbers);
  const years = numbers.map((value, index) => ({ year: index + 1, value }));
  return (
    <Card
      title="Confirm Policy"
      actions={
        <span className="row">
          <Button
            variant="ghost"
            icon={<RefreshCw size={16} />}
            busy={extract.isPending}
            onClick={() => extract.mutate()}
          >
            Extract Again
          </Button>
          <Button
            variant="secondary"
            icon={<XCircle size={16} />}
            onClick={() => setRejecting(true)}
          >
            Reject
          </Button>
        </span>
      }
    >
      <div className="stack">
        <ErrorAlert error={confirm.error ?? extract.error} />
        <div className="form-grid">
          {years.map((y) => (
            <Field
              key={`year-${y.year}`}
              label={years.length > 1 ? `Policy number, year ${y.year}` : 'Policy number'}
              required
            >
              {(fid) => (
                <input
                  id={fid}
                  className="input"
                  maxLength={60}
                  value={y.value}
                  onChange={(e) =>
                    setNumbers(numbers.map((x, j) => (j === y.year - 1 ? e.target.value : x)))
                  }
                />
              )}
            </Field>
          ))}
          <Field label="Issue date" hint="Today when left empty.">
            {(fid) => (
              <input
                id={fid}
                type="date"
                className="input"
                value={issueDate}
                onChange={(e) => setIssueDate(e.target.value)}
              />
            )}
          </Field>
        </div>
        {error !== undefined && <span className="field-error">{error}</span>}
        <div className="row">
          <span className="spacer" />
          <Button
            variant="primary"
            icon={<CheckCircle2 size={16} />}
            busy={confirm.isPending}
            disabled={error !== undefined}
            onClick={() => confirm.mutate()}
          >
            Confirm Policy
          </Button>
        </div>
      </div>
      {rejecting && (
        <ActionDialog
          title="Reject E-policy"
          reasonLov="EPOLICY_REJECT_REASON"
          confirmLabel="Reject"
          busy={reject.isPending}
          error={reject.error}
          onClose={() => setRejecting(false)}
          onConfirm={(note) =>
            reject.mutate({ reason: note.reasonCode ?? '', remarks: note.comment })
          }
        />
      )}
    </Card>
  );
}

import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { claimsApi } from '@/api/claims';
import type { Claim } from '@/api/claims';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';
import type { ClaimActions } from './claimWorkflow';

type Decision = 'close' | 'reopen' | 'repudiate' | 'withdraw';

const TITLES: Record<Decision, string> = {
  close: 'Close claim',
  reopen: 'Reopen claim',
  repudiate: 'Repudiate claim',
  withdraw: 'Record withdrawal',
};

const HINTS: Record<Decision, string> = {
  close: 'The outstanding reserve is released on the accounting date.',
  reopen: 'Request a new reserve afterwards; it needs approval.',
  repudiate: 'Only claims without settlements; the reserve is released.',
  withdraw: 'Only claims without settlements; the reserve is released.',
};

function run(claim: Claim, decision: Decision, reason: string, date: string) {
  switch (decision) {
    case 'close':
      return claimsApi.close(claim.id, { reason, accountingDate: date });
    case 'reopen':
      return claimsApi.reopen(claim.id, reason);
    case 'repudiate':
      return claimsApi.repudiate(claim.id, { reason, accountingDate: date });
    default:
      return claimsApi.withdraw(claim.id, { reason, accountingDate: date });
  }
}

/** Checker decisions on a claim: close, reopen, repudiate (rejected) and withdrawal. */
export function ClaimDecisions({
  claim,
  actions,
  onChange,
}: Readonly<{ claim: Claim; actions: ClaimActions; onChange: () => Promise<void> }>) {
  const toast = useToast();
  const [decision, setDecision] = useState<Decision | null>(null);
  const [reason, setReason] = useState('');
  const [date, setDate] = useState(today());
  const decide = useMutation({
    mutationFn: (d: Decision) => run(claim, d, reason, date),
    onSuccess: async (updated) => {
      toast.success(`${updated.claimNo}: ${updated.status.toLowerCase().replace('_', ' ')}`);
      setDecision(null);
      setReason('');
      await onChange();
    },
  });
  const offered: [Decision, boolean][] = [
    ['close', actions.close],
    ['reopen', actions.reopen],
    ['repudiate', actions.decline],
    ['withdraw', actions.decline],
  ];
  return (
    <>
      {offered
        .filter(([, allowed]) => allowed)
        .map(([d]) => (
          <Button key={d} variant="secondary" onClick={() => setDecision(d)}>
            {TITLES[d]}
          </Button>
        ))}
      {decision !== null && (
        <Modal
          title={`${TITLES[decision]} ${claim.claimNo}`}
          open
          onClose={() => setDecision(null)}
          footer={
            <Button
              variant={decision === 'reopen' ? 'accent' : 'danger'}
              disabled={reason.trim() === ''}
              busy={decide.isPending}
              onClick={() => decide.mutate(decision)}
            >
              Confirm
            </Button>
          }
        >
          <div className="stack">
            <ErrorAlert error={decide.error} />
            <p className="muted" style={{ margin: 0 }}>
              {HINTS[decision]}
            </p>
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
            {decision !== 'reopen' && (
              <Field label="Accounting date" required>
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
            )}
          </div>
        </Modal>
      )}
    </>
  );
}

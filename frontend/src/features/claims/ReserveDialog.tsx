import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { claimsApi } from '@/api/claims';
import type { Claim, CostType, EstimateSide } from '@/api/claims';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { NumberField, SelectField, TextField } from '@/features/underwriting/FormFields';
import { currentEstimate, ourShare } from './claimMath';

interface Props {
  claim: Claim;
  open: boolean;
  onClose: () => void;
  onSaved: () => Promise<void>;
}

const SIDES = [
  { value: 'PAYMENT', label: 'Payment (indemnity / expense)' },
  { value: 'RECOVERY', label: 'Recovery (salvage / subrogation)' },
];
const COSTS = [
  { value: 'LOSS', label: 'Loss' },
  { value: 'EXPENSE', label: 'Expense' },
];

/**
 * Maker requests a new estimate (100 %) for one side and cost type; a checker approves it within
 * their authorization limit.
 */
export function ReserveDialog({ claim, open, onClose, onSaved }: Readonly<Props>) {
  const toast = useToast();
  const [side, setSide] = useState<EstimateSide>('PAYMENT');
  const [cost, setCost] = useState<CostType>('LOSS');
  const [estimate, setEstimate] = useState<number | undefined>(undefined);
  const [reason, setReason] = useState('');
  const effectiveCost: CostType = side === 'RECOVERY' ? 'LOSS' : cost;
  const current = currentEstimate(claim, side, effectiveCost);
  const save = useMutation({
    mutationFn: () =>
      claimsApi.requestReserve(claim.id, {
        side,
        costType: effectiveCost,
        newEstimate: estimate ?? 0,
        reason,
      }),
    onSuccess: async (rc) => {
      toast.success(`Reserve change ${String(rc.changeNo)} submitted for approval`);
      setEstimate(undefined);
      setReason('');
      onClose();
      await onSaved();
    },
  });
  const valid =
    estimate !== undefined && estimate >= 0 && estimate !== current && reason.trim() !== '';
  return (
    <Modal
      title={`Change reserve – ${claim.claimNo}`}
      open={open}
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          disabled={!valid}
          busy={save.isPending}
          onClick={() => save.mutate()}
        >
          Submit for Approval
        </Button>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="form-grid">
          <SelectField
            label="Estimate"
            value={side}
            options={SIDES}
            onChange={(v) => setSide(v as EstimateSide)}
          />
          <SelectField
            label="Cost type"
            value={effectiveCost}
            disabled={side === 'RECOVERY'}
            options={COSTS}
            onChange={(v) => setCost(v as CostType)}
          />
          <NumberField
            label={`New estimate (100 %, ${claim.currency})`}
            required
            value={estimate}
            onChange={setEstimate}
          />
          <TextField label="Reason" required value={reason} onChange={setReason} />
        </div>
        <p className="muted" style={{ margin: 0 }}>
          Current estimate <Amount value={current} /> · new company share{' '}
          <Amount value={ourShare(estimate ?? current, claim.sharePct)} />
        </p>
      </div>
    </Modal>
  );
}

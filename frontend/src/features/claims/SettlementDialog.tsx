import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { claimsApi } from '@/api/claims';
import type { Claim, CostType, SettlementInput, SettlementType } from '@/api/claims';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { NumberField, SelectField, TextField } from '@/features/underwriting/FormFields';
import { outstandingOf, settlementNet, settlementSplit } from './claimMath';
import { partyOption, useClaimLookups } from './useClaimLookups';

interface Props {
  claim: Claim;
  open: boolean;
  onClose: () => void;
  onSaved: () => Promise<void>;
}

const EMPTY: SettlementInput = {
  payeeCode: '',
  costType: 'LOSS',
  settlementType: 'PARTIAL',
  assessedAmount: 0,
  narration: '',
};

/**
 * Maker enters a settlement at 100 %: net = assessed − deductible − excess, within the outstanding
 * reserve of its cost type. The preview shows the company share, the amount payable to the payee
 * and, when leading a coinsurance, the coinsurers' share recovered.
 */
export function SettlementDialog({ claim, open, onClose, onSaved }: Readonly<Props>) {
  const toast = useToast();
  const lookups = useClaimLookups();
  const [input, setInput] = useState<SettlementInput>(EMPTY);
  const set = (patch: Partial<SettlementInput>) => setInput((i) => ({ ...i, ...patch }));
  const net = settlementNet(input.assessedAmount, input.deductible, input.excess);
  const split = settlementSplit(net, claim.sharePct, claim.coinsuranceLeader);
  const available = outstandingOf(claim, 'PAYMENT', input.costType);
  const save = useMutation({
    mutationFn: () => claimsApi.createSettlement(claim.id, input),
    onSuccess: async (s) => {
      toast.success(`Settlement ${s.settlementNo} submitted for approval`);
      setInput(EMPTY);
      onClose();
      await onSaved();
    },
  });
  const valid =
    input.payeeCode !== '' && net > 0 && net <= available && input.narration.trim() !== '';
  return (
    <Modal
      title={`Settlement – ${claim.claimNo}`}
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
            label="Payee"
            required
            value={input.payeeCode}
            emptyLabel="Select…"
            options={lookups.payees.map(partyOption)}
            onChange={(v) => set({ payeeCode: v })}
          />
          <SelectField
            label="Cost type"
            value={input.costType}
            options={[
              { value: 'LOSS', label: 'Loss (indemnity)' },
              { value: 'EXPENSE', label: 'Expense (fees)' },
            ]}
            onChange={(v) => set({ costType: v as CostType })}
          />
          <SelectField
            label="Type"
            value={input.settlementType}
            options={[
              { value: 'PARTIAL', label: 'Partial' },
              { value: 'FINAL', label: 'Final (closes the claim)' },
            ]}
            onChange={(v) => set({ settlementType: v as SettlementType })}
          />
          <NumberField
            label={`Assessed amount (100 %, ${claim.currency})`}
            required
            value={input.assessedAmount}
            onChange={(v) => set({ assessedAmount: v ?? 0 })}
          />
          <NumberField
            label="Deductible"
            value={input.deductible}
            onChange={(v) => set({ deductible: v })}
          />
          <NumberField label="Excess" value={input.excess} onChange={(v) => set({ excess: v })} />
          <TextField
            label="Narration"
            required
            value={input.narration}
            onChange={(v) => set({ narration: v })}
          />
        </div>
        <p className="muted" style={{ margin: 0 }}>
          Net <Amount value={net} /> of outstanding <Amount value={available} /> · company share{' '}
          <Amount value={split.ours} /> · payable to payee <Amount value={split.payable} />
          {split.coinsurers > 0 && (
            <>
              {' '}
              · coinsurers&apos; share <Amount value={split.coinsurers} />
            </>
          )}
        </p>
      </div>
    </Modal>
  );
}

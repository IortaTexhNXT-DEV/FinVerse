import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { claimsApi } from '@/api/claims';
import type { Claim, RecoveryInput, RecoveryType } from '@/api/claims';
import { payablesApi } from '@/api/payables';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { NumberField, SelectField, TextField } from '@/features/underwriting/FormFields';
import { ourShare } from './claimMath';
import { partyOption, useClaimLookups } from './useClaimLookups';

interface Props {
  claim: Claim;
  open: boolean;
  onClose: () => void;
  onSaved: () => Promise<void>;
}

const EMPTY: RecoveryInput = {
  recoveryType: 'SALVAGE',
  bankAccountCode: '',
  amount: 0,
  narration: '',
};

/** Maker records salvage or subrogation money received into a bank account (100 %). */
export function RecoveryDialog({ claim, open, onClose, onSaved }: Readonly<Props>) {
  const toast = useToast();
  const lookups = useClaimLookups();
  const [input, setInput] = useState<RecoveryInput>(EMPTY);
  const set = (patch: Partial<RecoveryInput>) => setInput((i) => ({ ...i, ...patch }));
  const banks = useQuery({
    queryKey: ['bank-accounts', claim.companyId],
    queryFn: () => payablesApi.bankAccounts(claim.companyId),
    enabled: open,
    staleTime: 300_000,
  });
  const save = useMutation({
    mutationFn: () => claimsApi.createRecovery(claim.id, input),
    onSuccess: async (r) => {
      toast.success(`Recovery ${r.recoveryNo} submitted for approval`);
      setInput(EMPTY);
      onClose();
      await onSaved();
    },
  });
  const available = claim.totals.recoveryOutstanding;
  const valid =
    input.bankAccountCode !== '' &&
    input.amount > 0 &&
    input.amount <= available &&
    input.narration.trim() !== '';
  return (
    <Modal
      title={`Recovery – ${claim.claimNo}`}
      open={open}
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          disabled={!valid}
          busy={save.isPending}
          onClick={() => save.mutate()}
        >
          Submit for approval
        </Button>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error ?? banks.error} />
        <div className="form-grid">
          <SelectField
            label="Type"
            value={input.recoveryType}
            options={[
              { value: 'SALVAGE', label: 'Salvage' },
              { value: 'SUBROGATION', label: 'Subrogation' },
            ]}
            onChange={(v) => set({ recoveryType: v as RecoveryType })}
          />
          <SelectField
            label="Received from"
            value={input.fromPartyCode}
            emptyLabel="Not specified"
            options={lookups.payers.map(partyOption)}
            onChange={(v) => set({ fromPartyCode: v === '' ? undefined : v })}
          />
          <SelectField
            label="Bank account"
            required
            value={input.bankAccountCode}
            emptyLabel="Select…"
            options={(banks.data ?? [])
              .filter((b) => b.currency === claim.currency)
              .map((b) => ({ value: b.glAccountCode, label: `${b.code} – ${b.name}` }))}
            onChange={(v) => set({ bankAccountCode: v })}
          />
          <NumberField
            label={`Amount received (100 %, ${claim.currency})`}
            required
            value={input.amount}
            onChange={(v) => set({ amount: v ?? 0 })}
          />
          <TextField
            label="Narration"
            required
            value={input.narration}
            onChange={(v) => set({ narration: v })}
          />
        </div>
        <p className="muted" style={{ margin: 0 }}>
          Recovery estimate outstanding <Amount value={available} /> · company share{' '}
          <Amount value={ourShare(input.amount, claim.sharePct)} />
        </p>
      </div>
    </Modal>
  );
}

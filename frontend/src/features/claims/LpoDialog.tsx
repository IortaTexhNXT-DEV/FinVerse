import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { claimsApi } from '@/api/claims';
import type { Claim, LpoCover, LpoInput } from '@/api/claims';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { DateField, NumberField, SelectField, TextField } from '@/features/underwriting/FormFields';
import { today } from '@/utils/format';
import { lpoNet } from './claimMath';
import { partyOption, useClaimLookups } from './useClaimLookups';

interface Props {
  claim: Claim;
  open: boolean;
  onClose: () => void;
  onSaved: () => Promise<void>;
}

/** Issues a local purchase order to a garage for a motor repair (net = gross − discount). */
export function LpoDialog({ claim, open, onClose, onSaved }: Readonly<Props>) {
  const toast = useToast();
  const lookups = useClaimLookups();
  const [input, setInput] = useState<LpoInput>({
    garageCode: '',
    cover: 'OD',
    issueDate: today(),
    gross: 0,
    description: '',
  });
  const set = (patch: Partial<LpoInput>) => setInput((i) => ({ ...i, ...patch }));
  const net = lpoNet(input.gross, input.discount);
  const save = useMutation({
    mutationFn: () => claimsApi.issueLpo(claim.id, input),
    onSuccess: async (lpo) => {
      toast.success(`${lpo.lpoNo} issued to ${lpo.garageName}`);
      onClose();
      await onSaved();
    },
  });
  const valid = input.garageCode !== '' && net > 0 && input.description.trim() !== '';
  return (
    <Modal
      title={`Local purchase order – ${claim.claimNo}`}
      open={open}
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          disabled={!valid}
          busy={save.isPending}
          onClick={() => save.mutate()}
        >
          Issue LPO
        </Button>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="form-grid">
          <SelectField
            label="Garage"
            required
            value={input.garageCode}
            emptyLabel="Select…"
            options={lookups.garages.map(partyOption)}
            onChange={(v) => set({ garageCode: v })}
          />
          <SelectField
            label="Cover"
            value={input.cover}
            options={[
              { value: 'OD', label: 'Own damage' },
              { value: 'TP', label: 'Third party' },
            ]}
            onChange={(v) => set({ cover: v as LpoCover })}
          />
          <DateField
            label="Issue date"
            value={input.issueDate}
            onChange={(v) => set({ issueDate: v })}
          />
          <NumberField
            label={`Gross (${claim.currency})`}
            required
            value={input.gross}
            onChange={(v) => set({ gross: v ?? 0 })}
          />
          <NumberField
            label="Discount"
            value={input.discount}
            onChange={(v) => set({ discount: v })}
          />
          <TextField
            label="Repair"
            required
            value={input.description}
            onChange={(v) => set({ description: v })}
          />
        </div>
        <p className="muted" style={{ margin: 0 }}>
          Net LPO <Amount value={net} />
        </p>
      </div>
    </Modal>
  );
}

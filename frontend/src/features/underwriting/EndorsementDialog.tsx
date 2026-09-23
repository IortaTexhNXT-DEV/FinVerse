import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { underwritingApi } from '@/api/underwriting';
import type { Endorsement, EndorsementInput, EndorsementType, Policy } from '@/api/underwriting';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { formatAmount, today } from '@/utils/format';
import { DateField, NumberField, SelectField, TextField } from './FormFields';
import { cancellationReturn, oneYearFrom } from './premiumMath';

const TYPES: { value: EndorsementType; label: string }[] = [
  { value: 'ADDITIONAL', label: 'Additional premium' },
  { value: 'REFUND', label: 'Refund (return premium)' },
  { value: 'RENEWAL', label: 'Renewal' },
  { value: 'CANCELLATION', label: 'Cancellation (pro-rata 1/365)' },
  { value: 'NIL', label: 'NIL (non-financial)' },
];

function initial(): EndorsementInput {
  return { type: 'ADDITIONAL', issueDate: today(), effectiveDate: today(), description: '' };
}

/** Creates a draft endorsement on an approved policy. */
export function EndorsementDialog({
  policy,
  open,
  onClose,
  onCreated,
}: Readonly<{
  policy: Policy;
  open: boolean;
  onClose: () => void;
  onCreated: (e: Endorsement) => Promise<void>;
}>) {
  const [form, setForm] = useState<EndorsementInput>(initial);
  const set = (patch: Partial<EndorsementInput>) => setForm((f) => ({ ...f, ...patch }));
  const create = useMutation({
    mutationFn: (body: EndorsementInput) => underwritingApi.createEndorsement(policy.id, body),
    onSuccess: async (e) => {
      setForm(initial());
      await onCreated(e);
    },
  });
  const financial = form.type === 'ADDITIONAL' || form.type === 'REFUND';
  const renewal = form.type === 'RENEWAL';
  const selectType = (type: EndorsementType) => {
    const from = new Date(Date.parse(policy.periodTo) + 86_400_000).toISOString().slice(0, 10);
    set(
      type === 'RENEWAL'
        ? { type, effectiveDate: from, newPeriodFrom: from, newPeriodTo: oneYearFrom(from) }
        : { type },
    );
  };

  return (
    <Modal
      title={`Endorse ${policy.policyNo}`}
      open={open}
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          busy={create.isPending}
          disabled={form.description.trim() === ''}
          onClick={() => create.mutate(form)}
        >
          Create draft endorsement
        </Button>
      }
    >
      <div className="stack">
        <ErrorAlert error={create.error} />
        <div className="form-grid">
          <SelectField
            label="Type"
            required
            value={form.type}
            options={TYPES}
            onChange={(v) => selectType(v as EndorsementType)}
          />
          <DateField
            label="Issue date"
            required
            value={form.issueDate}
            onChange={(v) => set({ issueDate: v })}
          />
          <DateField
            label="Effective date"
            required
            value={form.effectiveDate}
            onChange={(v) => set({ effectiveDate: v })}
          />
          {financial && (
            <NumberField
              label="Gross premium (100%)"
              required
              value={form.grossPremium}
              onChange={(v) => set({ grossPremium: v })}
            />
          )}
          {(financial || renewal) && (
            <NumberField
              label={renewal ? 'Sum insured' : 'Sum insured change'}
              value={form.sumInsuredChange}
              onChange={(v) => set({ sumInsuredChange: v })}
            />
          )}
          {renewal && (
            <NumberField
              label="Renewal gross premium"
              hint="Blank = original premium"
              value={form.grossPremium}
              onChange={(v) => set({ grossPremium: v })}
            />
          )}
          {renewal && (
            <DateField
              label="New period from"
              value={form.newPeriodFrom}
              onChange={(v) => set({ newPeriodFrom: v })}
            />
          )}
          {renewal && (
            <DateField
              label="New period to"
              value={form.newPeriodTo}
              onChange={(v) => set({ newPeriodTo: v })}
            />
          )}
        </div>
        <TextField
          label="Description"
          required
          value={form.description}
          onChange={(v) => set({ description: v })}
        />
        {form.type === 'CANCELLATION' && (
          <p className="muted" style={{ margin: 0 }}>
            Estimated return of the original gross premium:{' '}
            {formatAmount(
              cancellationReturn(
                policy.premium.grossPremium,
                policy.periodFrom,
                policy.periodTo,
                form.effectiveDate,
              ),
            )}{' '}
            {policy.currency} (the server includes later endorsements).
          </p>
        )}
      </div>
    </Modal>
  );
}

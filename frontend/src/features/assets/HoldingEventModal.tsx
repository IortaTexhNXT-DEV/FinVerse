import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { investmentsApi } from '@/api/investments';
import type { Holding } from '@/api/investments';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, today } from '@/utils/format';
import { NumberInput, TextInput } from './FormControls';
import { realizedGain } from './investmentMath';

export type HoldingEvent = 'COUPON' | 'MATURITY' | 'SALE' | 'FAIR_VALUE';

const TITLES: Record<HoldingEvent, string> = {
  COUPON: 'Record coupon received',
  MATURITY: 'Record maturity',
  SALE: 'Record sale',
  FAIR_VALUE: 'Update fair value',
};

interface Props {
  holding: Holding;
  event: HoldingEvent;
  onClose: () => void;
}

interface EventForm {
  date?: string;
  amount?: number;
  finalTax?: number;
  remarks?: string;
}

function initialForm(holding: Holding, event: HoldingEvent): EventForm {
  const date = event === 'MATURITY' ? (holding.maturityDate ?? today()) : today();
  return { date, amount: event === 'MATURITY' ? holding.faceValue : undefined, finalTax: 0 };
}

/** Coupon receipt, maturity, sale or fair value update of a held investment. */
export function HoldingEventModal({ holding, event, onClose }: Readonly<Props>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<EventForm>(() => initialForm(holding, event));
  const set = (patch: EventForm) => setForm({ ...form, ...patch });
  const date = form.date ?? today();
  const amount = form.amount ?? 0;
  const tax = form.finalTax ?? 0;
  const submit = useMutation({
    mutationFn: () => {
      const redemption = {
        valueDate: date,
        proceeds: amount,
        finalTax: tax,
        remarks: form.remarks,
      };
      switch (event) {
        case 'COUPON':
          return investmentsApi.coupon(holding.id, {
            receiptDate: date,
            cashAmount: amount,
            finalTax: tax,
            remarks: form.remarks,
          });
        case 'MATURITY':
          return investmentsApi.mature(holding.id, redemption);
        case 'SALE':
          return investmentsApi.sell(holding.id, redemption);
        default:
          return investmentsApi.fairValue(holding.id, {
            valuationDate: date,
            fairValue: amount,
            remarks: form.remarks,
          });
      }
    },
    onSuccess: async (txn) => {
      await queryClient.invalidateQueries({ queryKey: ['holdings'] });
      await queryClient.invalidateQueries({ queryKey: ['holding-transactions'] });
      toast.success(
        `${TITLES[event]}: ${formatAmount(txn.amount)} – journal ${txn.batchNo ?? '—'}`,
      );
      onClose();
    },
  });
  const reserve = holding.classification === 'FVOCI' ? holding.fairValueAdjustment : 0;
  const estimate = realizedGain(
    amount,
    tax,
    holding.carryingAmount,
    holding.accruedInterest,
    reserve,
  );
  const redeeming = event === 'MATURITY' || event === 'SALE';

  return (
    <Modal
      title={`${TITLES[event]} – ${holding.holdingNo}`}
      open
      onClose={onClose}
      footer={
        <Button variant="accent" busy={submit.isPending} onClick={() => submit.mutate()}>
          Post
        </Button>
      }
    >
      <ErrorAlert error={submit.error} />
      <p className="muted">
        {holding.description} – carrying <Amount value={holding.carryingAmount} />, accrued interest{' '}
        <Amount value={holding.accruedInterest} />. Interest (and amortization) is first brought up
        to the date entered.
      </p>
      <div className="form-grid">
        <TextInput
          label="Value date"
          type="date"
          required
          value={form.date}
          onChange={(d) => set({ date: d })}
        />
        <NumberInput
          label={event === 'FAIR_VALUE' ? 'Fair value (clean)' : 'Cash received (net of tax)'}
          required
          hint={redeeming ? `Estimated gain / (loss) ${formatAmount(estimate)}` : undefined}
          value={form.amount}
          onChange={(v) => set({ amount: v })}
        />
        {event !== 'FAIR_VALUE' && (
          <NumberInput
            label="Final tax withheld"
            value={form.finalTax}
            onChange={(v) => set({ finalTax: v })}
          />
        )}
        <TextInput label="Remarks" value={form.remarks} onChange={(remarks) => set({ remarks })} />
      </div>
    </Modal>
  );
}

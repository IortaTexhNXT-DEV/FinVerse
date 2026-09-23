import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { investmentsApi, COUPON_FREQUENCIES, INSTRUMENT_TYPES } from '@/api/investments';
import type { CouponFrequency, DayCount, HoldingInput, InstrumentType } from '@/api/investments';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount } from '@/utils/format';
import { NumberInput, SelectInput, TextInput } from './FormControls';
import { discountOrPremium, validateHolding } from './investmentMath';
import { codeOptions, enumOptions } from './options';
import { useAssetLookups } from './useAssetLookups';

export type HoldingForm = Partial<HoldingInput> & { id?: number };

interface Props {
  initial: HoldingForm;
  onClose: () => void;
}

const DAY_COUNTS = [
  { value: 'ACT_365', label: 'Actual/365' },
  { value: 'THIRTY_360', label: '30E/360' },
];
const METHODS = enumOptions(['EFFECTIVE_INTEREST', 'STRAIGHT_LINE', 'NONE']);

/** Capture (or edit, while pending) an investment holding; a checker approves and posts it. */
export function HoldingFormModal({ initial, onClose }: Readonly<Props>) {
  const lookups = useAssetLookups();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<HoldingForm>(initial);
  const [touched, setTouched] = useState(false);
  const set = (patch: HoldingForm) => setForm({ ...form, ...patch });
  const errors = touched ? validateHolding(form) : {};
  const equity = form.instrumentType === 'EQUITY';
  const difference = discountOrPremium(form.faceValue ?? 0, form.purchasePrice ?? 0);
  const save = useMutation({
    mutationFn: (f: HoldingForm) => {
      const body = { ...f, companyId: lookups.companyId } as HoldingInput;
      return f.id === undefined ? investmentsApi.create(body) : investmentsApi.update(f.id, body);
    },
    onSuccess: async (h) => {
      await queryClient.invalidateQueries({ queryKey: ['holdings'] });
      toast.success(`Holding ${h.holdingNo} saved – pending approval`);
      onClose();
    },
  });
  const submit = () => {
    setTouched(true);
    if (Object.keys(validateHolding(form)).length === 0) {
      save.mutate(form);
    }
  };

  return (
    <Modal
      title={form.id === undefined ? 'New investment' : `Edit investment`}
      open
      onClose={onClose}
      footer={
        <Button variant="accent" busy={save.isPending} onClick={submit}>
          Save for approval
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <SelectInput
          label="Portfolio"
          required
          blank="Select"
          error={errors.portfolioId}
          value={form.portfolioId}
          options={lookups.activePortfolios.map((p) => ({ value: String(p.id), label: p.name }))}
          onChange={(v) => set({ portfolioId: Number(v) })}
        />
        <SelectInput
          label="Instrument"
          required
          value={form.instrumentType}
          options={enumOptions(INSTRUMENT_TYPES)}
          onChange={(v) => set({ instrumentType: v as InstrumentType })}
        />
        <TextInput
          label="Description"
          required
          error={errors.description}
          value={form.description}
          onChange={(description) => set({ description })}
        />
        <TextInput
          label="Security code / ISIN"
          value={form.securityCode}
          onChange={(securityCode) => set({ securityCode })}
        />
        <SelectInput
          label="Issuer / bank"
          required
          blank="Select"
          error={errors.issuerCode}
          value={form.issuerCode}
          options={codeOptions(lookups.issuers)}
          onChange={(issuerCode) => set({ issuerCode })}
        />
        <TextInput
          label="Custodian"
          value={form.custodian}
          onChange={(custodian) => set({ custodian })}
        />
        <NumberInput
          label="Face value"
          required
          error={errors.faceValue}
          value={form.faceValue}
          onChange={(faceValue) => set({ faceValue })}
        />
        <NumberInput
          label="Purchase price (clean)"
          required
          error={errors.purchasePrice}
          hint={`${difference >= 0 ? 'Discount' : 'Premium'} ${formatAmount(Math.abs(difference))}`}
          value={form.purchasePrice}
          onChange={(purchasePrice) => set({ purchasePrice })}
        />
        <TextInput
          label="Trade date"
          type="date"
          required
          value={form.tradeDate}
          onChange={(tradeDate) => set({ tradeDate })}
        />
        <TextInput
          label="Settlement date"
          type="date"
          required
          error={errors.settlementDate}
          value={form.settlementDate}
          onChange={(settlementDate) => set({ settlementDate })}
        />
        {!equity && (
          <>
            <TextInput
              label="Maturity date"
              type="date"
              required
              error={errors.maturityDate}
              value={form.maturityDate}
              onChange={(maturityDate) => set({ maturityDate })}
            />
            <NumberInput
              label="Coupon rate % p.a."
              required
              step="0.0001"
              value={form.couponRate}
              onChange={(couponRate) => set({ couponRate: couponRate ?? 0 })}
            />
            <SelectInput
              label="Coupon frequency"
              required
              value={form.couponFrequency}
              options={enumOptions(COUPON_FREQUENCIES)}
              onChange={(v) => set({ couponFrequency: v as CouponFrequency })}
            />
            <SelectInput
              label="Day count"
              required
              value={form.dayCount}
              options={DAY_COUNTS}
              onChange={(v) => set({ dayCount: v as DayCount })}
            />
            <SelectInput
              label="Amortization"
              blank="Default (effective interest; none at par)"
              value={form.amortizationMethod}
              options={METHODS}
              onChange={(v) =>
                set({
                  amortizationMethod:
                    v === '' ? undefined : (v as HoldingInput['amortizationMethod']),
                })
              }
            />
          </>
        )}
        <SelectInput
          label="Settlement bank account"
          required
          blank="Select"
          error={errors.bankAccount}
          value={form.bankAccount}
          options={lookups.bankAccounts.map((a) => ({
            value: a.code,
            label: `${a.code} – ${a.name}`,
          }))}
          onChange={(bankAccount) => set({ bankAccount })}
        />
        {form.takeOn === true && (
          <TextInput
            label="Take-on date"
            type="date"
            required
            error={errors.takeOnDate}
            value={form.takeOnDate}
            onChange={(takeOnDate) => set({ takeOnDate })}
          />
        )}
      </div>
      <div className="row">
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.securityDeposit ?? false}
            onChange={(e) => set({ securityDeposit: e.target.checked })}
          />
          Security deposit with the Insurance Commission
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.takeOn ?? false}
            onChange={(e) => set({ takeOn: e.target.checked })}
          />
          Existing holding taken on (opening balance)
        </label>
      </div>
    </Modal>
  );
}

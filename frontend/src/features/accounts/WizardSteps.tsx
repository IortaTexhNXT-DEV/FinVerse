import { useQuery } from '@tanstack/react-query';
import type { PaymentArrangement } from '@/api/accounts';
import { catalogApi } from '@/api/catalog';
import type { PeriodBasis, Product } from '@/api/catalog';
import { ClientPicker } from '@/components/broking/ClientPicker';
import { LovSelect } from '@/components/broking/LovSelect';
import { Field } from '@/components/ui/Field';
import { useCompanyId } from '@/context/workspaceContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { oneYearAfter, withClient } from './accountForm';
import type { AccountDraft } from './accountForm';

export interface StepProps {
  draft: AccountDraft;
  set: (patch: Partial<AccountDraft>) => void;
  /** The chosen product, once loaded. */
  product?: Product;
  /** Fields locked once the draft is saved (client, product). */
  saved: boolean;
}

/** Step 1: the client (confirmed, or a prospect for a draft). */
export function ClientStep({ draft, set, saved }: Readonly<StepProps>) {
  return (
    <div className="form-grid">
      <Field
        label="Client"
        required
        hint="A prospect can be used for a draft; it must be confirmed before Processing validates the account."
      >
        {(id) => (
          <ClientPicker
            id={id}
            value={draft.clientId}
            allowProspect
            disabled={saved}
            onChange={(c) => set(withClient(draft, c))}
          />
        )}
      </Field>
    </div>
  );
}

/** Step 2: market segment, product, source and insurer. */
export function ProductStep({ draft, set, saved }: Readonly<StepProps>) {
  const companyId = useCompanyId();
  const products = useQuery({
    queryKey: ['catalog', 'products', { activeOnly: true, segment: draft.marketSegment }],
    queryFn: () =>
      catalogApi.products({ activeOnly: true, segment: draft.marketSegment || undefined }),
  });
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
  });
  const insurerId = insurers.data?.find((i) => i.partyCode === draft.insurerCode)?.id;
  const insurer = useQuery({
    queryKey: ['catalog', 'insurer', insurerId],
    queryFn: () => catalogApi.insurer(insurerId ?? 0),
    enabled: insurerId !== undefined,
  });
  return (
    <div className="form-grid">
      <Field label="Market segment">
        {(id) => (
          <LovSelect
            id={id}
            type="MARKET_SEGMENT"
            value={draft.marketSegment}
            onChange={(marketSegment) => set({ marketSegment })}
          />
        )}
      </Field>
      <SelectInput
        label="Product"
        required
        blank="Select"
        disabled={saved}
        value={draft.productCode}
        options={(products.data ?? []).map((p) => ({
          value: p.code,
          label: `${p.code} – ${p.name}`,
        }))}
        onChange={(productCode) => set({ productCode, items: [] })}
      />
      <Field label="Source channel">
        {(id) => (
          <LovSelect
            id={id}
            type="SOURCE_CHANNEL"
            value={draft.sourceChannel}
            onChange={(sourceChannel) => set({ sourceChannel })}
          />
        )}
      </Field>
      <SelectInput
        label="Insurer"
        blank="To be chosen by Processing"
        value={draft.insurerCode}
        options={(insurers.data ?? [])
          .filter((i) => i.recordStatus === 'ACTIVE')
          .map((i) => ({ value: i.partyCode, label: i.name }))}
        onChange={(insurerCode) => set({ insurerCode, insurerBranch: '' })}
      />
      <SelectInput
        label="Insurer branch"
        blank="Select"
        value={draft.insurerBranch}
        options={(insurer.data?.branches ?? []).map((b) => ({ value: b.code, label: b.name }))}
        onChange={(insurerBranch) => set({ insurerBranch })}
      />
    </div>
  );
}

const ARRANGEMENTS = [
  { value: 'VIA_BDOI', label: 'Premium paid through BDOI' },
  { value: 'DIRECT_TO_INSURER', label: 'Direct payment to the insurer' },
];

/** Step 3: period, term, currency, payment arrangement, mortgage and Free First Year. */
export function PeriodStep({ draft, set, product }: Readonly<StepProps>) {
  return (
    <div className="form-grid">
      <TextInput
        label="Period from"
        type="date"
        required
        value={draft.periodFrom}
        onChange={(periodFrom) => set({ periodFrom, periodTo: oneYearAfter(periodFrom) })}
      />
      <TextInput
        label="Period to"
        type="date"
        required
        value={draft.periodTo}
        onChange={(periodTo) => set({ periodTo })}
      />
      {product?.multiYearAllowed === true && (
        <>
          <label className="checkbox" style={{ alignSelf: 'end' }}>
            <input
              type="checkbox"
              checked={draft.multiYear}
              onChange={(e) => set({ multiYear: e.target.checked })}
            />
            Multi-year
          </label>
          <NumberInput
            label="Term (years)"
            step="1"
            disabled={!draft.multiYear}
            hint={`Up to ${product.maxTermYears}`}
            value={draft.termYears}
            onChange={(termYears) => set({ termYears: termYears ?? 1 })}
          />
        </>
      )}
      <TextInput
        label="Currency"
        upper
        value={draft.currency}
        onChange={(currency) => set({ currency })}
      />
      <SelectInput
        label="Payment"
        value={draft.paymentArrangement}
        options={product?.directPaymentEligible === false ? ARRANGEMENTS.slice(0, 1) : ARRANGEMENTS}
        onChange={(v) => set({ paymentArrangement: v as PaymentArrangement })}
      />
      {product?.mortgageApplicable === true && (
        <>
          <Field label="Mortgagee bank">
            {(id) => (
              <LovSelect
                id={id}
                type="MORTGAGEE_BANK"
                value={draft.mortgageeBank}
                onChange={(mortgageeBank) => set({ mortgageeBank })}
              />
            )}
          </Field>
          <TextInput
            label="Loan application no."
            value={draft.loanApplicationNo}
            onChange={(loanApplicationNo) => set({ loanApplicationNo })}
          />
          <TextInput
            label="PN numbers"
            hint="Separate with commas"
            value={draft.pnNumbers}
            onChange={(pnNumbers) => set({ pnNumbers })}
          />
        </>
      )}
      {product?.ffyEligible === true && (
        <TextInput
          label="Free First Year from"
          type="date"
          hint="Leave blank if not FFY"
          value={draft.ffyStart}
          onChange={(ffyStart) => set({ ffyStart })}
        />
      )}
    </div>
  );
}

const BASES = [
  { value: 'ANNUAL', label: 'Annual' },
  { value: 'PRO_RATA', label: 'Pro-rata (days)' },
  { value: 'SHORT_PERIOD', label: 'Short period (table)' },
];

/** Step 5: contact for this account and how the premium is rated. */
export function ContactStep({ draft, set }: Readonly<StepProps>) {
  return (
    <div className="form-grid">
      <TextInput
        label="Contact person"
        value={draft.contactName}
        onChange={(contactName) => set({ contactName })}
      />
      <TextInput
        label="E-mail"
        value={draft.contactEmail}
        onChange={(contactEmail) => set({ contactEmail })}
      />
      <TextInput
        label="Mobile"
        value={draft.contactMobile}
        onChange={(contactMobile) => set({ contactMobile })}
      />
      <TextInput
        label="Mailing address"
        value={draft.contactAddress}
        onChange={(contactAddress) => set({ contactAddress })}
      />
      <SelectInput
        label="Rating basis"
        value={draft.ratingBasis}
        options={BASES}
        onChange={(v) => set({ ratingBasis: v as PeriodBasis })}
      />
      <NumberInput
        label="Commission % override"
        hint="Blank = insurer / product rate"
        value={draft.commissionRate}
        onChange={(commissionRate) => set({ commissionRate })}
      />
    </div>
  );
}

import { useQuery } from '@tanstack/react-query';
import { catalogApi } from '@/api/catalog';
import type { PeriodBasis, Product, ProductDetail } from '@/api/catalog';
import { ClientPicker } from '@/components/broking/ClientPicker';
import { LovSelect } from '@/components/broking/LovSelect';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { useCompanyId } from '@/context/workspaceContext';
import { RiskItemsStep } from '@/features/accounts/RiskItemsStep';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { alignGroups, withGroup, yearAfter } from './quotationForm';
import type { QuotationForm } from './quotationForm';

export interface QuotationStepProps {
  form: QuotationForm;
  set: (patch: Partial<QuotationForm>) => void;
  errors: Record<string, string>;
  /** Client and product are fixed once the quotation is saved. */
  saved: boolean;
}

/** Step 1: the client or prospect (BRNB.063), segment and source channel. */
export function ClientStep({ form, set, errors, saved }: Readonly<QuotationStepProps>) {
  return (
    <div className="form-grid">
      <Field
        label="Client or prospect"
        required
        error={errors.clientId}
        hint="A prospect is enough to quote; the client must be confirmed before accounts are created."
      >
        {(id) => (
          <ClientPicker
            id={id}
            value={form.clientId}
            allowProspect
            disabled={saved && form.clientId !== undefined}
            onChange={(c) =>
              set({
                clientId: c?.id,
                clientName: c?.displayName ?? '',
                marketSegment: form.marketSegment || (c?.marketSegment ?? ''),
              })
            }
          />
        )}
      </Field>
      <Field label="Market segment">
        {(id) => (
          <LovSelect
            id={id}
            type="MARKET_SEGMENT"
            value={form.marketSegment}
            onChange={(marketSegment) => set({ marketSegment })}
          />
        )}
      </Field>
      <Field label="Source channel">
        {(id) => (
          <LovSelect
            id={id}
            type="SOURCE_CHANNEL"
            value={form.sourceChannel}
            onChange={(sourceChannel) => set({ sourceChannel })}
          />
        )}
      </Field>
    </div>
  );
}

/** The plate number of a vehicle item, shown next to its risk group. */
function plateOf(item: QuotationForm['items'][number]): string {
  const plate = item.vehicle?.plateNo;
  return plate ? ` (${plate})` : '';
}

const BASES = [
  { value: 'ANNUAL', label: 'Annual' },
  { value: 'PRO_RATA', label: 'Pro-rata (days)' },
  { value: 'SHORT_PERIOD', label: 'Short period (table)' },
];

function InsurerFields({ form, set }: Readonly<Pick<QuotationStepProps, 'form' | 'set'>>) {
  const companyId = useCompanyId();
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
    enabled: companyId > 0,
  });
  const insurerId = insurers.data?.find((i) => i.partyCode === form.insurerCode)?.id;
  const insurer = useQuery({
    queryKey: ['catalog', 'insurer', insurerId],
    queryFn: () => catalogApi.insurer(insurerId ?? 0),
    enabled: insurerId !== undefined,
  });
  return (
    <>
      <SelectInput
        label="Insurer"
        blank="To be advised"
        value={form.insurerCode}
        options={(insurers.data ?? [])
          .filter((i) => i.recordStatus === 'ACTIVE')
          .map((i) => ({ value: i.partyCode, label: i.name }))}
        onChange={(insurerCode) => set({ insurerCode, insurerBranch: '' })}
      />
      <SelectInput
        label="Insurer branch (LGT)"
        blank="Select"
        disabled={form.insurerCode === ''}
        value={form.insurerBranch}
        options={(insurer.data?.branches ?? []).map((b) => ({ value: b.code, label: b.name }))}
        onChange={(insurerBranch) => set({ insurerBranch })}
      />
    </>
  );
}

/** Step 2: product, insurer, period, validity, rating basis, direct payment and remarks. */
export function TermsStep({
  form,
  set,
  errors,
  saved,
  product,
}: Readonly<QuotationStepProps & { product?: Product }>) {
  const products = useQuery({
    queryKey: ['catalog', 'products', { activeOnly: true, segment: form.marketSegment }],
    queryFn: () =>
      catalogApi.products({ activeOnly: true, segment: form.marketSegment || undefined }),
  });
  return (
    <div className="form-grid">
      <SelectInput
        label="Product"
        required
        blank="Select"
        disabled={saved}
        error={errors.productCode}
        value={form.productCode}
        options={(products.data ?? []).map((p) => ({
          value: p.code,
          label: `${p.code} – ${p.name}${p.packaged ? '' : ' (non-package)'}`,
        }))}
        onChange={(productCode) => set({ productCode, items: [], groups: [] })}
      />
      <InsurerFields form={form} set={set} />
      <TextInput
        label="Period from"
        type="date"
        value={form.periodFrom}
        onChange={(periodFrom) => set({ periodFrom, periodTo: yearAfter(periodFrom) })}
      />
      <TextInput
        label="Period to"
        type="date"
        error={errors.periodTo}
        value={form.periodTo}
        onChange={(periodTo) => set({ periodTo })}
      />
      <TextInput
        label="Valid until"
        type="date"
        hint="Blank = the configured validity"
        value={form.validUntil}
        onChange={(validUntil) => set({ validUntil })}
      />
      <SelectInput
        label="Rating basis"
        value={form.ratingBasis}
        options={BASES}
        onChange={(v) => set({ ratingBasis: v as PeriodBasis })}
      />
      {product?.directPaymentEligible === true && (
        <label className="checkbox" style={{ alignSelf: 'end' }}>
          <input
            type="checkbox"
            checked={form.directPayment}
            onChange={(e) => set({ directPayment: e.target.checked })}
          />
          Premium paid directly to the insurer
        </label>
      )}
      <TextInput label="Remarks" value={form.remarks} onChange={(remarks) => set({ remarks })} />
    </div>
  );
}

/** Step 3: the risk items of the product line and the risk group (account) of each. */
export function ItemsStep({
  form,
  set,
  errors,
  detail,
}: Readonly<QuotationStepProps & { detail?: ProductDetail }>) {
  const groups = alignGroups(form.groups, form.items.length);
  return (
    <div className="stack">
      {errors.items && (
        <div className="alert warning" role="alert">
          {errors.items}
        </div>
      )}
      <RiskItemsStep
        kind={detail?.riskItemKind ?? 'GENERIC'}
        motor={detail?.ratingMethod === 'MOTOR'}
        fleet={detail?.product.fleetCapable !== false}
        items={form.items}
        onChange={(items) => set({ items, groups: alignGroups(form.groups, items.length) })}
      />
      {form.items.length > 1 && (
        <Card title="Accounts to create">
          <p className="muted">
            Items sharing a risk group become one account when the client accepts them.
          </p>
          <div className="form-grid">
            {form.items.map((item, index) => (
              <NumberInput
                key={`group-${index + 1}`}
                label={`Item ${index + 1}${plateOf(item)}: risk group`}
                step="1"
                value={groups[index]}
                onChange={(g) => set({ groups: withGroup(groups, index, g ?? 1) })}
              />
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}

import type { PolicyInput } from '@/api/underwriting';
import { CheckboxField, DateField, NumberField, SelectField, TextField } from './FormFields';
import { useUwLookups } from './useUwLookups';

interface Props {
  form: PolicyInput;
  editingExisting: boolean;
  onChange: (patch: Partial<PolicyInput>) => void;
}

const SOURCES = [
  { value: 'DIRECT', label: 'Direct' },
  { value: 'AGENT', label: 'Agent' },
  { value: 'BROKER', label: 'Broker' },
];

const BUSINESS_TYPES = [
  { value: 'DIRECT', label: 'Direct (100%)' },
  { value: 'DIRECT_WITH_COINSURANCE', label: 'Direct with coinsurance' },
];

/** Header terms of a policy: product, parties, channel, period, share and rates. */
export function PolicyHeaderFields({ form, editingExisting, onChange }: Readonly<Props>) {
  const lookups = useUwLookups();
  const intermediaryType = form.sourceType === 'AGENT' ? 'AGENT' : 'BROKER';
  const coinsured = form.businessType === 'DIRECT_WITH_COINSURANCE';
  return (
    <div className="form-grid">
      <SelectField
        label="Product"
        required
        disabled={editingExisting}
        value={form.productId > 0 ? String(form.productId) : ''}
        emptyLabel="Select product"
        options={lookups.activeProducts.map((p) => ({
          value: String(p.id),
          label: `${p.code} – ${p.name}`,
        }))}
        onChange={(v) => onChange({ productId: Number(v) })}
      />
      <SelectField
        label="Branch"
        required
        value={String(form.branchId)}
        options={lookups.branches.map((b) => ({ value: String(b.id), label: b.name }))}
        onChange={(v) => onChange({ branchId: Number(v) })}
      />
      <SelectField
        label="Customer (client)"
        required
        value={form.customerCode}
        emptyLabel="Select client"
        options={lookups.clients.map((c) => ({ value: c.code, label: `${c.code} – ${c.name}` }))}
        onChange={(v) =>
          onChange({
            customerCode: v,
            insuredName:
              form.insuredName === ''
                ? (lookups.clients.find((c) => c.code === v)?.name ?? '')
                : form.insuredName,
          })
        }
      />
      <TextField
        label="Insured name"
        required
        value={form.insuredName}
        onChange={(v) => onChange({ insuredName: v })}
      />
      <SelectField
        label="Source"
        required
        value={form.sourceType}
        options={SOURCES}
        onChange={(v) => onChange({ sourceType: v as PolicyInput['sourceType'] })}
      />
      {form.sourceType !== 'DIRECT' && (
        <SelectField
          label={form.sourceType === 'AGENT' ? 'Agent' : 'Broker'}
          required
          value={form.intermediaryCode}
          emptyLabel="Select intermediary"
          options={lookups.intermediaries
            .filter((i) => i.partyType === intermediaryType)
            .map((i) => ({ value: i.code, label: `${i.code} – ${i.name}` }))}
          onChange={(v) => onChange({ intermediaryCode: v })}
        />
      )}
      {form.sourceType !== 'DIRECT' && (
        <NumberField
          label="Commission %"
          hint="Blank = intermediary / product rate"
          value={form.commissionRate}
          onChange={(v) => onChange({ commissionRate: v })}
        />
      )}
      <DateField
        label="Issue date"
        required
        value={form.issueDate}
        onChange={(v) => onChange({ issueDate: v })}
      />
      <DateField
        label="Period from"
        required
        value={form.periodFrom}
        onChange={(v) => onChange({ periodFrom: v })}
      />
      <DateField
        label="Period to"
        required
        value={form.periodTo}
        onChange={(v) => onChange({ periodTo: v })}
      />
      <SelectField
        label="Currency"
        required
        value={form.currency}
        options={lookups.currencies.map((c) => ({ value: c.code, label: c.code }))}
        onChange={(v) => onChange({ currency: v })}
      />
      <SelectField
        label="Business type"
        required
        value={form.businessType}
        options={BUSINESS_TYPES}
        onChange={(v) => onChange({ businessType: v as PolicyInput['businessType'] })}
      />
      {coinsured && (
        <NumberField
          label="Our share %"
          required
          value={form.sharePct}
          onChange={(v) => onChange({ sharePct: v ?? 0 })}
        />
      )}
      {coinsured && (
        <SelectField
          label="Coinsurer"
          required
          value={form.coinsurerCode}
          emptyLabel="Select coinsurer"
          options={lookups.coinsurers.map((c) => ({
            value: c.code,
            label: `${c.code} – ${c.name}`,
          }))}
          onChange={(v) => onChange({ coinsurerCode: v })}
        />
      )}
      {coinsured && (
        <CheckboxField
          label="We lead (bill 100%)"
          checked={form.coinsuranceLeader}
          onChange={(v) => onChange({ coinsuranceLeader: v })}
        />
      )}
      <NumberField
        label="Discount %"
        value={form.discountRate}
        onChange={(v) => onChange({ discountRate: v })}
      />
      <NumberField
        label="Loading %"
        value={form.loadingRate}
        onChange={(v) => onChange({ loadingRate: v })}
      />
    </div>
  );
}

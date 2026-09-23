import type { QuotationInput } from '@/api/underwriting';
import { DateField, NumberField, SelectField, TextField } from './FormFields';
import { IterationFields } from './IterationFields';
import { useUwLookups } from './useUwLookups';

const SOURCES = [
  { value: 'DIRECT', label: 'Direct' },
  { value: 'AGENT', label: 'Agent' },
  { value: 'BROKER', label: 'Broker' },
];

/** Quotation terms and first iteration. */
export function QuotationForm({
  form,
  onChange,
}: Readonly<{ form: QuotationInput; onChange: (form: QuotationInput) => void }>) {
  const lookups = useUwLookups();
  const set = (patch: Partial<QuotationInput>) => onChange({ ...form, ...patch });
  const intermediaryType = form.sourceType === 'AGENT' ? 'AGENT' : 'BROKER';
  return (
    <div className="stack">
      <div className="form-grid">
        <SelectField
          label="Product"
          required
          value={form.productId > 0 ? String(form.productId) : ''}
          emptyLabel="Select product"
          options={lookups.activeProducts.map((p) => ({ value: String(p.id), label: p.name }))}
          onChange={(v) => set({ productId: Number(v) })}
        />
        <SelectField
          label="Branch"
          required
          value={form.branchId > 0 ? String(form.branchId) : ''}
          emptyLabel="Select branch"
          options={lookups.branches.map((b) => ({ value: String(b.id), label: b.name }))}
          onChange={(v) => set({ branchId: Number(v) })}
        />
        <SelectField
          label="Customer"
          required
          value={form.customerCode}
          emptyLabel="Select client"
          options={lookups.clients.map((c) => ({ value: c.code, label: c.name }))}
          onChange={(v) =>
            set({
              customerCode: v,
              insuredName: lookups.clients.find((c) => c.code === v)?.name ?? '',
            })
          }
        />
        <TextField
          label="Insured"
          required
          value={form.insuredName}
          onChange={(v) => set({ insuredName: v })}
        />
        <SelectField
          label="Source"
          required
          value={form.sourceType}
          options={SOURCES}
          onChange={(v) =>
            set({ sourceType: v as QuotationInput['sourceType'], intermediaryCode: undefined })
          }
        />
        {form.sourceType !== 'DIRECT' && (
          <SelectField
            label="Agent / broker"
            required
            value={form.intermediaryCode}
            emptyLabel="Select intermediary"
            options={lookups.intermediaries
              .filter((i) => i.partyType === intermediaryType)
              .map((i) => ({ value: i.code, label: i.name }))}
            onChange={(v) => set({ intermediaryCode: v })}
          />
        )}
        <DateField
          label="Issue date"
          required
          value={form.issueDate}
          onChange={(v) => set({ issueDate: v })}
        />
        <NumberField
          label="Validity (days)"
          required
          value={form.validityDays}
          onChange={(v) => set({ validityDays: v ?? 30 })}
        />
        <DateField
          label="Period from"
          required
          value={form.periodFrom}
          onChange={(v) => set({ periodFrom: v })}
        />
        <DateField
          label="Period to"
          required
          value={form.periodTo}
          onChange={(v) => set({ periodTo: v })}
        />
        <NumberField
          label="Our share %"
          required
          value={form.sharePct}
          onChange={(v) => set({ sharePct: v ?? 100 })}
        />
        <NumberField
          label="Brokerage %"
          hint="Blank = product default"
          value={form.commissionRate}
          onChange={(v) => set({ commissionRate: v })}
        />
      </div>
      {form.iteration !== undefined && (
        <IterationFields
          value={form.iteration}
          sharePct={form.sharePct}
          onChange={(iteration) => set({ iteration })}
        />
      )}
    </div>
  );
}

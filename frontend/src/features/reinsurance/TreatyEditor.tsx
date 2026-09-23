import type { TreatyType } from '@/api/reinsurance';
import type { Party } from '@/api/parties';
import type { DimensionValue } from '@/api/masters';
import { DateField, NumberField, SelectField, TextField } from '@/features/underwriting/FormFields';
import type { TreatyForm } from './treatyForm';
import { LayerRows, ParticipantRows } from './TreatyRows';

const TYPES = [
  { value: 'QUOTA_SHARE', label: 'Quota share' },
  { value: 'SURPLUS', label: 'Surplus' },
  { value: 'XOL', label: 'Excess of loss' },
];

interface TreatyEditorProps {
  form: TreatyForm;
  reinsurers: Party[];
  brokers: Party[];
  businessLines: DimensionValue[];
  onChange: (form: TreatyForm) => void;
}

/** Treaty terms, participants and (excess of loss) layers. */
export function TreatyEditor({
  form,
  reinsurers,
  brokers,
  businessLines,
  onChange,
}: Readonly<TreatyEditorProps>) {
  const set = (patch: Partial<TreatyForm>) => onChange({ ...form, ...patch });
  return (
    <div className="stack">
      <div className="form-grid">
        <TextField
          label="Code"
          required
          disabled={form.id !== undefined}
          value={form.code}
          onChange={(v) => set({ code: v.toUpperCase() })}
        />
        <TextField label="Name" required value={form.name} onChange={(v) => set({ name: v })} />
        <SelectField
          label="Treaty type"
          required
          value={form.treatyType}
          options={TYPES}
          onChange={(v) => set({ treatyType: v as TreatyType })}
        />
        <SelectField
          label="Class (line of business)"
          required
          value={form.businessLine}
          emptyLabel="Select class"
          options={businessLines.map((d) => ({ value: d.code, label: d.name }))}
          onChange={(v) => set({ businessLine: v })}
        />
        <NumberField
          label="Underwriting year"
          required
          value={form.uwYear}
          onChange={(v) => set({ uwYear: v ?? form.uwYear })}
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
        <TextField
          label="Currency"
          required
          disabled
          value={form.currency}
          onChange={() => undefined}
        />
        <TypeTerms form={form} set={set} />
        <NumberField
          label="Premium tax / levy %"
          value={form.levyPct}
          onChange={(v) => set({ levyPct: v })}
        />
        <NumberField
          label="Interest on reserves % p.a."
          value={form.reserveInterestPct}
          onChange={(v) => set({ reserveInterestPct: v })}
        />
        <NumberField
          label="O/S loss reserve retained %"
          value={form.lossReservePct}
          onChange={(v) => set({ lossReservePct: v })}
        />
        <SelectField
          label="Reinsurance broker"
          value={form.brokerCode ?? ''}
          emptyLabel="Placed direct"
          options={brokers.map((p) => ({ value: p.code, label: `${p.code} ${p.name}` }))}
          onChange={(v) => set({ brokerCode: v })}
        />
      </div>
      <ParticipantRows
        rows={form.participants}
        reinsurers={reinsurers}
        onChange={(participants) => set({ participants })}
      />
      {form.treatyType === 'XOL' && (
        <LayerRows rows={form.layers} onChange={(layers) => set({ layers })} />
      )}
    </div>
  );
}

/** Capacity terms that depend on the treaty type. */
function TypeTerms({
  form,
  set,
}: Readonly<{ form: TreatyForm; set: (patch: Partial<TreatyForm>) => void }>) {
  if (form.treatyType === 'QUOTA_SHARE') {
    return (
      <>
        <NumberField
          label="Quota share %"
          required
          value={form.quotaSharePct}
          onChange={(v) => set({ quotaSharePct: v })}
        />
        <NumberField
          label="Limit per risk (blank = unlimited)"
          value={form.treatyLimit}
          onChange={(v) => set({ treatyLimit: v })}
        />
      </>
    );
  }
  if (form.treatyType === 'SURPLUS') {
    return (
      <>
        <NumberField
          label="Retention (one line)"
          required
          value={form.retentionLimit}
          onChange={(v) => set({ retentionLimit: v })}
        />
        <NumberField
          label="Number of lines"
          required
          value={form.lines}
          onChange={(v) => set({ lines: v })}
        />
      </>
    );
  }
  return null;
}

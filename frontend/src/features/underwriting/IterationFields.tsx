import type { IterationInput } from '@/api/underwriting';
import { formatAmount } from '@/utils/format';
import { NumberField, TextField } from './FormFields';
import { pct, round2 } from './premiumMath';

/** Figures of a quotation iteration at 100 % with the resulting net and our-share premium. */
export function IterationFields({
  value,
  sharePct,
  onChange,
}: Readonly<{
  value: IterationInput;
  sharePct: number;
  onChange: (value: IterationInput) => void;
}>) {
  const set = (patch: Partial<IterationInput>) => onChange({ ...value, ...patch });
  const net = round2(value.grossPremium + (value.loading ?? 0) - (value.discount ?? 0));
  return (
    <div className="stack">
      <div className="form-grid">
        <NumberField
          label="Sum insured (100%)"
          required
          value={value.sumInsured}
          onChange={(v) => set({ sumInsured: v ?? 0 })}
        />
        <NumberField
          label="Gross premium (100%)"
          required
          value={value.grossPremium}
          onChange={(v) => set({ grossPremium: v ?? 0 })}
        />
        <NumberField
          label="Discount"
          value={value.discount}
          onChange={(v) => set({ discount: v })}
        />
        <NumberField label="Loading" value={value.loading} onChange={(v) => set({ loading: v })} />
        <NumberField label="Charges" value={value.charges} onChange={(v) => set({ charges: v })} />
        <TextField label="Remarks" value={value.remarks} onChange={(v) => set({ remarks: v })} />
      </div>
      <p className="muted" style={{ margin: 0 }}>
        Net premium (100%) {formatAmount(net)} · our share ({sharePct}%){' '}
        {formatAmount(pct(net, sharePct))}
      </p>
    </div>
  );
}

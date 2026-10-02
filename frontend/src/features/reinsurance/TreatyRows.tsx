import { Plus, Trash2 } from 'lucide-react';
import type { LayerInput, ParticipantInput } from '@/api/reinsurance';
import type { Party } from '@/api/parties';
import { Button } from '@/components/ui/Button';
import { NumberField, SelectField } from '@/features/underwriting/FormFields';
import { blankLayer, blankParticipant, totalShare } from './treatyForm';

/** Editable participant lines of a treaty (reinsurer, share and commercial terms). */
export function ParticipantRows({
  rows,
  reinsurers,
  onChange,
}: Readonly<{
  rows: ParticipantInput[];
  reinsurers: Party[];
  onChange: (rows: ParticipantInput[]) => void;
}>) {
  const update = (key: string | undefined, patch: Partial<ParticipantInput>) =>
    onChange(rows.map((r) => (r.key === key ? { ...r, ...patch } : r)));
  const total = totalShare(rows);
  return (
    <div className="stack">
      <div className="row">
        <h3>Participants</h3>
        <span className={total === 100 ? 'muted' : 'field-error'}>Total share {total} %</span>
        <div className="spacer" />
        <Button
          size="sm"
          variant="secondary"
          icon={<Plus size={14} />}
          onClick={() => onChange([...rows, blankParticipant()])}
        >
          Add Reinsurer
        </Button>
      </div>
      {rows.map((r) => (
        <div key={r.key} className="form-grid">
          <SelectField
            label="Reinsurer"
            required
            value={r.reinsurerCode}
            emptyLabel="Select reinsurer"
            options={reinsurers.map((p) => ({ value: p.code, label: `${p.code} ${p.name}` }))}
            onChange={(v) => update(r.key, { reinsurerCode: v })}
          />
          <NumberField
            label="Share %"
            required
            value={r.sharePct}
            onChange={(v) => update(r.key, { sharePct: v ?? 0 })}
          />
          <NumberField
            label="Commission %"
            value={r.commissionPct}
            onChange={(v) => update(r.key, { commissionPct: v })}
          />
          <NumberField
            label="Profit commission %"
            value={r.profitCommissionPct}
            onChange={(v) => update(r.key, { profitCommissionPct: v })}
          />
          <NumberField
            label="Premium reserve retained %"
            value={r.premiumReservePct}
            onChange={(v) => update(r.key, { premiumReservePct: v })}
          />
          <Button
            size="sm"
            variant="ghost"
            aria-label="Remove reinsurer"
            icon={<Trash2 size={14} />}
            disabled={rows.length === 1}
            onClick={() => onChange(rows.filter((x) => x.key !== r.key))}
          />
        </div>
      ))}
    </div>
  );
}

/** Editable layers of an excess of loss treaty. */
export function LayerRows({
  rows,
  onChange,
}: Readonly<{ rows: LayerInput[]; onChange: (rows: LayerInput[]) => void }>) {
  const update = (key: string | undefined, patch: Partial<LayerInput>) =>
    onChange(rows.map((r) => (r.key === key ? { ...r, ...patch } : r)));
  return (
    <div className="stack">
      <div className="row">
        <h3>Layers</h3>
        <div className="spacer" />
        <Button
          size="sm"
          variant="secondary"
          icon={<Plus size={14} />}
          onClick={() => onChange([...rows, blankLayer()])}
        >
          Add Layer
        </Button>
      </div>
      {rows.map((r, i) => (
        <div key={r.key} className="form-grid">
          <NumberField
            label={`Layer ${i + 1} priority`}
            required
            value={r.priority}
            onChange={(v) => update(r.key, { priority: v ?? 0 })}
          />
          <NumberField
            label="Limit"
            required
            value={r.limit}
            onChange={(v) => update(r.key, { limit: v ?? 0 })}
          />
          <NumberField
            label="Minimum and deposit premium"
            value={r.minDepositPremium}
            onChange={(v) => update(r.key, { minDepositPremium: v })}
          />
          <NumberField
            label="Reinstatements"
            value={r.reinstatements}
            onChange={(v) => update(r.key, { reinstatements: v })}
          />
          <Button
            size="sm"
            variant="ghost"
            aria-label="Remove layer"
            icon={<Trash2 size={14} />}
            onClick={() => onChange(rows.filter((x) => x.key !== r.key))}
          />
        </div>
      ))}
    </div>
  );
}

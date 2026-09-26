import { Plus, Trash2 } from 'lucide-react';
import type { RiskInput } from '@/api/underwriting';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { DateField, NumberField, TextField } from './FormFields';
import { emptyRisk } from './policyForm';
import { riskPremium, riskTotals } from './premiumMath';

interface Props {
  risks: RiskInput[];
  marine: boolean;
  /** Exactly one risk (marine certificate): no add / remove. */
  single?: boolean;
  onChange: (risks: RiskInput[]) => void;
}

function MarineFields({
  risk,
  onPatch,
}: Readonly<{ risk: RiskInput; onPatch: (patch: Partial<RiskInput>) => void }>) {
  return (
    <div className="form-grid">
      <TextField
        label="Vessel"
        value={risk.vesselName}
        onChange={(v) => onPatch({ vesselName: v })}
      />
      <TextField
        label="Port from"
        value={risk.voyageFrom}
        onChange={(v) => onPatch({ voyageFrom: v })}
      />
      <TextField label="Port to" value={risk.voyageTo} onChange={(v) => onPatch({ voyageTo: v })} />
      <DateField
        label="Sail date"
        value={risk.sailDate}
        onChange={(v) => onPatch({ sailDate: v })}
      />
      <TextField label="B/L no." value={risk.blNo} onChange={(v) => onPatch({ blNo: v })} />
      <DateField label="B/L date" value={risk.blDate} onChange={(v) => onPatch({ blDate: v })} />
      <TextField label="LC no." value={risk.lcNo} onChange={(v) => onPatch({ lcNo: v })} />
      <TextField label="LC bank" value={risk.bankName} onChange={(v) => onPatch({ bankName: v })} />
      <TextField
        label="Basis of valuation"
        value={risk.valuationBasis}
        onChange={(v) => onPatch({ valuationBasis: v })}
      />
    </div>
  );
}

/** Editable list of insured risks (sections) with running totals at 100 %. */
export function RiskEditor({ risks, marine, single = false, onChange }: Readonly<Props>) {
  const patch = (index: number, p: Partial<RiskInput>) =>
    onChange(risks.map((r, i) => (i === index ? { ...r, ...p } : r)));
  const totals = riskTotals(risks);
  return (
    <div className="stack">
      {risks.map((risk, index) => (
        <div key={risk.key} className="card">
          <div className="card-body stack">
            <div className="row">
              <strong>Risk {index + 1}</strong>
              <span className="muted">
                Premium <Amount value={riskPremium(risk)} />
              </span>
              <div className="spacer" />
              {risks.length > 1 && (
                <Button
                  size="sm"
                  variant="ghost"
                  icon={<Trash2 size={14} />}
                  onClick={() => onChange(risks.filter((_, i) => i !== index))}
                >
                  Remove
                </Button>
              )}
            </div>
            <div className="form-grid">
              <TextField
                label="Description"
                required
                value={risk.description}
                onChange={(v) => patch(index, { description: v })}
              />
              <NumberField
                label="Sum insured"
                required
                value={risk.sumInsured}
                onChange={(v) => patch(index, { sumInsured: v ?? 0 })}
              />
              <NumberField
                label="Rate %"
                value={risk.rate}
                onChange={(v) => patch(index, { rate: v })}
              />
              <NumberField
                label="Premium"
                hint="Blank = sum insured × rate"
                value={risk.premium}
                onChange={(v) => patch(index, { premium: v })}
              />
              <TextField
                label="Occupation"
                value={risk.occupation}
                onChange={(v) => patch(index, { occupation: v })}
              />
              <TextField
                label="Accumulation zone"
                value={risk.accumulationZone}
                onChange={(v) => patch(index, { accumulationZone: v })}
              />
            </div>
            {marine && <MarineFields risk={risk} onPatch={(p) => patch(index, p)} />}
          </div>
        </div>
      ))}
      <div className="row">
        {!single && (
          <Button
            variant="secondary"
            icon={<Plus size={16} />}
            onClick={() => onChange([...risks, emptyRisk()])}
          >
            Add Risk
          </Button>
        )}
        <div className="spacer" />
        <span>
          Total sum insured{' '}
          <strong>
            <Amount value={totals.sumInsured} />
          </strong>{' '}
          · Gross premium{' '}
          <strong>
            <Amount value={totals.grossPremium} />
          </strong>
        </span>
      </div>
    </div>
  );
}

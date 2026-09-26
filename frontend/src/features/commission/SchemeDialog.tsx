import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import type {
  Beneficiary,
  Calculation,
  IncentiveTier,
  PeriodType,
  Scheme,
  SchemeTerms,
  SchemeType,
} from './commissionApi';
import { schemeProblem } from './commissionLogic';

const NEW_TERMS: SchemeTerms = {
  name: '',
  schemeType: 'OTHER',
  calculation: 'TARGET_TIERED',
  periodType: 'MONTHLY',
  beneficiary: 'BDOI',
  segments: [],
  productLines: [],
  active: false,
  tiers: [],
};

function numberOrUndefined(value: string): number | undefined {
  return value.trim() === '' ? undefined : Number(value);
}

function list(value: string): string[] {
  return value
    .split(',')
    .map((v) => v.trim().toUpperCase())
    .filter((v) => v !== '');
}

function TierEditor({
  calculation,
  tiers,
  onChange,
}: Readonly<{
  calculation: Calculation;
  tiers: IncentiveTier[];
  onChange: (tiers: IncentiveTier[]) => void;
}>) {
  const tiered = calculation === 'TARGET_TIERED';
  const fields: readonly { key: keyof IncentiveTier; label: string }[] = tiered
    ? [
        { key: 'minProduction', label: 'Production Target' },
        { key: 'ratePercent', label: 'Rate %' },
        { key: 'multiplier', label: 'Multiplier' },
      ]
    : [
        { key: 'minBasicPremium', label: 'Minimum Basic Premium' },
        { key: 'fixedAmount', label: 'Amount per Policy' },
      ];
  const set = (index: number, key: keyof IncentiveTier, value: string) =>
    onChange(tiers.map((t, i) => (i === index ? { ...t, [key]: numberOrUndefined(value) } : t)));
  return (
    <div className="stack">
      <table className="table">
        <caption className="visually-hidden">Tiers</caption>
        <thead>
          <tr>
            <th scope="col">Tier</th>
            {fields.map((f) => (
              <th scope="col" key={f.key}>
                {f.label}
              </th>
            ))}
            <th scope="col">
              <span className="visually-hidden">Remove</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {tiers.map((t, index) => (
            <tr key={index}>
              <th scope="row">{index + 1}</th>
              {fields.map((f) => (
                <td key={f.key}>
                  <input
                    className="input"
                    type="number"
                    min={0}
                    step="any"
                    aria-label={`${f.label} of tier ${String(index + 1)}`}
                    value={t[f.key] ?? ''}
                    onChange={(e) => set(index, f.key, e.target.value)}
                  />
                </td>
              ))}
              <td>
                <Button
                  size="sm"
                  variant="ghost"
                  icon={<Trash2 size={14} />}
                  aria-label={`Remove tier ${String(index + 1)}`}
                  onClick={() => onChange(tiers.filter((_, i) => i !== index))}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div>
        <Button
          size="sm"
          variant="secondary"
          icon={<Plus size={14} />}
          onClick={() => onChange([...tiers, {}])}
        >
          Add Tier
        </Button>
      </div>
    </div>
  );
}

function Select<T extends string>({
  label,
  value,
  options,
  onChange,
}: Readonly<{
  label: string;
  value: T;
  options: readonly { id: T; label: string }[];
  onChange: (v: T) => void;
}>) {
  return (
    <Field label={label} required>
      {(id) => (
        <select
          id={id}
          className="select"
          value={value}
          onChange={(e) => onChange(e.target.value as T)}
        >
          {options.map((o) => (
            <option key={o.id} value={o.id}>
              {o.label}
            </option>
          ))}
        </select>
      )}
    </Field>
  );
}

const TYPES: readonly { id: SchemeType; label: string }[] = [
  { id: 'NO_TOUCH', label: 'No Touch' },
  { id: 'TOP_UP', label: 'Top Up' },
  { id: 'MOTOR_MANIA', label: 'Motor Mania' },
  { id: 'OTHER', label: 'Other' },
];
const CALCULATIONS: readonly { id: Calculation; label: string }[] = [
  { id: 'TARGET_TIERED', label: 'Production Target Tiers' },
  { id: 'FIXED_PER_POLICY', label: 'Fixed Amount per Policy' },
];
const PERIODS: readonly { id: PeriodType; label: string }[] = [
  { id: 'MONTHLY', label: 'Monthly' },
  { id: 'QUARTERLY', label: 'Quarterly' },
  { id: 'SEMI_ANNUAL', label: 'Semi-annual' },
  { id: 'ANNUAL', label: 'Annual' },
  { id: 'CUSTOM', label: 'Custom' },
];
const BENEFICIARIES: readonly { id: Beneficiary; label: string }[] = [
  { id: 'BDOI', label: 'BDOI' },
  { id: 'BRANCH', label: 'Branch (Passed On)' },
];

function DateField({
  label,
  value,
  onChange,
}: Readonly<{
  label: string;
  value: string | undefined;
  onChange: (v: string | undefined) => void;
}>) {
  return (
    <Field label={label}>
      {(id) => (
        <input
          id={id}
          type="date"
          className="input"
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value || undefined)}
        />
      )}
    </Field>
  );
}

/** Creates or changes an incentive scheme with its tiers (CMRID.005/006). */
export function SchemeDialog({
  scheme,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  scheme: Scheme | undefined;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (code: string, terms: SchemeTerms) => void;
}>) {
  const [code, setCode] = useState(scheme?.code ?? '');
  const [terms, setTerms] = useState<SchemeTerms>(scheme?.terms ?? NEW_TERMS);
  const [problem, setProblem] = useState<string>();
  const set = (patch: Partial<SchemeTerms>) => {
    setTerms((t) => ({ ...t, ...patch }));
    setProblem(undefined);
  };
  const save = () => {
    const p = schemeProblem(code, terms);
    if (p === undefined) {
      onSave(code.trim().toUpperCase(), terms);
    } else {
      setProblem(p);
    }
  };
  return (
    <Modal
      title={scheme ? `Edit ${scheme.terms.name}` : 'New Incentive Scheme'}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={save}>
            Save Scheme
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        {problem && <p className="field-error">{problem}</p>}
        <div className="form-grid">
          <Field label="Code" required>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={30}
                disabled={scheme !== undefined}
                value={code}
                onChange={(e) => setCode(e.target.value)}
              />
            )}
          </Field>
          <Field label="Name" required>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={120}
                value={terms.name}
                onChange={(e) => set({ name: e.target.value })}
              />
            )}
          </Field>
          <Select
            label="Scheme Type"
            value={terms.schemeType}
            options={TYPES}
            onChange={(schemeType) => set({ schemeType })}
          />
          <Select
            label="Calculation"
            value={terms.calculation}
            options={CALCULATIONS}
            onChange={(calculation) => set({ calculation, tiers: [] })}
          />
          <Select
            label="Period"
            value={terms.periodType}
            options={PERIODS}
            onChange={(periodType) => set({ periodType })}
          />
          <Select
            label="Beneficiary"
            value={terms.beneficiary}
            options={BENEFICIARIES}
            onChange={(beneficiary) => set({ beneficiary })}
          />
          <Field label="Insurer Code" hint="Blank for every insurer">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={30}
                value={terms.insurerCode ?? ''}
                onChange={(e) => set({ insurerCode: e.target.value.toUpperCase() || undefined })}
              />
            )}
          </Field>
          <Field label="Segments" hint="Comma separated; blank for all">
            {(id) => (
              <input
                id={id}
                className="input"
                value={terms.segments.join(', ')}
                onChange={(e) => set({ segments: list(e.target.value) })}
              />
            )}
          </Field>
          <Field label="Product Lines" hint="Comma separated; blank for all">
            {(id) => (
              <input
                id={id}
                className="input"
                value={terms.productLines.join(', ')}
                onChange={(e) => set({ productLines: list(e.target.value) })}
              />
            )}
          </Field>
          <DateField
            label="Effective From"
            value={terms.effectiveFrom}
            onChange={(effectiveFrom) => set({ effectiveFrom })}
          />
          <DateField
            label="Effective To"
            value={terms.effectiveTo}
            onChange={(effectiveTo) => set({ effectiveTo })}
          />
        </div>
        <TierEditor
          calculation={terms.calculation}
          tiers={terms.tiers}
          onChange={(tiers) => set({ tiers })}
        />
        <label className="row">
          <input
            type="checkbox"
            checked={terms.active}
            onChange={(e) => set({ active: e.target.checked })}
          />{' '}
          Active
        </label>
      </div>
    </Modal>
  );
}

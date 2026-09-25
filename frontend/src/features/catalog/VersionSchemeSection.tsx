import type { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import type { VersionForm } from './versionForm';

type TextKey = Exclude<keyof VersionForm, 'coverages' | 'insurers' | 'terms'>;

interface Props {
  form: VersionForm;
  errors: Record<string, string>;
  readOnly: boolean;
  onChange: (form: VersionForm) => void;
}

interface InputSpec {
  key: TextKey;
  label: string;
  type?: 'number' | 'date' | 'text';
  required?: boolean;
  hint?: string;
}

const SCHEME: InputSpec[] = [
  {
    key: 'defaultRate',
    label: 'Package Rate %',
    type: 'number',
    hint: 'Empty when every insurer has its own rate',
  },
  { key: 'minimumPremium', label: 'Minimum Premium', type: 'number', required: true },
  { key: 'defaultCommissionRate', label: 'Commission %', type: 'number', required: true },
  {
    key: 'maxSumInsured',
    label: 'Package TSI Limit',
    type: 'number',
    hint: 'Above it TSU prices the risk',
  },
];

const DATES: InputSpec[] = [
  { key: 'effectiveFrom', label: 'Effective From', type: 'date', required: true },
  { key: 'packageStartDate', label: 'Package Start', type: 'date' },
  {
    key: 'packageEndDate',
    label: 'Package End',
    type: 'date',
    hint: 'Expiry monitoring (BRPM.017)',
  },
  { key: 'anniversaryDate', label: 'Anniversary', type: 'date' },
];

const ORIGIN: InputSpec[] = [
  { key: 'changeSummary', label: 'Change Summary' },
  { key: 'mancomSignoffRef', label: 'ManCom Sign-off Reference' },
  { key: 'ratingBasisNote', label: 'Computation Basis', hint: 'As agreed with the insurers' },
];

function Inputs({
  specs,
  form,
  errors,
  readOnly,
  onChange,
}: Readonly<Props & { specs: InputSpec[] }>): ReactNode {
  return (
    <div className="form-grid">
      {specs.map((spec) => (
        <Field
          key={spec.key}
          label={spec.label}
          required={spec.required}
          error={errors[spec.key]}
          hint={spec.hint}
        >
          {(id) => (
            <input
              id={id}
              className="input"
              type={spec.type ?? 'text'}
              step={spec.type === 'number' ? 'any' : undefined}
              value={form[spec.key]}
              disabled={readOnly}
              onChange={(e) => onChange({ ...form, [spec.key]: e.target.value })}
            />
          )}
        </Field>
      ))}
    </div>
  );
}

/**
 * Rate scheme, package term and origin of a package version (BRPM.007/017): what new business is
 * priced on once the version is released.
 */
export function VersionSchemeSection(props: Readonly<Props>) {
  return (
    <div className="stack">
      <Card title="Rate Scheme">
        <Inputs specs={SCHEME} {...props} />
      </Card>
      <Card title="Dates">
        <Inputs specs={DATES} {...props} />
      </Card>
      <Card title="Origin">
        <Inputs specs={ORIGIN} {...props} />
      </Card>
    </div>
  );
}

import type { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { TypedInput } from '@/components/ui/DateInput';
import { formatAmount, formatDate, formatRate } from '@/utils/format';
import { DetailList } from './DetailList';
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
  /** How a number reads on a released version: a rate in percent or an amount. */
  format?: 'rate' | 'amount';
  required?: boolean;
  hint?: string;
}

const SCHEME: InputSpec[] = [
  {
    key: 'defaultRate',
    label: 'Package Rate %',
    type: 'number',
    format: 'rate',
    hint: 'Empty when every insurer has its own rate',
  },
  {
    key: 'minimumPremium',
    label: 'Minimum Premium',
    type: 'number',
    format: 'amount',
    required: true,
  },
  {
    key: 'defaultCommissionRate',
    label: 'Commission %',
    type: 'number',
    format: 'rate',
    required: true,
  },
  {
    key: 'maxSumInsured',
    label: 'Package TSI Limit',
    type: 'number',
    format: 'amount',
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
    hint: 'Expiry monitoring',
  },
  { key: 'anniversaryDate', label: 'Anniversary', type: 'date' },
];

const ORIGIN: InputSpec[] = [
  { key: 'changeSummary', label: 'Change Summary' },
  { key: 'mancomSignoffRef', label: 'ManCom Sign-off Reference' },
  { key: 'ratingBasisNote', label: 'Computation Basis', hint: 'As agreed with the insurers' },
];

/** A value of a released (read-only) version as people read it; empty shows the muted dash. */
function shown(spec: InputSpec, value: string): string {
  if (spec.type === 'date') {
    return formatDate(value);
  }
  if (spec.format === 'rate') {
    return formatRate(value);
  }
  return spec.format === 'amount' ? formatAmount(value) : value;
}

function Inputs({
  specs,
  form,
  errors,
  readOnly,
  onChange,
}: Readonly<Props & { specs: InputSpec[] }>): ReactNode {
  if (readOnly) {
    return <DetailList rows={specs.map((spec) => [spec.label, shown(spec, form[spec.key])])} />;
  }
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
            <TypedInput
              id={id}
              className="input"
              type={spec.type ?? 'text'}
              step={spec.type === 'number' ? 'any' : undefined}
              value={form[spec.key]}
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
 * priced on once the version is released. A released version reads as formatted values
 * (amounts with separators, rates in percent, dates as dd-MMM-yyyy, a dash when empty).
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

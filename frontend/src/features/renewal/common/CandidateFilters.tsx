import { useQuery } from '@tanstack/react-query';
import { renewalApi } from '@/api/renewal';
import type { RenewalFilters } from '@/api/renewal';
import { MultiSelectFilter } from '@/components/broking/MultiSelectFilter';
import { DateInput } from '@/components/ui/DateInput';
import { Field } from '@/components/ui/Field';
import { useCompanyId } from '@/context/workspaceContext';

/** Multi-select criteria of the Filters panel and the code list that feeds each one. */
const CRITERIA: { key: keyof RenewalFilters; label: string; source: string }[] = [
  { key: 'unitHead', label: 'Unit Head', source: 'renewal.unitHead' },
  { key: 'origin', label: 'Business Origin', source: 'renewal.origin' },
  { key: 'accountType', label: 'Account Type', source: 'renewal.accountType' },
  { key: 'region', label: 'Region', source: 'renewal.region' },
  { key: 'department', label: 'Department', source: 'renewal.department' },
  { key: 'branch', label: 'Invoicing Branch', source: 'renewal.branch' },
  { key: 'riskCode', label: 'Risk Code', source: 'renewal.riskCode' },
  { key: 'segment', label: 'Segment', source: 'renewal.segment' },
  { key: 'officer', label: 'Account Officer', source: 'renewal.officer' },
  { key: 'insurer', label: 'Insurance Company', source: 'renewal.insurer' },
  { key: 'bucket', label: 'Classification', source: 'renewal.bucket' },
  { key: 'disposition', label: 'Disposition', source: 'renewal.disposition' },
  { key: 'stage', label: 'Status', source: 'renewal.stage' },
];

function Criterion({
  label,
  source,
  value,
  onChange,
}: Readonly<{ label: string; source: string; value: string; onChange: (v: string) => void }>) {
  const companyId = useCompanyId();
  const options = useQuery({
    queryKey: ['renewal', 'code-set', source, companyId],
    queryFn: () => renewalApi.codeSet(source, companyId),
    enabled: companyId > 0,
    staleTime: 60_000,
  });
  return (
    <MultiSelectFilter
      label={label}
      options={options.data ?? []}
      value={value}
      onChange={onChange}
    />
  );
}

/** The Filters panel of a renewal list: the expiry range and the multi-select criteria. */
export function CandidateFilters({
  criteria,
  onChange,
}: Readonly<{
  criteria: RenewalFilters;
  onChange: (key: keyof RenewalFilters, value: string) => void;
}>) {
  return (
    <div className="worklist-filters form-grid">
      <Field label="Expiry From">
        {(id) => (
          <DateInput
            id={id}
            value={criteria.expiryFrom ?? ''}
            onChange={(e) => onChange('expiryFrom', e.target.value)}
          />
        )}
      </Field>
      <Field label="Expiry To">
        {(id) => (
          <DateInput
            id={id}
            value={criteria.expiryTo ?? ''}
            onChange={(e) => onChange('expiryTo', e.target.value)}
          />
        )}
      </Field>
      {CRITERIA.map((c) => (
        <Criterion
          key={c.key}
          label={c.label}
          source={c.source}
          value={(criteria[c.key] as string | undefined) ?? ''}
          onChange={(v) => onChange(c.key, v)}
        />
      ))}
    </div>
  );
}

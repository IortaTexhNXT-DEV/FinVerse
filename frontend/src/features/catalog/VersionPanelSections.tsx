import { useQuery } from '@tanstack/react-query';
import { Trash2 } from 'lucide-react';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import { productCatalogApi } from '@/api/productCatalog';
import type { InsurerRole } from '@/api/productCatalog';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { useCompanyId } from '@/context/workspaceContext';
import type { CoverageRow, InsurerRow, VersionForm } from './versionForm';
import { syncTerms } from './versionForm';

interface Props {
  form: VersionForm;
  lineCode: string;
  errors: Record<string, string>;
  readOnly: boolean;
  onChange: (form: VersionForm) => void;
}

const ROLES: InsurerRole[] = ['PANEL', 'LEAD', 'PARTICIPANT'];

/** A text or number cell input with an accessible label. */
function CellInput({
  label,
  value,
  readOnly,
  onChange,
  type = 'number',
}: Readonly<{
  label: string;
  value: string;
  readOnly: boolean;
  onChange: (value: string) => void;
  type?: 'number' | 'text';
}>) {
  return (
    <input
      className="input"
      aria-label={label}
      type={type}
      step={type === 'number' ? 'any' : undefined}
      value={value}
      disabled={readOnly}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}

function AddSelect({
  label,
  options,
  onAdd,
}: Readonly<{
  label: string;
  options: { value: string; text: string }[];
  onAdd: (v: string) => void;
}>) {
  const [value, setValue] = useState('');
  return (
    <div className="row">
      <select
        className="select"
        aria-label={label}
        value={value}
        onChange={(e) => setValue(e.target.value)}
      >
        <option value="">{label}</option>
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.text}
          </option>
        ))}
      </select>
      <Button
        size="sm"
        variant="secondary"
        disabled={value === ''}
        onClick={() => {
          onAdd(value);
          setValue('');
        }}
      >
        Add
      </Button>
    </div>
  );
}

/** Coverages and perils of the package version (PMADD01). */
export function VersionCoveragesSection({
  form,
  lineCode,
  errors,
  readOnly,
  onChange,
}: Readonly<Props>) {
  const coverages = useQuery({
    queryKey: ['catalog', 'coverages', lineCode],
    queryFn: () => productCatalogApi.coverages(lineCode),
  });
  const names = new Map((coverages.data ?? []).map((c) => [c.code, c]));
  const update = (index: number, change: Partial<CoverageRow>) => {
    const rows = form.coverages.map((c, i) => (i === index ? { ...c, ...change } : c));
    const next = { ...form, coverages: rows };
    onChange({ ...next, terms: syncTerms(next) });
  };
  const remove = (index: number) => {
    const next = { ...form, coverages: form.coverages.filter((_, i) => i !== index) };
    onChange({ ...next, terms: syncTerms(next) });
  };
  const add = (code: string) => {
    const row: CoverageRow = {
      coverageCode: code,
      included: true,
      optional: false,
      limitAmount: '',
      deductibleAmount: '',
      deductibleText: '',
    };
    const next = { ...form, coverages: [...form.coverages, row] };
    onChange({ ...next, terms: syncTerms(next) });
  };
  const available = (coverages.data ?? [])
    .filter(
      (c) => c.recordStatus === 'ACTIVE' && !form.coverages.some((r) => r.coverageCode === c.code),
    )
    .map((c) => ({ value: c.code, text: `${c.name}${c.basic ? ' (basic)' : ''}` }));
  return (
    <Card
      title="Coverages"
      flush
      actions={!readOnly && <AddSelect label="Add coverage" options={available} onAdd={add} />}
    >
      {errors.coverages && (
        <p className="field-error" style={{ padding: 'var(--space-3)' }}>
          {errors.coverages}
        </p>
      )}
      <DataTable<CoverageRow>
        rows={form.coverages}
        rowKey={(c) => c.coverageCode}
        emptyMessage="No coverage yet: add the basic coverage first."
        columns={[
          {
            key: 'c',
            header: 'Coverage',
            render: (c) => (
              <span>
                <strong>{names.get(c.coverageCode)?.name ?? c.coverageCode}</strong>
                {names.get(c.coverageCode)?.basic && <span className="tag">Basic</span>}
              </span>
            ),
          },
          {
            key: 'i',
            header: 'Included',
            render: (c) => (
              <input
                type="checkbox"
                aria-label={`Include ${c.coverageCode}`}
                checked={c.included}
                disabled={readOnly}
                onChange={(e) => update(form.coverages.indexOf(c), { included: e.target.checked })}
              />
            ),
          },
          {
            key: 'o',
            header: 'Optional',
            render: (c) => (
              <input
                type="checkbox"
                aria-label={`Optional ${c.coverageCode}`}
                checked={c.optional}
                disabled={readOnly}
                onChange={(e) => update(form.coverages.indexOf(c), { optional: e.target.checked })}
              />
            ),
          },
          {
            key: 'l',
            header: 'Limit',
            render: (c) => (
              <CellInput
                label={`Limit of ${c.coverageCode}`}
                value={c.limitAmount}
                readOnly={readOnly}
                onChange={(v) => update(form.coverages.indexOf(c), { limitAmount: v })}
              />
            ),
          },
          {
            key: 'd',
            header: 'Deductible',
            render: (c) => (
              <CellInput
                label={`Deductible of ${c.coverageCode}`}
                value={c.deductibleAmount}
                readOnly={readOnly}
                onChange={(v) => update(form.coverages.indexOf(c), { deductibleAmount: v })}
              />
            ),
          },
          {
            key: 'x',
            header: '',
            render: (c) =>
              !readOnly && (
                <Button
                  size="sm"
                  variant="ghost"
                  aria-label={`Remove ${c.coverageCode}`}
                  icon={<Trash2 size={14} />}
                  onClick={() => remove(form.coverages.indexOf(c))}
                />
              ),
          },
        ]}
      />
    </Card>
  );
}

/** Insurers of the package version: panel, lead or participants with their rates (PMADD02). */
export function VersionInsurersSection({ form, errors, readOnly, onChange }: Readonly<Props>) {
  const companyId = useCompanyId();
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
  });
  const update = (index: number, change: Partial<InsurerRow>) =>
    onChange({
      ...form,
      insurers: form.insurers.map((r, i) => (i === index ? { ...r, ...change } : r)),
    });
  const remove = (index: number) => {
    const next = { ...form, insurers: form.insurers.filter((_, i) => i !== index) };
    onChange({ ...next, terms: syncTerms(next) });
  };
  const add = (code: string) => {
    const row: InsurerRow = {
      insurerCode: code,
      role: 'PANEL',
      sharePercent: '',
      rate: '',
      minimumPremium: '',
      defaultBranchCode: '',
    };
    const next = { ...form, insurers: [...form.insurers, row] };
    onChange({ ...next, terms: syncTerms(next) });
  };
  const available = (insurers.data ?? [])
    .filter(
      (i) =>
        i.recordStatus === 'ACTIVE' && !form.insurers.some((r) => r.insurerCode === i.partyCode),
    )
    .map((i) => ({ value: i.partyCode, text: i.name }));
  const cell = (key: 'sharePercent' | 'rate' | 'minimumPremium', label: string) => ({
    key,
    header: label,
    render: (r: InsurerRow) => {
      const index = form.insurers.indexOf(r);
      return (
        <span>
          <CellInput
            label={`${label} of ${r.insurerCode}`}
            value={r[key]}
            readOnly={readOnly}
            onChange={(v) => update(index, { [key]: v })}
          />
          {errors[`insurers.${index}.${key}`] && (
            <span className="field-error">{errors[`insurers.${index}.${key}`]}</span>
          )}
        </span>
      );
    },
  });
  return (
    <Card
      title="Insurers"
      flush
      actions={!readOnly && <AddSelect label="Add insurer" options={available} onAdd={add} />}
    >
      <DataTable<InsurerRow>
        rows={form.insurers}
        rowKey={(r) => r.insurerCode}
        emptyMessage="No insurer yet: the package is placed with any insurer at the scheme rate."
        columns={[
          { key: 'n', header: 'Insurer', render: (r) => <strong>{r.insurerCode}</strong> },
          {
            key: 'r',
            header: 'Role',
            render: (r) => (
              <select
                className="select"
                aria-label={`Role of ${r.insurerCode}`}
                value={r.role}
                disabled={readOnly}
                onChange={(e) =>
                  update(form.insurers.indexOf(r), { role: e.target.value as InsurerRole })
                }
              >
                {ROLES.map((role) => (
                  <option key={role} value={role}>
                    {role}
                  </option>
                ))}
              </select>
            ),
          },
          cell('sharePercent', 'Share %'),
          cell('rate', 'Rate %'),
          cell('minimumPremium', 'Minimum Premium'),
          {
            key: 'x',
            header: '',
            render: (r) =>
              !readOnly && (
                <Button
                  size="sm"
                  variant="ghost"
                  aria-label={`Remove ${r.insurerCode}`}
                  icon={<Trash2 size={14} />}
                  onClick={() => remove(form.insurers.indexOf(r))}
                />
              ),
          },
        ]}
      />
    </Card>
  );
}

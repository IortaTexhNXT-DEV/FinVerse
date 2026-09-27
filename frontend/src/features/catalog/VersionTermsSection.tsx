import { useQuery } from '@tanstack/react-query';
import { productCatalogApi } from '@/api/productCatalog';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { TermRow, VersionForm } from './versionForm';
import { syncTerms } from './versionForm';

interface Props {
  form: VersionForm;
  lineCode: string;
  readOnly: boolean;
  onChange: (form: VersionForm) => void;
}

const keyOf = (t: TermRow) => `${t.insurerCode}:${t.coverageCode}`;

/**
 * Insurer x coverage terms of the package version (PMADD02): whether the insurer covers it, its
 * limit and deductible, and the warranties, clauses and exclusions of the clause library.
 */
export function VersionTermsSection({ form, lineCode, readOnly, onChange }: Readonly<Props>) {
  const clauses = useQuery({
    queryKey: ['catalog', 'clauses'],
    queryFn: productCatalogApi.clauses,
  });
  const options = (clauses.data ?? []).filter(
    (c) => c.recordStatus === 'ACTIVE' && (!c.lineCode || c.lineCode === lineCode),
  );
  const rows = syncTerms(form);
  const update = (row: TermRow, change: Partial<TermRow>) =>
    onChange({
      ...form,
      terms: rows.map((t) => (keyOf(t) === keyOf(row) ? { ...t, ...change } : t)),
    });
  const input = (
    row: TermRow,
    key: 'limitAmount' | 'deductibleAmount' | 'deductibleText',
    label: string,
  ) => (
    <input
      className="input"
      aria-label={`${label} of ${row.insurerCode} on ${row.coverageCode}`}
      type={key === 'deductibleText' ? 'text' : 'number'}
      value={row[key]}
      disabled={readOnly}
      onChange={(e) => update(row, { [key]: e.target.value })}
    />
  );
  return (
    <Card title="Insurer Terms" flush>
      <DataTable<TermRow>
        rows={rows}
        rowKey={keyOf}
        emptyMessage="Add insurers and included coverages to maintain their terms."
        columns={[
          { key: 'i', header: 'Insurer', render: (t) => <strong>{t.insurerCode}</strong> },
          { key: 'c', header: 'Coverage', render: (t) => t.coverageCode },
          {
            key: 'n',
            header: 'Covered',
            render: (t) => (
              <input
                type="checkbox"
                aria-label={`${t.insurerCode} covers ${t.coverageCode}`}
                checked={t.included}
                disabled={readOnly}
                onChange={(e) => update(t, { included: e.target.checked })}
              />
            ),
          },
          { key: 'l', header: 'Limit', render: (t) => input(t, 'limitAmount', 'Limit') },
          {
            key: 'd',
            header: 'Deductible',
            render: (t) => input(t, 'deductibleAmount', 'Deductible'),
          },
          {
            key: 'w',
            header: 'Deductible Wording',
            render: (t) => input(t, 'deductibleText', 'Wording'),
          },
          {
            key: 'k',
            header: 'Clauses',
            render: (t) => (
              <select
                multiple
                className="select"
                aria-label={`Clauses of ${t.insurerCode} on ${t.coverageCode}`}
                value={t.clauseCodes}
                disabled={readOnly}
                onChange={(e) =>
                  update(t, { clauseCodes: [...e.target.selectedOptions].map((o) => o.value) })
                }
              >
                {options.map((c) => (
                  <option key={c.code} value={c.code}>
                    {c.title}
                  </option>
                ))}
              </select>
            ),
          },
        ]}
      />
    </Card>
  );
}

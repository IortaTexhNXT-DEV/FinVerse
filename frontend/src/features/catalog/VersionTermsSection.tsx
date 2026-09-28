import { useQuery } from '@tanstack/react-query';
import { productCatalogApi } from '@/api/productCatalog';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { TermRow, VersionForm } from './versionForm';
import { syncTerms } from './versionForm';
import { CoverageName, InsurerName } from '@/components/broking/LovLabel';
import { formatAmount } from '@/utils/format';

const DASH = <span className="muted">—</span>;

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
  const clauseTitles = (codes: string[]) =>
    codes.length === 0 ? (
      DASH
    ) : (
      <span style={{ whiteSpace: 'normal' }}>
        {codes
          .map((code) => (clauses.data ?? []).find((c) => c.code === code)?.title ?? code)
          .join('; ')}
      </span>
    );
  const update = (row: TermRow, change: Partial<TermRow>) =>
    onChange({
      ...form,
      terms: rows.map((t) => (keyOf(t) === keyOf(row) ? { ...t, ...change } : t)),
    });
  const input = (
    row: TermRow,
    key: 'limitAmount' | 'deductibleAmount' | 'deductibleText',
    label: string,
  ) => {
    if (readOnly) {
      if (row[key] === '') {
        return DASH;
      }
      return key === 'deductibleText' ? (
        <span style={{ whiteSpace: 'normal', overflowWrap: 'anywhere' }}>{row[key]}</span>
      ) : (
        formatAmount(row[key])
      );
    }
    return (
      <input
        className="input"
        aria-label={`${label} of ${row.insurerCode} on ${row.coverageCode}`}
        type={key === 'deductibleText' ? 'text' : 'number'}
        value={row[key]}
        onChange={(e) => update(row, { [key]: e.target.value })}
      />
    );
  };
  return (
    <Card title="Insurer Terms" flush>
      <DataTable<TermRow>
        rows={rows}
        rowKey={keyOf}
        emptyMessage="Add insurers and included coverages to maintain their terms."
        columns={[
          {
            key: 'i',
            header: 'Insurer',
            render: (t) => (
              <strong>
                <InsurerName code={t.insurerCode} />
              </strong>
            ),
          },
          {
            key: 'c',
            header: 'Coverage',
            render: (t) => <CoverageName line={lineCode} code={t.coverageCode} />,
          },
          {
            key: 'n',
            header: 'Covered',
            render: (t) =>
              readOnly ? (
                t.included ? (
                  'Yes'
                ) : (
                  'No'
                )
              ) : (
                <input
                  type="checkbox"
                  aria-label={`${t.insurerCode} covers ${t.coverageCode}`}
                  checked={t.included}
                  disabled={readOnly}
                  onChange={(e) => update(t, { included: e.target.checked })}
                />
              ),
          },
          {
            key: 'l',
            header: 'Limit',
            numeric: readOnly,
            render: (t) => input(t, 'limitAmount', 'Limit'),
          },
          {
            key: 'd',
            header: 'Deductible',
            numeric: readOnly,
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
            render: (t) =>
              readOnly ? (
                clauseTitles(t.clauseCodes)
              ) : (
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

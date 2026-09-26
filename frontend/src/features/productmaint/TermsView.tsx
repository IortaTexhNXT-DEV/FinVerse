import { Fragment } from 'react';
import type { CoverageTerm, InsurerLine, PackageTerms } from '@/api/productmaint';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { formatAmount, formatDate } from '@/utils/format';

function deductible(c: CoverageTerm): string {
  const parts = [
    c.deductibleText,
    c.deductibleAmount === undefined ? undefined : formatAmount(c.deductibleAmount),
    c.deductiblePercent === undefined ? undefined : `${c.deductiblePercent}%`,
  ].filter((p) => p !== undefined && p !== '');
  return parts.length === 0 ? '—' : parts.join(' / ');
}

const COVERAGE_COLUMNS: Column<CoverageTerm>[] = [
  {
    key: 'code',
    header: 'Coverage',
    render: (c) => <span className="mono">{c.coverageCode}</span>,
  },
  { key: 'included', header: 'Included', render: (c) => (c.included ? 'Yes' : 'No') },
  {
    key: 'limit',
    header: 'Limit',
    numeric: true,
    render: (c) => (c.limitAmount === undefined ? '—' : formatAmount(c.limitAmount)),
  },
  { key: 'deductible', header: 'Deductible', render: deductible },
];

const INSURER_COLUMNS: Column<InsurerLine>[] = [
  { key: 'insurer', header: 'Insurer', render: (i) => <strong>{i.insurerCode}</strong> },
  { key: 'role', header: 'Role', render: (i) => i.role ?? 'Panel' },
  { key: 'share', header: 'Share %', numeric: true, render: (i) => i.sharePercent ?? '—' },
  { key: 'rate', header: 'Rate %', numeric: true, render: (i) => i.rate ?? '—' },
  {
    key: 'min',
    header: 'Minimum',
    numeric: true,
    render: (i) => (i.minimumPremium === undefined ? '—' : formatAmount(i.minimumPremium)),
  },
];

/** Read-only package terms: sections, coverages, rate scheme and term, insurers (BRPM.008/015). */
export function TermsView({ terms, title }: Readonly<{ terms: PackageTerms; title: string }>) {
  const s = terms.scheme;
  const d = terms.dates;
  return (
    <>
      <Card title={title}>
        <dl className="detail-list">
          {terms.sections.map((section, i) => (
            <Fragment key={`${section.heading ?? 'section'}-${i + 1}`}>
              <dt>{section.heading ?? 'Details'}</dt>
              <dd>{section.text}</dd>
            </Fragment>
          ))}
          <dt>Rate / minimum premium</dt>
          <dd>
            {s.defaultRate ?? '—'}% /{' '}
            {s.minimumPremium === undefined ? '—' : formatAmount(s.minimumPremium)}
          </dd>
          <dt>Commission / TSI limit</dt>
          <dd>
            {s.commissionRate ?? '—'}% /{' '}
            {s.maxSumInsured === undefined ? '—' : formatAmount(s.maxSumInsured)}
          </dd>
          <dt>Computation basis</dt>
          <dd>{s.ratingBasisNote ?? '—'}</dd>
          <dt>Effective from</dt>
          <dd>{formatDate(d.effectiveFrom)}</dd>
          <dt>Package term</dt>
          <dd>
            {formatDate(d.packageStartDate)} – {formatDate(d.packageEndDate)}
            {d.anniversaryDate && ` (anniversary ${formatDate(d.anniversaryDate)})`}
          </dd>
        </dl>
      </Card>
      <Card title="Coverages" flush>
        <DataTable<CoverageTerm>
          rows={terms.coverages}
          rowKey={(c) => c.coverageCode}
          emptyMessage="No coverage listed"
          columns={COVERAGE_COLUMNS}
        />
      </Card>
      <Card title="Insurers" flush>
        <DataTable<InsurerLine>
          rows={terms.insurers}
          rowKey={(i) => i.insurerCode}
          emptyMessage="No insurer listed"
          columns={INSURER_COLUMNS}
        />
      </Card>
    </>
  );
}

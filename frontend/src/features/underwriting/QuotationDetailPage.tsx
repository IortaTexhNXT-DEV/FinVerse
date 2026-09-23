import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { underwritingApi } from '@/api/underwriting';
import type { Iteration, Quotation } from '@/api/underwriting';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, humanize } from '@/utils/format';
import { QuotationActions } from './QuotationActions';

function Terms({ quotation: q }: Readonly<{ quotation: Quotation }>) {
  const facts: [string, string][] = [
    ['Product', `${q.productCode} – ${q.productName}`],
    ['Customer', `${q.customerCode} – ${q.customerName}`],
    [
      'Source',
      q.intermediaryName === undefined
        ? 'Direct'
        : `${humanize(q.sourceType)}: ${q.intermediaryName}`,
    ],
    ['Issued', formatDate(q.issueDate)],
    ['Valid until', `${formatDate(q.expiryDate)} (${String(q.validityDays)} days)`],
    ['Proposed period', `${formatDate(q.periodFrom)} – ${formatDate(q.periodTo)}`],
    ['Our share', `${String(q.sharePct)}%`],
    ['Brokerage', `${String(q.commissionRate)}%`],
    ['Prepared by', q.createdBy],
    ['Decided by', q.decidedBy ?? '—'],
  ];
  return (
    <Card title="Terms">
      <dl className="form-grid" style={{ margin: 0 }}>
        {facts.map(([label, value]) => (
          <div key={label}>
            <dt className="muted">{label}</dt>
            <dd style={{ margin: 0, fontWeight: 600 }}>{value}</dd>
          </div>
        ))}
      </dl>
      {q.decisionReason !== undefined && (
        <div className="alert warning" style={{ marginTop: 12 }}>
          Rejected: {q.decisionReason}
        </div>
      )}
      {q.convertedPolicyId !== undefined && (
        <p style={{ marginBottom: 0 }}>
          Converted into{' '}
          <Link to={`/underwriting/policies/${String(q.convertedPolicyId)}`}>policy</Link>.
        </p>
      )}
    </Card>
  );
}

/** Quotation view: terms, negotiation iterations and workflow actions. */
export default function QuotationDetailPage() {
  const id = Number(useParams().id);
  const query = useQuery({
    queryKey: ['quotation', id],
    queryFn: () => underwritingApi.quotation(id),
  });

  if (query.data === undefined) {
    return query.error ? (
      <ErrorAlert error={query.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const q = query.data;
  return (
    <div className="stack">
      <PageHeader
        section="Underwriting · Quotation"
        title={q.quotationNo}
        description={q.insuredName}
        actions={
          <>
            <StatusBadge status={q.status} />
            <QuotationActions quotation={q} />
          </>
        }
      />
      <Terms quotation={q} />
      <Card title="Iterations" flush>
        <DataTable<Iteration>
          rows={q.iterations}
          rowKey={(it) => it.iterationNo}
          caption="Iterations"
          columns={[
            { key: 'no', header: '#', render: (it) => it.iterationNo },
            {
              key: 'si',
              header: 'Sum insured',
              numeric: true,
              render: (it) => <Amount value={it.sumInsured} />,
            },
            {
              key: 'gross',
              header: 'Gross',
              numeric: true,
              render: (it) => <Amount value={it.grossPremium} />,
            },
            {
              key: 'disc',
              header: 'Discount',
              numeric: true,
              render: (it) => <Amount value={it.discount} />,
            },
            {
              key: 'load',
              header: 'Loading',
              numeric: true,
              render: (it) => <Amount value={it.loading} />,
            },
            {
              key: 'net',
              header: 'Net (100%)',
              numeric: true,
              render: (it) => <Amount value={it.netPremium} />,
            },
            {
              key: 'our',
              header: 'Our net',
              numeric: true,
              render: (it) => <Amount value={it.ourNetPremium} />,
            },
            {
              key: 'brk',
              header: 'Brokerage',
              numeric: true,
              render: (it) => <Amount value={it.brokerage} />,
            },
            { key: 'rem', header: 'Remarks', render: (it) => it.remarks ?? '' },
            { key: 'by', header: 'By', render: (it) => it.createdBy },
          ]}
        />
      </Card>
    </div>
  );
}

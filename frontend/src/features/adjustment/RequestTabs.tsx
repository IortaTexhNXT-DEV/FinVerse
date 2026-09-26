import { useQuery } from '@tanstack/react-query';
import { Fragment } from 'react';
import { Link } from 'react-router-dom';
import { workflowApi } from '@/api/workflow';
import { StageTimeline } from '@/components/broking/StageTimeline';
import { workflowKey } from '@/components/broking/workflowKey';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { formatAmount, formatDate, formatDateTime, humanize } from '@/utils/format';
import { adjustmentApi, REQUEST_ENTITY } from './api';
import type { EndorsementRequest, GlLine } from './api';
import { RecomputePreview } from './RecomputePreview';

type Row = [string, string | undefined];

function Facts({ rows }: Readonly<{ rows: Row[] }>) {
  return (
    <dl className="detail-list">
      {rows.map(([label, value]) => (
        <Fragment key={label}>
          <dt>{label}</dt>
          <dd>{value === undefined || value === '' ? '—' : value}</dd>
        </Fragment>
      ))}
    </dl>
  );
}

const amountText = (value: number | undefined) =>
  value === undefined ? undefined : formatAmount(value);

/** The request as entered and as processed (ADJID.020-022). */
export function DetailsTab({ request: r }: Readonly<{ request: EndorsementRequest }>) {
  return (
    <div className="grid-2">
      <Card title="Request">
        <Facts rows={requestRows(r)} />
      </Card>
      <Card title="Processing">
        <Facts rows={processingRows(r)} />
      </Card>
    </div>
  );
}

const optionalLabel = (code: string | undefined) =>
  code === undefined ? undefined : humanize(code);

function requestRows(r: EndorsementRequest): Row[] {
  const t = r.terms;
  const period =
    t.newPeriodFrom === undefined
      ? undefined
      : `${formatDate(t.newPeriodFrom)} – ${formatDate(t.newPeriodTo)}`;
  return [
    ['Endorsement Type', humanize(t.endorsementType)],
    ['Request Type', optionalLabel(t.requestType)],
    ['Reason', optionalLabel(t.reasonCode)],
    ['Effective Date', formatDate(t.effectiveDate)],
    ['Refund Basis', humanize(t.refundBasis)],
    ['Sum Insured Change', amountText(t.sumInsuredChange)],
    ['Insurer Endorsement Ref.', t.endorsementRef],
    ['New Period', period],
    ['Description', t.description],
    ['Instructions', t.instructions],
    ['Duplicate Justification', r.control.duplicateOverride],
    ['Baseline Justification', r.control.baselineOverride],
    ['Quotation', r.control.quotationRef],
  ];
}

function done(by: string | undefined, at: string | undefined): string | undefined {
  return at === undefined ? undefined : `${by ?? ''} · ${formatDateTime(at)}`;
}

function processingRows(r: EndorsementRequest): Row[] {
  const t = r.trail;
  return [
    ['Requested By', done(r.createdBy, r.createdAt)],
    ['Submitted', done(t.submittedBy, t.submittedAt)],
    ['Validated', done(t.validatedBy, t.validatedAt)],
    ['Approved', done(t.approvedBy, t.approvedAt)],
    ['Posted', done(t.postedBy, t.postedAt)],
    ['Completed', t.completedAt === undefined ? undefined : formatDateTime(t.completedAt)],
    ['Aging', `${String(r.agingDays)} day(s)`],
    ['Endorsement Slip', r.control.slipNo],
  ];
}

/** Live recompute of an open request, or the recompute recorded when it was posted. */
export function RecomputeTab({ request }: Readonly<{ request: EndorsementRequest }>) {
  const recompute = useQuery({
    queryKey: ['adjustment', 'recompute', request.id, request.stage],
    queryFn: () => adjustmentApi.recompute(request.id),
    retry: false,
  });
  if (recompute.data === undefined) {
    return recompute.error ? (
      <ErrorAlert error={recompute.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  return <RecomputePreview recompute={recompute.data} />;
}

const GL_COLUMNS: Column<GlLine>[] = [
  { key: 'batch', header: 'Journal', render: (l) => l.batchNo },
  {
    key: 'account',
    header: 'GL Account',
    render: (l) => (
      <>
        {l.accountCode}
        <div className="muted">{l.accountName}</div>
      </>
    ),
  },
  { key: 'party', header: 'Party', render: (l) => l.partyCode ?? '—' },
  {
    key: 'debit',
    header: 'Debit',
    numeric: true,
    render: (l) => (l.side === 'DEBIT' ? <Amount value={l.amount} /> : ''),
  },
  {
    key: 'credit',
    header: 'Credit',
    numeric: true,
    render: (l) => (l.side === 'CREDIT' ? <Amount value={l.amount} /> : ''),
  },
];

/** What the posting produced: endorsement, invoice, service invoices, GL entries (ADJID.017). */
export function AccountingTab({ request: r }: Readonly<{ request: EndorsementRequest }>) {
  const journal = useQuery({
    queryKey: ['adjustment', 'journal', r.id, r.journals.length],
    queryFn: () => adjustmentApi.journal(r.id),
  });
  const o = r.outcome;
  return (
    <div className="stack">
      <Card title="Posting">
        <Facts
          rows={[
            ['Validation Batch', o.batchNo],
            ['Endorsement No.', o.endorsementNo],
            ['Invoice Booked', o.newInvoiceNo],
            ['Service Invoices', o.serviceInvoices],
            ['AR Insurer', amountText(o.arInsurerAmount)],
            ['Excess to Unapplied', amountText(o.excessAmount)],
            ['Unapplied Item', o.unappliedRef],
          ]}
        />
        {o.newInvoiceNo && (
          <Link to={`/operations/invoices/${encodeURIComponent(o.newInvoiceNo)}`}>
            Open invoice {o.newInvoiceNo}
          </Link>
        )}
      </Card>
      <Card title="GL Entries" flush>
        <ErrorAlert error={journal.error} />
        <DataTable
          caption="GL entries of the request"
          columns={GL_COLUMNS}
          rows={journal.data ?? []}
          rowKey={(l) =>
            `${l.batchNo}-${l.accountCode}-${l.side}-${String(l.amount)}-${l.partyCode ?? ''}`
          }
          loading={journal.isLoading}
          emptyMessage="Nothing posted yet"
        />
      </Card>
    </div>
  );
}

/** Status history of the request (ADJID.022). */
export function HistoryTab({ id }: Readonly<{ id: number }>) {
  const detail = useQuery({
    queryKey: workflowKey(REQUEST_ENTITY, id),
    queryFn: () => workflowApi.byRecord(REQUEST_ENTITY, id),
  });
  return (
    <Card title="History">
      <StageTimeline history={detail.data?.history ?? []} />
    </Card>
  );
}

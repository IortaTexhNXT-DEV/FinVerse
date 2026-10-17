import { useQuery } from '@tanstack/react-query';
import { Fragment } from 'react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { workflowApi } from '@/api/workflow';
import { HistoryTable } from '@/components/broking/HistoryTable';
import { workflowKey } from '@/components/broking/workflowKey';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import {
  formatAmount,
  formatDate,
  formatDateTime,
  formatAging,
  formatPeriod,
} from '@/utils/format';
import { InsurerName, ProductName } from '@/components/broking/LovLabel';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { useRequestLabels } from './useRequestLabels';
import { adjustmentApi, REQUEST_ENTITY } from './api';
import type { EndorsementRequest, GlLine } from './api';
import { RecomputePreview } from './RecomputePreview';
import { displayNameOf } from '@/api/users';
import { LoadingPanel } from '@/components/ui/LoadingPanel';

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
  const labels = useRequestLabels();
  return (
    <div className="grid-2">
      <Card title="Request">
        <Facts rows={requestRows(r, labels)} />
      </Card>
      <Card title="Processing">
        <Facts rows={processingRows(r)} />
      </Card>
    </div>
  );
}

type Labels = ReturnType<typeof useRequestLabels>;

const BASIS_LABELS: Record<string, string> = {
  PRO_RATA: 'Pro-rata',
  SHORT_PERIOD: 'Short Period',
};

function requestRows(r: EndorsementRequest, labels: Labels): Row[] {
  const t = r.terms;
  const period =
    t.newPeriodFrom === undefined ? undefined : formatPeriod(t.newPeriodFrom, t.newPeriodTo);
  return [
    ['Endorsement Type', labels.type(t.endorsementType)],
    ['Request Type', labels.requestType(t.requestType)],
    ['Reason', labels.reason(t.reasonCode)],
    ['Effective Date', formatDate(t.effectiveDate)],
    ['Refund Basis', BASIS_LABELS[t.refundBasis] ?? t.refundBasis],
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
  return at === undefined ? undefined : `${displayNameOf(by)} · ${formatDateTime(at)}`;
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
    ['Aging', formatAging(r.agingDays)],
    ['Endorsement Slip', r.control.slipNo],
  ];
}

/**
 * The policy and placement the request is against (ADJID.001/020): insurer policy number, ARN,
 * invoice, placement slip, client, insurer, product and cover, with links to the account and the
 * invoice.
 */
export function PolicyTab({ request: r }: Readonly<{ request: EndorsementRequest }>) {
  const p = r.policy;
  const gross =
    p?.grossPremium === undefined ? undefined : `${p.currency} ${formatAmount(p.grossPremium)}`;
  const rows: [string, ReactNode][] = [
    [
      'Policy No.',
      p?.policyNo ?? r.invoice.policyNo ?? <span className="muted">Not yet issued</span>,
    ],
    ['ARN', <ReferenceChip key="arn" value={r.invoice.arn} />],
    [
      'Invoice',
      <Link key="inv" to={`/operations/invoices/${encodeURIComponent(r.invoice.invoiceNo)}`}>
        {r.invoice.invoiceNo}
      </Link>,
    ],
    ['Placement Slip', p?.slipNo],
    ['Client', r.invoice.assuredName],
    ['Insurer', <InsurerName key="ins" code={r.invoice.insurerCode} />],
    ['Product', <ProductName key="prd" code={p?.productCode} />],
    ['Period of Cover', formatPeriod(p?.periodFrom, p?.periodTo)],
    ['Gross Premium', gross],
  ];
  return (
    <Card title="Policy and Placement">
      <dl className="detail-list">
        {rows.map(([label, value]) => (
          <Fragment key={label}>
            <dt>{label}</dt>
            <dd>{value === undefined || value === '' ? '—' : value}</dd>
          </Fragment>
        ))}
      </dl>
      {p?.accountId !== undefined && (
        <Link to={`/accounts/${String(p.accountId)}`}>Open account {r.invoice.arn}</Link>
      )}
    </Card>
  );
}

/** Live recompute of an open request, or the recompute recorded when it was posted. */
export function RecomputeTab({ request }: Readonly<{ request: EndorsementRequest }>) {
  const recompute = useQuery({
    queryKey: ['adjustment', 'recompute', request.id, request.stage],
    queryFn: () => adjustmentApi.recompute(request.id),
    retry: false,
  });
  if (recompute.data === undefined) {
    return recompute.error ? <ErrorAlert error={recompute.error} /> : <LoadingPanel />;
  }
  return <RecomputePreview recompute={recompute.data} />;
}

const GL_COLUMNS: Column<GlLine>[] = [
  { key: 'batch', header: 'Journal', kind: 'code', render: (l) => l.batchNo },
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
  { key: 'party', header: 'Party', kind: 'code', render: (l) => l.partyCode ?? '' },
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
      <HistoryTable history={detail.data?.history ?? []} terminal={detail.data?.stageTerminal} />
    </Card>
  );
}

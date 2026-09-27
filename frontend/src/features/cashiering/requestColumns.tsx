import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import type { CollectorRequest, PaymentReversal, RefundValidation } from './requestsApi';

/** Columns of the Incoming Requests queues (wave C1-C). */

const who = (by: string, at: string) => (
  <>
    {by}
    <div className="muted">{formatDateTime(at)}</div>
  </>
);

export const REQUEST_COLUMNS: Column<CollectorRequest>[] = [
  {
    key: 'no',
    header: 'Request No.',
    render: (r) => (
      <>
        <strong>{r.requestNo}</strong>
        <div className="muted">{r.unappliedRef}</div>
      </>
    ),
  },
  {
    key: 'action',
    header: 'Collector Asks',
    render: (r) => (
      <>
        {humanize(r.action)}
        {r.invoiceNo !== undefined && <div className="muted">{r.invoiceNo}</div>}
      </>
    ),
  },
  { key: 'payor', header: 'Payor', render: (r) => r.payorName ?? '' },
  {
    key: 'balance',
    header: 'Unapplied',
    numeric: true,
    render: (r) => <Amount value={r.balance} />,
  },
  {
    key: 'amount',
    header: 'Amount',
    numeric: true,
    render: (r) => (r.amount === undefined ? 'Whole balance' : <Amount value={r.amount} />),
  },
  { key: 'by', header: 'Requested By', render: (r) => who(r.requestedBy, r.requestedAt) },
  { key: 'remarks', header: 'Remarks', render: (r) => r.decisionNote ?? r.remarks ?? '' },
  { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
];

export const VALIDATION_COLUMNS: Column<RefundValidation>[] = [
  {
    key: 'no',
    header: 'Validation No.',
    render: (t) => (
      <>
        <strong>{t.taskNo}</strong>
        <div className="muted">
          {t.sourceModule} {t.sourceRef}
        </div>
      </>
    ),
  },
  { key: 'invoice', header: 'Cancelled Invoice', render: (t) => t.invoiceNo ?? t.clientCode ?? '' },
  { key: 'ar', header: 'AR of the Payment', render: (t) => t.arNo ?? '' },
  { key: 'amount', header: 'Refund', numeric: true, render: (t) => <Amount value={t.amount} /> },
  { key: 'by', header: 'Requested By', render: (t) => who(t.requestedBy, t.requestedAt) },
  { key: 'result', header: 'New AR / Remarks', render: (t) => t.newArNo ?? t.resultRemarks ?? '' },
  { key: 'status', header: 'Status', render: (t) => <StatusBadge status={t.status} /> },
];

export const REVERSAL_COLUMNS: Column<PaymentReversal>[] = [
  {
    key: 'no',
    header: 'Reversal No.',
    render: (r) => (
      <>
        <strong>{r.requestNo}</strong>
        <div className="muted">
          {r.sourceModule} {r.sourceRef}
        </div>
      </>
    ),
  },
  { key: 'invoice', header: 'Invoice No.', render: (r) => r.invoiceNo },
  { key: 'receipt', header: 'Receipt', render: (r) => r.receiptNo },
  {
    key: 'amount',
    header: 'Amount',
    numeric: true,
    render: (r) => (r.amount === undefined ? 'Whole application' : <Amount value={r.amount} />),
  },
  { key: 'date', header: 'Value Date', render: (r) => formatDate(r.valueDate) },
  { key: 'reason', header: 'Reason', render: (r) => r.decisionNote ?? r.reason ?? '' },
  { key: 'by', header: 'Requested By', render: (r) => who(r.requestedBy, r.requestedAt) },
  { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
];

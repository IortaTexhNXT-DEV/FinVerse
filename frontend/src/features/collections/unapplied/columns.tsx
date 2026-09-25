import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import type { CashieringRequest, UnappliedRow } from './api';
import { ACTION_LABELS } from './labels';
import { CashieringCell } from './UnappliedParts';

/** Table columns of the unapplied-payment screens (BRCLXN.036, 040). */

/** The columns of the collector list (BRCLXN.036). */
export const UNAPPLIED_COLUMNS: Column<UnappliedRow>[] = [
  {
    key: 'ref',
    header: 'Unapplied Reference',
    render: (r) => (
      <>
        <strong>{r.unappliedRef}</strong>
        <div className="muted">{r.payor}</div>
      </>
    ),
  },
  {
    key: 'date',
    header: 'Payment Date',
    render: (r) => (
      <>
        {formatDate(r.paymentDate)}
        <div className="muted">{r.ageDays} days</div>
      </>
    ),
  },
  {
    key: 'txn',
    header: 'Transaction',
    render: (r) => (
      <>
        {r.transactionNo}
        <div className="muted">
          {[r.paymentType, r.bankCode, r.checkNo].filter(Boolean).join(' · ')}
        </div>
      </>
    ),
  },
  {
    key: 'amount',
    header: 'Paid',
    numeric: true,
    render: (r) => <Amount value={r.amount} />,
  },
  {
    key: 'balance',
    header: 'Unapplied',
    numeric: true,
    render: (r) => <Amount value={r.balance} />,
  },
  {
    key: 'match',
    header: 'Matched Account',
    render: (r) => (
      <>
        {r.invoiceNo ?? r.clientCode ?? 'Not matched'}
        {r.account?.assuredName !== undefined && (
          <div className="muted">{r.account.assuredName}</div>
        )}
      </>
    ),
  },
  {
    key: 'segment',
    header: 'Segment / AO',
    render: (r) => (
      <>
        {r.account?.segment ?? r.salesUnit ?? ''}
        {r.account?.aoUsername !== undefined && <div className="muted">{r.account.aoUsername}</div>}
      </>
    ),
  },
  {
    key: 'disposition',
    header: 'Collector Disposition',
    render: (r) =>
      r.dispositionCode === undefined ? (
        <span className="muted">None yet</span>
      ) : (
        <>
          {humanize(r.dispositionCode)}
          <div className="muted">
            {r.dispositionBy}, {formatDateTime(r.dispositionAt)}
          </div>
        </>
      ),
  },
  { key: 'cashiering', header: 'Cashiering', render: (r) => <CashieringCell row={r} /> },
];

/** The columns of the requests sent to Cashiering (status view). */
export const REQUEST_COLUMNS: Column<CashieringRequest>[] = [
  {
    key: 'ref',
    header: 'Unapplied Reference',
    render: (r) => (
      <>
        <strong>{r.unappliedRef}</strong>
        <div className="muted">{r.payor}</div>
      </>
    ),
  },
  {
    key: 'action',
    header: 'Request',
    render: (r) => (
      <>
        {ACTION_LABELS[r.action]}
        {r.invoiceNo !== undefined && <div className="muted">{r.invoiceNo}</div>}
      </>
    ),
  },
  {
    key: 'amount',
    header: 'Amount',
    numeric: true,
    render: (r) =>
      r.amount === undefined ? (
        <span className="muted">Whole balance</span>
      ) : (
        <Amount value={r.amount} />
      ),
  },
  {
    key: 'by',
    header: 'Requested By',
    render: (r) => (
      <>
        {r.requestedBy}
        <div className="muted">{formatDateTime(r.requestedAt)}</div>
      </>
    ),
  },
  {
    key: 'cashiering',
    header: 'Cashiering Reference',
    render: (r) => (
      <>
        {r.cashieringRef}
        {r.statusMessage !== undefined && <div className="muted">{r.statusMessage}</div>}
      </>
    ),
  },
  { key: 'file', header: 'Application File', render: (r) => r.fileRunNo ?? '' },
  { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
];

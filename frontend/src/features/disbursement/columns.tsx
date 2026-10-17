import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import type { PaymentRequest, VoucherSummary } from './api';
import { MODE_LABELS } from './labels';

/** Columns of the payment requests (DIS 2.4.0-2.4.4). */
export const REQUEST_COLUMNS: Column<PaymentRequest>[] = [
  {
    key: 'no',
    header: 'Request No.',
    render: (r) => (
      <>
        <strong>{r.requestNo}</strong>
        <div className="dsb-muted">{r.rfpNo ?? r.sourceRef}</div>
      </>
    ),
  },
  { key: 'source', header: 'Source', render: (r) => humanize(r.sourceModule) },
  { key: 'type', header: 'Type', render: (r) => humanize(r.disbursementType) },
  {
    key: 'payee',
    header: 'Payee',
    render: (r) => (
      <>
        {r.payeeName ?? r.payeeCode}
        <div className="dsb-muted">{r.payeeCode}</div>
      </>
    ),
  },
  { key: 'cur', header: 'Currency', render: (r) => r.currency },
  { key: 'amount', header: 'Amount', numeric: true, render: (r) => <Amount value={r.amount} /> },
  { key: 'received', header: 'Received', render: (r) => formatDateTime(r.receivedAt) },
  { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
];

/** Columns of the vouchers (DIS 2.4.4, 2.13.0). */
export const VOUCHER_COLUMNS: Column<VoucherSummary>[] = [
  {
    key: 'dv',
    header: 'DV No.',
    render: (v) => (
      <>
        <strong>{v.dvNo}</strong>
        <div className="dsb-muted">{formatDate(v.createdAt)}</div>
      </>
    ),
  },
  { key: 'type', header: 'Type', render: (v) => humanize(v.disbursementType) },
  {
    key: 'payee',
    header: 'Payee',
    render: (v) => (
      <>
        {v.payeeName}
        <div className="dsb-muted">{v.payeeCode}</div>
      </>
    ),
  },
  { key: 'mode', header: 'Mode', render: (v) => (v.mode ? MODE_LABELS[v.mode] : '') },
  { key: 'net', header: 'Net Amount', numeric: true, render: (v) => <Amount value={v.net} /> },
  { key: 'cur', header: 'Currency', render: (v) => v.currency },
  {
    key: 'flags',
    header: 'Flags',
    render: (v) => (
      <span className="tag-list">
        {v.autoCreated && <span className="tag">Automatic</span>}
        {v.proformaEdited && <span className="tag">Entry Edited</span>}
        {v.postingStatus === 'FAILED' && <span className="tag">Posting Failed</span>}
      </span>
    ),
  },
  { key: 'stage', header: 'Status', render: (v) => <StatusBadge status={v.stage} /> },
];

import { Link } from 'react-router-dom';
import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import type { Application, ReceiptAction, ReceiptDetail, ReceiptLine } from './cashieringApi';
import { allocationRows, componentLabel, journalRows } from './cashieringLogic';
import type { JournalRow, ReceiptTabId } from './cashieringLogic';

const APPLICATION_COLUMNS: Column<Application>[] = [
  {
    key: 'invoice',
    header: 'Invoice',
    render: (a) => (
      <>
        <Link to={`/operations/invoices/${encodeURIComponent(a.invoiceNo)}`}>{a.invoiceNo}</Link>
        <span className="cell-sub">{a.arn}</span>
      </>
    ),
  },
  { key: 'ref', header: 'Reference', render: (a) => <code>{a.reference}</code> },
  {
    key: 'alloc',
    header: 'By Component',
    render: (a) =>
      allocationRows(a.allocation)
        .map((r) => `${componentLabel(r.component)} ${r.amount.toFixed(2)}`)
        .join(' · '),
  },
  { key: 'amount', header: 'Applied', numeric: true, render: (a) => <Amount value={a.amount} /> },
  {
    key: 'comm',
    header: 'Commission Realized',
    numeric: true,
    render: (a) => <Amount value={a.realizedCommission} />,
  },
  { key: 'date', header: 'Value Date', render: (a) => formatDate(a.valueDate) },
  { key: 'status', header: 'Status', render: (a) => <StatusBadge status={a.status} /> },
];

const LINE_COLUMNS: Column<ReceiptLine>[] = [
  { key: 'invoice', header: 'Invoice / Item', render: (l) => l.invoiceNo ?? l.description ?? '' },
  { key: 'insurer', header: 'Insurer', render: (l) => l.insurerCode ?? '' },
  { key: 'gross', header: 'Gross', numeric: true, render: (l) => <Amount value={l.gross} /> },
  { key: 'vat', header: 'VAT', numeric: true, render: (l) => <Amount value={l.vat} /> },
  { key: 'wtax', header: 'WTAX', numeric: true, render: (l) => <Amount value={l.wtax} /> },
  { key: 'net', header: 'Net', numeric: true, render: (l) => <Amount value={l.net} /> },
];

const ACTION_COLUMNS: Column<ReceiptAction>[] = [
  { key: 'no', header: 'Transaction No.', render: (a) => <strong>{a.transactionNo}</strong> },
  { key: 'action', header: 'Action', render: (a) => humanize(a.action) },
  { key: 'reason', header: 'Reason', render: (a) => a.reasonText ?? humanize(a.reasonCode) },
  { key: 'amount', header: 'Amount', numeric: true, render: (a) => <Amount value={a.amount} /> },
  {
    key: 'req',
    header: 'Requested',
    render: (a) => `${a.requestedBy} · ${formatDateTime(a.requestedAt)}`,
  },
  {
    key: 'appr',
    header: 'Approved',
    render: (a) => (a.approvedBy ? `${a.approvedBy} · ${formatDateTime(a.approvedAt)}` : ''),
  },
  { key: 'stage', header: 'Status', render: (a) => <StatusBadge status={a.stage} /> },
];

/** The content of one tab of the receipt page. */
export function ReceiptTabContent({
  tab,
  receipt,
}: Readonly<{ tab: ReceiptTabId; receipt: ReceiptDetail }>) {
  switch (tab) {
    case 'applications':
      return (
        <DataTable
          caption="Applications"
          columns={APPLICATION_COLUMNS}
          rows={receipt.applications}
          rowKey={(a) => a.id}
          emptyMessage="Nothing applied from this receipt"
        />
      );
    case 'lines':
      return (
        <DataTable
          caption="Receipt lines"
          columns={LINE_COLUMNS}
          rows={receipt.lines}
          rowKey={(l) => `${l.invoiceNo ?? ''}${l.description ?? ''}${l.gross}`}
          emptyMessage="No items to display"
        />
      );
    case 'journal':
      return (
        <DataTable
          caption="Journal batches"
          columns={[
            {
              key: 'batch',
              header: 'Journal Batch',
              render: (j: JournalRow) => <code>{j.batch}</code>,
            },
            { key: 'what', header: 'Posting', render: (j: JournalRow) => j.what },
          ]}
          rows={journalRows(receipt)}
          rowKey={(j) => `${j.batch}-${j.what}`}
          emptyMessage="No journal posted yet"
        />
      );
    default:
      return (
        <DataTable
          caption="Actions"
          columns={ACTION_COLUMNS}
          rows={receipt.actions}
          rowKey={(a) => a.id}
          emptyMessage="No cancellation or reinstatement"
        />
      );
  }
}

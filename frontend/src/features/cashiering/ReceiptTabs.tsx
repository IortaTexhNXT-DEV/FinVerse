import { Link } from 'react-router-dom';
import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime } from '@/utils/format';
import type { Application, ReceiptAction, ReceiptDetail, ReceiptLine } from './cashieringApi';
import { allocationRows, componentLabel, journalRows } from './cashieringLogic';
import type { JournalRow, ReceiptTabId } from './cashieringLogic';
import { useState } from 'react';
import { InsurerName, LovLabel } from '@/components/broking/LovLabel';
import { CellStack } from '@/components/ui/CellStack';
import { UserName } from '@/components/ui/UserName';
import type { LedgerComponent } from '@/api/operations';
import { countOf } from '@/utils/format';
import { receiptActionLabel } from './cashieringLabels';
import { ReceiptDocuments } from './ReceiptDocuments';

/** The reason of a receipt transaction, from its list (cancellation or reinstatement reasons). */
function ReasonLabel({ action }: Readonly<{ action: ReceiptAction }>) {
  return (
    <LovLabel
      type={action.action === 'CANCEL' ? 'RECEIPT_CANCEL_REASON' : 'REINSTATEMENT_REASON'}
      code={action.reasonCode}
    />
  );
}

/**
 * The applications of a receipt, one row per invoice; the split by premium component opens as a
 * table under the row (never a sentence of amounts).
 */
function ApplicationsTable({ applications }: Readonly<{ applications: Application[] }>) {
  const [open, setOpen] = useState<ReadonlySet<number>>(new Set());
  const toggle = (id: number) =>
    setOpen((o) => {
      const next = new Set(o);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  const columns: Column<Application>[] = [
    {
      key: 'invoice',
      header: 'Invoice',
      render: (a) => (
        <CellStack
          main={
            <Link to={`/operations/invoices/${encodeURIComponent(a.invoiceNo)}`}>
              {a.invoiceNo}
            </Link>
          }
          sub={a.arn}
        />
      ),
    },
    {
      key: 'alloc',
      header: 'By Component',
      render: (a) => {
        const n = allocationRows(a.allocation).length;
        return (
          <button
            type="button"
            className="link-button"
            aria-expanded={open.has(a.id)}
            onClick={() => toggle(a.id)}
          >
            {open.has(a.id) ? 'Hide' : 'Show'} {countOf(n, 'component')}
          </button>
        );
      },
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
  return (
    <DataTable
      caption="Applications"
      columns={columns}
      rows={applications}
      rowKey={(a) => a.id}
      expanded={open}
      renderExpanded={(a) => (
        <DataTable
          caption={`Application to ${a.invoiceNo} by component`}
          columns={[
            {
              key: 'c',
              header: 'Component',
              render: (r: { component: LedgerComponent; amount: number }) =>
                componentLabel(r.component),
            },
            {
              key: 'a',
              header: 'Applied',
              numeric: true,
              render: (r: { component: LedgerComponent; amount: number }) => (
                <Amount value={r.amount} />
              ),
            },
          ]}
          rows={allocationRows(a.allocation)}
          rowKey={(r) => r.component}
        />
      )}
      emptyMessage="Nothing applied from this receipt"
    />
  );
}

const LINE_COLUMNS: Column<ReceiptLine>[] = [
  { key: 'invoice', header: 'Invoice / Item', render: (l) => l.invoiceNo ?? l.description ?? '' },
  { key: 'insurer', header: 'Insurer', render: (l) => <InsurerName code={l.insurerCode} /> },
  { key: 'gross', header: 'Gross', numeric: true, render: (l) => <Amount value={l.gross} /> },
  { key: 'vat', header: 'VAT', numeric: true, render: (l) => <Amount value={l.vat} /> },
  { key: 'wtax', header: 'WTAX', numeric: true, render: (l) => <Amount value={l.wtax} /> },
  { key: 'net', header: 'Net', numeric: true, render: (l) => <Amount value={l.net} /> },
];

const ACTION_COLUMNS: Column<ReceiptAction>[] = [
  { key: 'no', header: 'Transaction No.', render: (a) => <strong>{a.transactionNo}</strong> },
  { key: 'action', header: 'Action', render: (a) => receiptActionLabel(a.action) },
  { key: 'reason', header: 'Reason', render: (a) => a.reasonText ?? <ReasonLabel action={a} /> },
  { key: 'amount', header: 'Amount', numeric: true, render: (a) => <Amount value={a.amount} /> },
  {
    key: 'req',
    header: 'Requested',
    render: (a) => (
      <CellStack main={<UserName login={a.requestedBy} />} sub={formatDateTime(a.requestedAt)} />
    ),
  },
  {
    key: 'appr',
    header: 'Approved',
    render: (a) =>
      a.approvedBy ? (
        <CellStack main={<UserName login={a.approvedBy} />} sub={formatDateTime(a.approvedAt)} />
      ) : (
        ''
      ),
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
      return <ApplicationsTable applications={receipt.applications} />;
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
    case 'documents':
      return <ReceiptDocuments summary={receipt.summary} />;
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

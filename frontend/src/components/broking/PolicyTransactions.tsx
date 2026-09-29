import { useQuery } from '@tanstack/react-query';
import { ChevronDown, ChevronRight } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { policyTransactionsApi } from '@/api/policyTransactions';
import type {
  JournalEntryLine,
  PolicyTransaction,
  PolicyTransactions as History,
  TransactionJournal,
} from '@/api/policyTransactions';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { CellStack } from '@/components/ui/CellStack';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount, formatDate } from '@/utils/format';
import { journalCountLabel, transactionRowKey } from './policyTransactionRows';
import { useBaseCurrency } from '@/context/workspaceContext';

interface PolicyTransactionsProps {
  /** Any invoice of the policy (Invoice 360, endorsement request). */
  invoiceNo?: string;
  /** Or the account: every policy year of the ARN. */
  arn?: string;
  /** Row of the transaction to highlight (the request being viewed). */
  highlightRequestNo?: string;
}

const LINE_COLUMNS: Column<JournalEntryLine>[] = [
  {
    key: 'account',
    header: 'GL Account',
    render: (l) => <CellStack main={l.accountCode} sub={l.accountName} />,
  },
  { key: 'party', header: 'Party', kind: 'code', render: (l) => l.partyCode ?? '' },
  { key: 'narration', header: 'Narration', render: (l) => l.narration ?? '' },
  {
    key: 'debit',
    header: 'Debit',
    kind: 'amount',
    render: (l) => (l.debit === undefined ? '' : <Amount value={l.debit} />),
  },
  {
    key: 'credit',
    header: 'Credit',
    kind: 'amount',
    render: (l) => (l.credit === undefined ? '' : <Amount value={l.credit} />),
  },
];

function JournalTitle({ journal }: Readonly<{ journal: TransactionJournal }>) {
  const { can } = useAuth();
  const number = can('JOURNAL_VIEW') ? (
    <Link to={`/gl/journals/${String(journal.id)}`}>{journal.batchNo}</Link>
  ) : (
    <strong>{journal.batchNo}</strong>
  );
  return (
    <div className="policy-txn-journal-title">
      {number}
      <span>{formatDate(journal.valueDate)}</span>
      <StatusBadge status={journal.status} />
      {journal.narration && <span className="muted">{journal.narration}</span>}
    </div>
  );
}

/** The GL journals of a transaction with their lines (the expanded row). */
export function TransactionJournals({ journals }: Readonly<{ journals: TransactionJournal[] }>) {
  if (journals.length === 0) {
    return <p className="muted">No GL journal for this transaction.</p>;
  }
  return (
    <div className="stack policy-txn-journals">
      {journals.map((j) => (
        <section key={j.batchNo} aria-label={`Journal ${j.batchNo}`}>
          <JournalTitle journal={j} />
          <DataTable
            caption={`Lines of journal ${j.batchNo}`}
            columns={LINE_COLUMNS}
            rows={j.lines}
            rowKey={(l) => l.lineNo}
            emptyMessage="No lines"
            footer={
              <tr className="row-total">
                <td colSpan={3}>Total</td>
                <td className="num">{formatAmount(j.totalDebit)}</td>
                <td className="num">{formatAmount(j.totalCredit)}</td>
              </tr>
            }
          />
        </section>
      ))}
    </div>
  );
}

function Reference({ row }: Readonly<{ row: PolicyTransaction }>) {
  const { invoiceNo, endorsementNo, requestNo, requestId } = row.refs;
  const invoice = invoiceNo ? (
    <Link
      to={`/operations/invoices/${encodeURIComponent(invoiceNo)}`}
      onClick={(e) => e.stopPropagation()}
    >
      {invoiceNo}
    </Link>
  ) : undefined;
  const request =
    requestNo && requestId !== undefined ? (
      <Link to={`/adjustment/requests/${String(requestId)}`} onClick={(e) => e.stopPropagation()}>
        {requestNo}
      </Link>
    ) : (
      requestNo
    );
  return (
    <CellStack main={invoice ?? request} sub={invoice ? (request ?? endorsementNo) : undefined} />
  );
}

function columns(
  currency: string,
  expanded: ReadonlySet<number>,
  toggle: (seq: number) => void,
): Column<PolicyTransaction>[] {
  return [
    {
      key: 'date',
      header: 'Date',
      kind: 'date',
      render: (t) => (
        <CellStack
          main={formatDate(t.date)}
          sub={t.effectiveDate ? `Effective ${formatDate(t.effectiveDate)}` : undefined}
        />
      ),
    },
    {
      key: 'type',
      header: 'Transaction',
      render: (t) => <CellStack main={t.typeLabel} sub={t.detail} />,
    },
    { key: 'ref', header: 'Reference', kind: 'code', render: (t) => <Reference row={t} /> },
    {
      key: 'premium',
      header: `Premium Change (${currency})`,
      kind: 'amount',
      render: (t) => <Amount value={t.change.premium} />,
    },
    {
      key: 'taxes',
      header: `Taxes Change (${currency})`,
      kind: 'amount',
      render: (t) => <Amount value={t.change.taxes} />,
    },
    {
      key: 'commission',
      header: `Commission Change (${currency})`,
      kind: 'amount',
      render: (t) => <Amount value={t.change.commission} />,
    },
    {
      key: 'after',
      header: `Gross Premium After (${currency})`,
      kind: 'amount',
      render: (t) =>
        t.after === undefined ? (
          <span className="muted">Not posted</span>
        ) : (
          <CellStack
            main={<Amount value={t.after.gross} />}
            sub={`Commission ${formatAmount(t.after.commission)}`}
          />
        ),
    },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (t) => <StatusBadge status={t.status} label={t.statusLabel} />,
    },
    {
      key: 'journals',
      header: 'Accounting',
      render: (t) =>
        t.journals.length === 0 ? (
          ''
        ) : (
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            aria-expanded={expanded.has(t.seq)}
            onClick={(e) => {
              e.stopPropagation();
              toggle(t.seq);
            }}
          >
            {expanded.has(t.seq) ? (
              <ChevronDown size={14} aria-hidden="true" />
            ) : (
              <ChevronRight size={14} aria-hidden="true" />
            )}
            {journalCountLabel(t.journals.length)}
          </button>
        ),
    },
  ];
}

/** The history table of a loaded policy history (tests and the pages below). */
export function PolicyTransactionsTable({
  history,
  highlightRequestNo,
  loading = false,
}: Readonly<{ history?: History; highlightRequestNo?: string; loading?: boolean }>) {
  const [expanded, setExpanded] = useState<ReadonlySet<number>>(new Set());
  const toggle = (seq: number) => {
    const next = new Set(expanded);
    if (next.has(seq)) {
      next.delete(seq);
    } else {
      next.add(seq);
    }
    setExpanded(next);
  };
  const baseCurrency = useBaseCurrency();
  const rows = history?.rows ?? [];
  const selected = rows.find(
    (r) => highlightRequestNo !== undefined && r.refs.requestNo === highlightRequestNo,
  );
  return (
    <DataTable
      caption="Policy transactions"
      columns={columns(history?.currency ?? baseCurrency, expanded, toggle)}
      rows={rows}
      rowKey={transactionRowKey}
      loading={loading}
      selectedKey={selected === undefined ? undefined : transactionRowKey(selected)}
      emptyMessage="No transaction recorded"
      expanded={expanded}
      renderExpanded={(t) => <TransactionJournals journals={t.journals} />}
    />
  );
}

/**
 * Policy Transactions (ADJID.022/024): the original booking, then every endorsement, cancellation,
 * adjustment and refund of the policy in order, with the premium, tax and commission change, the
 * position after it, its status and its GL journals (journal number, date and lines in an
 * expandable row, with a link to the journal). One component for the Invoice 360, the account and
 * the endorsement request pages.
 */
export function PolicyTransactions({
  invoiceNo,
  arn,
  highlightRequestNo,
}: Readonly<PolicyTransactionsProps>) {
  const history = useQuery({
    queryKey: ['ops', 'policy-transactions', invoiceNo ?? '', arn ?? ''],
    queryFn: () =>
      invoiceNo !== undefined
        ? policyTransactionsApi.forInvoice(invoiceNo)
        : policyTransactionsApi.forAccount(arn ?? ''),
    enabled: invoiceNo !== undefined || arn !== undefined,
  });
  return (
    <div className="stack">
      <ErrorAlert error={history.error} />
      <PolicyTransactionsTable
        history={history.data}
        highlightRequestNo={highlightRequestNo}
        loading={history.isLoading}
      />
    </div>
  );
}

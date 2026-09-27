import { Amount } from '@/components/ui/Amount';
import { CellStack, EmptyCell } from '@/components/ui/CellStack';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tag } from '@/components/ui/Tag';
import { UserName } from '@/components/ui/UserName';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import type { CashieringRequest, UnappliedRow } from './api';
import { ACTION_LABELS, TAB_LABELS, cashieringStatus } from './labels';

/** Table columns of the unapplied-payment screens (BRCLXN.036, 040). */

const DASH = <EmptyCell />;

/** The columns of the collector list (BRCLXN.036). */
export const UNAPPLIED_COLUMNS: Column<UnappliedRow>[] = [
  {
    key: 'ref',
    header: 'Unapplied Reference',
    kind: 'code',
    render: (r) => <CellStack main={<strong>{r.unappliedRef}</strong>} sub={r.payor} />,
  },
  {
    key: 'date',
    header: 'Payment Date',
    kind: 'date',
    render: (r) => <CellStack main={formatDate(r.paymentDate)} sub={`${String(r.ageDays)} days`} />,
  },
  {
    key: 'txn',
    header: 'Transaction',
    kind: 'code',
    render: (r) => (
      <CellStack
        main={r.transactionNo}
        sub={[r.paymentType, r.bankCode, r.checkNo].filter(Boolean).join(' · ')}
      />
    ),
  },
  {
    key: 'amount',
    header: 'Paid (PHP)',
    kind: 'amount',
    render: (r) => <Amount value={r.amount} />,
  },
  {
    key: 'balance',
    header: 'Unapplied (PHP)',
    kind: 'amount',
    render: (r) => <Amount value={r.balance} />,
  },
  {
    key: 'match',
    header: 'Matched Account',
    kind: 'code',
    render: (r) => {
      const matched = r.invoiceNo ?? r.clientCode;
      return matched === undefined ? (
        <StatusBadge status="UNMATCHED" label="Not Matched" tone="neutral" />
      ) : (
        <CellStack main={matched} sub={r.account?.assuredName} />
      );
    },
  },
  {
    key: 'segment',
    header: 'Segment / AO',
    render: (r) => {
      const segment = r.account?.segment ?? r.salesUnit;
      if (segment === undefined && r.account?.aoUsername === undefined) {
        return DASH;
      }
      return (
        <CellStack
          main={segment ?? '—'}
          sub={
            r.account?.aoUsername === undefined ? undefined : (
              <UserName login={r.account.aoUsername} />
            )
          }
        />
      );
    },
  },
  {
    key: 'disposition',
    header: 'Collector Disposition',
    render: (r) =>
      r.dispositionCode === undefined ? (
        DASH
      ) : (
        <CellStack
          main={<Tag tone="info">{humanize(r.dispositionCode)}</Tag>}
          sub={
            <>
              <UserName login={r.dispositionBy} /> · {formatDate(r.dispositionAt)}
            </>
          }
        />
      ),
  },
  {
    key: 'tab',
    header: 'Cashiering',
    kind: 'status',
    render: (r) => <StatusBadge status={r.cashieringTab} label={TAB_LABELS[r.cashieringTab]} />,
  },
  {
    key: 'request',
    header: 'Request Status',
    kind: 'status',
    render: (r) => {
      const status = cashieringStatus(r.cashieringStatus);
      return status === undefined ? DASH : <StatusBadge status={status} />;
    },
  },
];

/** The columns of the requests sent to Cashiering (status view). */
export const REQUEST_COLUMNS: Column<CashieringRequest>[] = [
  {
    key: 'ref',
    header: 'Unapplied Reference',
    kind: 'code',
    render: (r) => <CellStack main={<strong>{r.unappliedRef}</strong>} sub={r.payor} />,
  },
  {
    key: 'action',
    header: 'Request',
    render: (r) => <CellStack main={ACTION_LABELS[r.action]} sub={r.invoiceNo} />,
  },
  {
    key: 'amount',
    header: 'Amount (PHP)',
    kind: 'amount',
    render: (r) =>
      r.amount === undefined ? (
        <span className="muted">Whole Balance</span>
      ) : (
        <Amount value={r.amount} />
      ),
  },
  {
    key: 'by',
    header: 'Requested By',
    render: (r) => (
      <CellStack main={<UserName login={r.requestedBy} />} sub={formatDateTime(r.requestedAt)} />
    ),
  },
  {
    key: 'cashiering',
    header: 'Cashiering Reference',
    kind: 'code',
    render: (r) =>
      r.cashieringRef === undefined ? (
        DASH
      ) : (
        <CellStack main={r.cashieringRef} sub={r.statusMessage} />
      ),
  },
  { key: 'file', header: 'Application File', kind: 'code', render: (r) => r.fileRunNo ?? DASH },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (r) => <StatusBadge status={r.status} />,
  },
];

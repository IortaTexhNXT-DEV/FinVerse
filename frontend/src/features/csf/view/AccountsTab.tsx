import { useQuery } from '@tanstack/react-query';
import { csfApi } from '@/api/csf';
import type { AccountLine } from '@/api/csf';
import { useLovLabel } from '@/components/broking/useLabels';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { periodColumn } from '@/components/ui/periodColumn';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tag } from '@/components/ui/Tag';
import { formatAmount } from '@/utils/format';
import { CSF_LOV, csfStatusTone } from '../csfCodes';
import { InBaseCurrency } from '@/components/ui/InBaseCurrency';

/** Title of the notice of a tab whose source module does not answer (FR-CSF-011). */
export const TAB_UNAVAILABLE = 'Information not available now. Try again';

function useColumns(compact: boolean): Column<AccountLine>[] {
  const statusLabel = useLovLabel(CSF_LOV.status);
  const columns: Column<AccountLine>[] = [
    {
      key: 'arn',
      header: 'ARN',
      kind: 'code',
      render: (a) => <CellStack main={a.arn} sub={a.policyNumbers.join(', ')} />,
    },
    {
      key: 'product',
      header: 'Product',
      render: (a) => <CellStack main={a.productName} sub={a.insurerName} />,
    },
    {
      key: 'csfStatus',
      header: 'Status',
      kind: 'status',
      render: (a) =>
        a.csfStatus ? (
          <StatusBadge
            status={a.csfStatus}
            label={statusLabel(a.csfStatus)}
            tone={csfStatusTone(a.csfStatus)}
          />
        ) : (
          ''
        ),
    },
    {
      key: 'stage',
      header: 'BIBS Stage',
      kind: 'status',
      render: (a) => <StatusBadge status={a.stage} />,
    },
    periodColumn<AccountLine>(
      'period',
      'Period',
      (a) => a.periodFrom,
      (a) => a.periodTo,
    ),
  ];
  if (!compact) {
    columns.push(
      {
        key: 'balance',
        header: <InBaseCurrency label="Balance" />,
        kind: 'amount',
        render: (a) => (a.balance === null ? '' : formatAmount(a.balance)),
      },
      {
        key: 'payment',
        header: 'Payment',
        kind: 'status',
        render: (a) => (a.paymentStatus ? <StatusBadge status={a.paymentStatus} /> : ''),
      },
      {
        key: 'pn',
        header: 'PN / Application No.',
        render: (a) => <CellStack main={a.pnNumbers.join(', ')} sub={a.loanApplicationNo} />,
      },
      {
        key: 'flags',
        header: 'Flags',
        render: (a) => (
          <span className="tag-list">
            {a.ffy && <Tag>FFY</Tag>}
            {a.directPayment && <Tag>Direct Payment</Tag>}
          </span>
        ),
      },
    );
  }
  return columns;
}

/**
 * The accounts of a client (FR-CSF-011, 012): ARN with the policy number, product and insurer,
 * CSF status next to the BIBS stage, period, balance, payment status, PN and flags; `compact`
 * shows the matching accounts under a search result.
 */
export function AccountsTable({
  rows,
  compact = false,
  loading = false,
}: Readonly<{ rows: AccountLine[]; compact?: boolean; loading?: boolean }>) {
  const columns = useColumns(compact);
  return (
    <DataTable
      columns={columns}
      rows={rows}
      rowKey={(a) => a.id}
      loading={loading}
      caption="Accounts"
    />
  );
}

/** The Accounts tab of the Servicing View: every account of the client, CBG and non-CBG. */
export function AccountsTab({
  companyId,
  clientId,
}: Readonly<{ companyId: number; clientId: number }>) {
  const accounts = useQuery({
    queryKey: ['csf', 'accounts', companyId, clientId],
    queryFn: () => csfApi.accounts(companyId, clientId),
  });
  return (
    <Card flush>
      <ErrorAlert
        error={accounts.error}
        title={TAB_UNAVAILABLE}
        onRetry={() => void accounts.refetch()}
      />
      <AccountsTable rows={accounts.data ?? []} loading={accounts.isLoading} />
    </Card>
  );
}

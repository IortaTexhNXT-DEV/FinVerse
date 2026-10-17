import { useQuery } from '@tanstack/react-query';
import { insurerAccountsApi } from '@/api/insurerPage';
import type { InsurerGlAccount, InsurerSubLedgerLine } from '@/api/insurerPage';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { formatAmount, formatDate, humanize } from '@/utils/format';

/**
 * Ledger tab of the insurer page (G08): the general ledger accounts posted with the insurer as
 * party, with debits, credits and balance in the base currency, and the insurer's sub-ledger open
 * items by document type, direction, currency and status. Read-only; the accounts are maintained
 * on the accounting screens.
 */
export function InsurerAccountsTab({ insurerId }: Readonly<{ insurerId: number }>) {
  const accounts = useQuery({
    queryKey: ['catalog', 'insurer-accounts', insurerId],
    queryFn: () => insurerAccountsApi.accounts(insurerId),
  });
  return (
    <>
      <ErrorAlert error={accounts.error} onRetry={() => void accounts.refetch()} />
      <Card title="General ledger accounts" flush>
        <DataTable<InsurerGlAccount>
          rows={accounts.data?.glAccounts ?? []}
          rowKey={(a) => a.code}
          loading={accounts.isLoading}
          emptyMessage="No ledger entry posted for this insurer."
          caption="General ledger accounts of the insurer"
          columns={[
            { key: 'code', header: 'Account', render: (a) => a.code },
            { key: 'name', header: 'Name', render: (a) => a.name, truncate: true },
            { key: 'class', header: 'Class', render: (a) => humanize(a.accountClass) },
            {
              key: 'sl',
              header: 'Sub-ledger',
              render: (a) => (a.controlAccount ? humanize(a.subLedgerType) : '-'),
            },
            { key: 'entries', header: 'Entries', numeric: true, render: (a) => a.entries },
            { key: 'debit', header: 'Debit', numeric: true, render: (a) => formatAmount(a.debit) },
            {
              key: 'credit',
              header: 'Credit',
              numeric: true,
              render: (a) => formatAmount(a.credit),
            },
            {
              key: 'balance',
              header: 'Balance',
              numeric: true,
              render: (a) => formatAmount(a.balance),
            },
            { key: 'last', header: 'Last Entry', render: (a) => formatDate(a.lastDate) },
          ]}
        />
      </Card>
      <Card title="Sub-ledger open items" flush>
        <DataTable<InsurerSubLedgerLine>
          rows={accounts.data?.subLedger ?? []}
          rowKey={(l) => `${l.documentType}-${l.direction}-${l.currency}-${l.status}`}
          loading={accounts.isLoading}
          emptyMessage="No sub-ledger item for this insurer."
          caption="Sub-ledger open items of the insurer"
          columns={[
            { key: 'type', header: 'Document Type', render: (l) => humanize(l.documentType) },
            { key: 'dir', header: 'Direction', render: (l) => humanize(l.direction) },
            { key: 'cur', header: 'Currency', render: (l) => l.currency },
            { key: 'status', header: 'Status', render: (l) => humanize(l.status) },
            { key: 'items', header: 'Items', numeric: true, render: (l) => l.items },
            {
              key: 'amount',
              header: 'Amount',
              numeric: true,
              render: (l) => formatAmount(l.amount),
            },
            {
              key: 'settled',
              header: 'Settled',
              numeric: true,
              render: (l) => formatAmount(l.settled),
            },
            {
              key: 'out',
              header: 'Outstanding',
              numeric: true,
              render: (l) => formatAmount(l.outstanding),
            },
          ]}
        />
      </Card>
    </>
  );
}

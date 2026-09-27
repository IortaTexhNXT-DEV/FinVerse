import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ebApi } from '@/api/eb';
import type { ActivityRow, CycleAccount } from '@/api/eb';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate, formatDateTime } from '@/utils/format';
import { ebLabel } from '../common/ebCodes';

const ACCOUNT_COLUMNS: Column<CycleAccount>[] = [
  {
    key: 'arn',
    header: 'ARN',
    kind: 'code',
    render: (a) => <Link to={`/accounts/${String(a.id)}`}>{a.arn}</Link>,
  },
  { key: 'cycle', header: 'Cycle', kind: 'code', render: (a) => a.cycleNo },
  {
    key: 'type',
    header: 'Business Type',
    render: (a) => <CellStack main={ebLabel(a.businessType)} sub={a.renewalOfRef ?? undefined} />,
  },
  { key: 'product', header: 'Product', kind: 'code', render: (a) => a.productCode ?? '' },
  { key: 'insurer', header: 'Insurer', kind: 'code', render: (a) => a.insurerCode ?? '' },
  {
    key: 'period',
    header: 'Period',
    render: (a) => `${formatDate(a.periodFrom)} – ${formatDate(a.periodTo)}`,
  },
  {
    key: 'premium',
    header: 'Gross Premium',
    kind: 'amount',
    render: (a) =>
      a.grossPremium === null || a.grossPremium === undefined
        ? ''
        : `${a.currency ?? ''} ${formatAmount(a.grossPremium)}`.trim(),
  },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (a) => <StatusBadge status={a.status} />,
  },
];

/**
 * Accounts tab (FR-EB-046; BT0): the accounts created for the cycles at placement, with their
 * business type (a renewal names the account it renews) and their placement status.
 */
export function AccountsTab({ programmeId }: Readonly<{ programmeId: number }>) {
  const companyId = useCompanyId();
  const accounts = useQuery({
    queryKey: ['eb', 'accounts', programmeId],
    queryFn: () => ebApi.accounts(companyId, programmeId),
  });
  return (
    <Card title="Accounts">
      <ErrorAlert error={accounts.error} onRetry={() => void accounts.refetch()} />
      <DataTable<CycleAccount>
        loading={accounts.isLoading}
        rows={accounts.data ?? []}
        rowKey={(a) => a.id}
        columns={ACCOUNT_COLUMNS}
        emptyMessage="No accounts yet. They are created when the client's confirmation triggers the placement."
      />
    </Card>
  );
}

const ACTIVITY_COLUMNS: Column<ActivityRow>[] = [
  { key: 'activity', header: 'Activity', render: (a) => ebLabel(a.activity) },
  { key: 'cycle', header: 'Cycle', kind: 'code', render: (a) => a.cycleNo ?? '' },
  {
    key: 'received',
    header: 'Received',
    kind: 'datetime',
    render: (a) => formatDateTime(a.receivedAt),
  },
  {
    key: 'released',
    header: 'Released',
    kind: 'datetime',
    render: (a) => formatDateTime(a.releasedAt),
  },
  {
    key: 'actor',
    header: 'By',
    render: (a) => (a.actor === 'SYSTEM' ? 'BIBS' : <UserName login={a.actor} />),
  },
  { key: 'remarks', header: 'Remarks', render: (a) => a.remarks ?? '' },
];

/** History tab (FR-EB-003, FR-EB-062): the activity log of the programme, newest first. */
export function HistoryTab({ programmeId }: Readonly<{ programmeId: number }>) {
  const companyId = useCompanyId();
  const activity = useQuery({
    queryKey: ['eb', 'activity', programmeId],
    queryFn: () => ebApi.activity(companyId, programmeId),
  });
  return (
    <Card title="Activity">
      <ErrorAlert error={activity.error} onRetry={() => void activity.refetch()} />
      <DataTable<ActivityRow>
        loading={activity.isLoading}
        rows={activity.data ?? []}
        rowKey={(a) => a.id}
        columns={ACTIVITY_COLUMNS}
        emptyMessage="No activity recorded"
      />
    </Card>
  );
}

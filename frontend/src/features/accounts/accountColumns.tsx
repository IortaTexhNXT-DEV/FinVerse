import type { AccountSummary } from '@/api/accounts';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate } from '@/utils/format';

/** Columns of the account lists: ARN chip, client, product, insurer, period, premium, status. */
export const ACCOUNT_COLUMNS: Column<AccountSummary>[] = [
  {
    key: 'arn',
    header: 'ARN',
    render: (a) => (
      <span onClick={(e) => e.stopPropagation()} role="presentation">
        <ReferenceChip value={a.arn} />
      </span>
    ),
  },
  {
    key: 'c',
    header: 'Client',
    render: (a) => (
      <>
        <strong>{a.clientName}</strong>
        <div className="muted">{a.clientCode ?? 'Prospect'}</div>
      </>
    ),
  },
  { key: 'p', header: 'Product', render: (a) => a.productCode },
  { key: 'i', header: 'Insurer', render: (a) => a.insurerCode ?? '' },
  {
    key: 'd',
    header: 'Period',
    render: (a) => `${formatDate(a.periodFrom)} – ${formatDate(a.periodTo)}`,
  },
  {
    key: 'g',
    header: 'Gross premium',
    numeric: true,
    render: (a) => <Amount value={a.grossPremium} />,
  },
  {
    key: 's',
    header: 'Status',
    render: (a) => (
      <div className="row">
        <StatusBadge status={a.status} />
        {a.ffy && <StatusBadge status="FFY" />}
        {a.directPayment && <StatusBadge status="DIRECT_PAYMENT" />}
      </div>
    ),
  },
  { key: 'o', header: 'Officer', render: (a) => a.accountOfficer ?? '' },
];

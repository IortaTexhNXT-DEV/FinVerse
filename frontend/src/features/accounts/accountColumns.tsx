import type { AccountSummary } from '@/api/accounts';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Amount } from '@/components/ui/Amount';
import { OriginBadge } from '@/components/ui/OriginBadge';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { InsurerName, ProductName } from '@/components/broking/LovLabel';
import { periodColumn } from '@/components/ui/periodColumn';

/**
 * Columns of the account lists: ARN chip, client, product with the insurer under it, period,
 * premium, status with the flags under it, officer; every column fits a 1440-pixel window.
 */
export const ACCOUNT_COLUMNS: Column<AccountSummary>[] = [
  {
    key: 'arn',
    header: 'ARN',
    kind: 'code',
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
  {
    key: 'p',
    header: 'Product / Insurer',
    render: (a) => (
      <span className="cell-stack">
        <ProductName code={a.productCode} />
        <span className="muted">
          <InsurerName code={a.insurerCode} empty="To be advised" />
        </span>
      </span>
    ),
  },
  periodColumn<AccountSummary>(
    'd',
    'Period',
    (a) => a.periodFrom,
    (a) => a.periodTo,
  ),
  {
    key: 'g',
    header: 'Gross Premium',
    numeric: true,
    render: (a) => <Amount value={a.grossPremium} />,
  },
  {
    key: 's',
    header: 'Status / Flags',
    kind: 'status',
    render: (a) => (
      <span className="cell-stack">
        <span>
          <StatusBadge status={a.status} />
        </span>
        <span className="tag-list">
          {a.ffy && <span className="tag">FFY</span>}
          {a.directPayment && <span className="tag">Direct Payment</span>}
          <OriginBadge record={{ ...a, origin: a.origin === 'MIGRATED' ? 'MIGRATED' : 'BIBS' }} />
        </span>
      </span>
    ),
  },
  { key: 'o', header: 'Officer', render: (a) => <UserName login={a.accountOfficer} /> },
];

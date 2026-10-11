import { InsurerName } from '@/components/broking/LovLabel';
import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { formatDate } from '@/utils/format';
import type { BatchSummary } from './api';
import { TypeChip } from './RemittanceParts';

/**
 * Columns of Remittance Batches. The processor stays on one line cut at the column width (full
 * name in the tooltip) and the status is a pill column, so the list fits its card.
 */
export const BATCH_COLUMNS: Column<BatchSummary>[] = [
  {
    key: 'no',
    header: 'Batch No.',
    render: (b) => (
      <>
        <strong>{b.batchNo}</strong>
        <div className="remit-muted">{b.specialRequestNo ?? formatDate(b.createdAt)}</div>
      </>
    ),
  },
  { key: 'ins', header: 'Insurer', render: (b) => <InsurerName code={b.insurerCode} /> },
  { key: 'type', header: 'Type', render: (b) => <TypeChip type={b.type} /> },
  { key: 'n', header: 'Accounts', numeric: true, render: (b) => b.lineCount },
  {
    key: 'paid',
    header: 'Paid AR',
    numeric: true,
    render: (b) => <Amount value={b.totals.paidAr} />,
  },
  {
    key: 'pay',
    header: 'Payable',
    numeric: true,
    render: (b) => <Amount value={b.totals.payable} />,
  },
  { key: 'cur', header: 'Currency', defaultHidden: true, render: (b) => b.currency },
  {
    key: 'proc',
    header: 'Processor',
    defaultHidden: true,
    width: '180px',
    truncate: true,
    render: (b) => <UserName login={b.processor} empty="Unassigned" truncate />,
  },
  { key: 'dv', header: 'DV No.', render: (b) => b.dvNo ?? '' },
  {
    key: 'stage',
    header: 'Status',
    kind: 'status',
    render: (b) => <StatusBadge status={b.stage} />,
  },
];

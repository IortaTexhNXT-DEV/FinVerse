import type { ProposalListItem } from '@/api/proposals';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate } from '@/utils/format';

/** Columns of the PRF lists. */
export const PROPOSAL_COLUMNS: Column<ProposalListItem>[] = [
  {
    key: 'no',
    header: 'Proposal No.',
    render: (p) => (
      <>
        <strong className="mono">{p.prfNo}</strong>
        <div className="muted">{formatDate(p.createdAt.slice(0, 10))}</div>
      </>
    ),
  },
  {
    key: 'arn',
    header: 'ARN',
    render: (p) => (
      <span onClick={(e) => e.stopPropagation()} role="presentation">
        <ReferenceChip value={p.arn} />
      </span>
    ),
  },
  {
    key: 'client',
    header: 'Client',
    render: (p) => (
      <>
        <strong>{p.clientName}</strong>
        <div className="muted">{p.clientCode}</div>
      </>
    ),
  },
  { key: 'product', header: 'Product', render: (p) => p.productCode },
  {
    key: 'tsi',
    header: 'Sum insured',
    numeric: true,
    render: (p) => <Amount value={p.totalSumInsured} />,
  },
  {
    key: 'slips',
    header: 'Slips',
    render: (p) => [p.qsNo, p.psNo].filter(Boolean).join(' / ') || '—',
  },
  { key: 'insurer', header: 'Chosen insurer', render: (p) => p.chosenInsurer ?? '—' },
  { key: 'status', header: 'Stage', render: (p) => <StatusBadge status={p.status} /> },
];

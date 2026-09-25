import type { RequestListItem } from '@/api/productmaint';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate } from '@/utils/format';
import { typeLabel } from './packageRequest';
import { StageAge } from './StageAge';

function productOf(r: RequestListItem): string {
  if (r.productCode === undefined) {
    return 'New package';
  }
  return r.resultingVersionNo === undefined
    ? r.productCode
    : `${r.productCode} v${r.resultingVersionNo}`;
}

/** Columns of the package request lists (without the selection column). */
export const REQUEST_COLUMNS: Column<RequestListItem>[] = [
  {
    key: 'no',
    header: 'Request No.',
    render: (r) => (
      <>
        <strong className="mono">{r.requestNo}</strong>
        <div className="muted">{formatDate(r.createdAt.slice(0, 10))}</div>
      </>
    ),
  },
  { key: 'type', header: 'Type', render: (r) => typeLabel(r.requestType) },
  {
    key: 'scope',
    header: 'Scope',
    render: (r) =>
      r.scope === 'CLIENT_SPECIFIC' ? (
        <span className="tag">Client-specific</span>
      ) : (
        <span className="tag">Generic</span>
      ),
  },
  {
    key: 'title',
    header: 'Client / Programme',
    render: (r) => (
      <>
        <strong>{r.title}</strong>
        <div className="muted">{r.clientName ?? 'Programme'}</div>
      </>
    ),
  },
  { key: 'line', header: 'Line', render: (r) => r.lineCode },
  {
    key: 'product',
    header: 'Product',
    render: productOf,
  },
  { key: 'stage', header: 'Stage', render: (r) => <StatusBadge status={r.status} /> },
  { key: 'age', header: 'In Stage', render: (r) => <StageAge item={r} /> },
  { key: 'assignee', header: 'Assignee', render: (r) => r.assignee ?? '—' },
];

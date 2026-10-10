import type { RequestListItem } from '@/api/productmaint';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate } from '@/utils/format';
import { typeLabel } from './packageRequest';
import { StageAge } from './StageAge';
import { UserName } from '@/components/ui/UserName';
import { LineLabel } from '@/components/broking/LovLabel';

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
        <ReferenceChip value={r.requestNo} />
        <div className="muted">{formatDate(r.createdAt.slice(0, 10))}</div>
      </>
    ),
  },
  { key: 'type', header: 'Type', render: (r) => typeLabel(r.requestType) },
  {
    key: 'scope',
    header: 'Scope',
    defaultHidden: true,
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
  {
    key: 'line',
    header: 'Line',
    defaultHidden: true,
    render: (r) => <LineLabel code={r.lineCode} />,
  },
  {
    key: 'product',
    header: 'Product',
    render: productOf,
  },
  { key: 'stage', header: 'Stage', render: (r) => <StatusBadge status={r.status} /> },
  { key: 'age', header: 'In Stage', render: (r) => <StageAge item={r} /> },
  { key: 'assignee', header: 'Assignee', render: (r) => <UserName login={r.assignee} /> },
  { key: 'ao', header: 'Account Officer', render: (r) => <UserName login={r.createdBy} /> },
  {
    key: 'submitted',
    header: 'Submission Date',
    render: (r) => (r.submittedAt === undefined ? '' : formatDate(r.submittedAt.slice(0, 10))),
  },
];

/**
 * The Package Request List of the Product Maintenance landing page in BDOI's words (FRPM.002.02):
 * Package Request Number, Reference Number (update requests), Request Type, Product Line, Account
 * Officer, Assigned TSU User, Submission Date and Current Status.
 */
export const PACKAGE_REQUEST_LIST_COLUMNS: Column<RequestListItem>[] = [
  {
    key: 'no',
    header: 'Package Request Number',
    render: (r) => <ReferenceChip value={r.requestNo} />,
  },
  {
    key: 'ref',
    header: 'Reference Number',
    render: (r) => (r.requestType === 'NEW' ? '' : productOf(r)),
  },
  { key: 'type', header: 'Request Type', render: (r) => typeLabel(r.requestType) },
  { key: 'line', header: 'Product Line', render: (r) => <LineLabel code={r.lineCode} /> },
  { key: 'ao', header: 'Account Officer', render: (r) => <UserName login={r.createdBy} /> },
  {
    key: 'tsu',
    header: 'Assigned TSU User',
    render: (r) => <UserName login={r.assignee} empty="Unassigned" />,
  },
  {
    key: 'submitted',
    header: 'Submission Date',
    render: (r) => (r.submittedAt === undefined ? '' : formatDate(r.submittedAt.slice(0, 10))),
  },
  {
    key: 'status',
    header: 'Current Status',
    render: (r) => <StatusBadge status={r.status} label={r.statusName} full />,
  },
];

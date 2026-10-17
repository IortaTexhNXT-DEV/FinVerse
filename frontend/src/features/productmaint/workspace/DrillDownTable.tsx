import { useNavigate } from 'react-router-dom';
import type { PmDrillRow } from '@/api/pmWorkspace';
import type { PageResponse } from '@/api/types';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { PageFooter } from '@/components/ui/Pager';
import { LineLabel } from '@/components/broking/LovLabel';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { formatDate } from '@/utils/format';
import { agingText, drillLink } from './pmDashboard';

const COLUMNS: Column<PmDrillRow>[] = [
  { key: 'no', header: 'Request Number', render: (r) => <strong>{r.requestNo}</strong> },
  { key: 'type', header: 'Request Type', render: (r) => r.requestType },
  { key: 'line', header: 'Product Line', render: (r) => <LineLabel code={r.lineCode} /> },
  { key: 'by', header: 'Requested By', render: (r) => <UserName login={r.requestedBy} /> },
  {
    key: 'officer',
    header: 'Assigned TSU Officer',
    render: (r) => <UserName login={r.assignee} empty="Unassigned" />,
  },
  {
    key: 'status',
    header: 'Current Status',
    render: (r) => <StatusBadge status={r.status} label={r.status} full />,
  },
  {
    key: 'submitted',
    header: 'Submission Date',
    render: (r) => (r.kind === 'EXPIRY' ? '' : formatDate(r.submittedAt)),
  },
  {
    key: 'aging',
    header: 'Aging',
    numeric: true,
    render: (r) => (r.kind === 'EXPIRY' ? `Expires ${formatDate(r.expiryDate)}` : agingText(r)),
  },
];

/**
 * The drill-down of a KPI (BDOI FRS FRPM.001.01): Request Number, Request Type, Product Line,
 * Requested By, Assigned TSU Officer, Current Status, Submission Date and Aging, oldest first; a
 * row opens the record.
 */
export function DrillDownTable({
  data,
  loading,
  onPage,
}: Readonly<{
  data: PageResponse<PmDrillRow> | undefined;
  loading: boolean;
  onPage: (page: number) => void;
}>) {
  const navigate = useNavigate();
  return (
    <>
      <DataTable<PmDrillRow>
        loading={loading}
        rows={data?.content ?? []}
        rowKey={(r) => `${r.kind}-${r.id ?? r.requestNo}`}
        onRowClick={(r) => void navigate(drillLink(r))}
        columns={COLUMNS}
        emptyMessage="No requests for these filters."
      />
      <PageFooter data={data} noun="requests" onPage={onPage} />
    </>
  );
}

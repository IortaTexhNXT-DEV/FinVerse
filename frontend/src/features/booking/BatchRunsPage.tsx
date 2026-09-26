import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bookingApi } from '@/api/booking';
import type { BatchRun } from '@/api/booking';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';

/**
 * Booking batch runs (BRNB.036): who or what started each run (a user, "Book now", the upload or
 * the end-of-day BOOKING_BATCH job) with its booked and failed counts; open one for the result of
 * every account.
 */
export default function BatchRunsPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const runs = useQuery({
    queryKey: ['booking', 'runs', companyId, page],
    queryFn: () => bookingApi.runs(companyId, page),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Booking"
        title="Batch Runs"
        description="Booking batches with their per-account results; each account is booked on its own, so a batch can partly succeed."
      />
      <ErrorAlert error={runs.error} />
      <Card flush>
        <DataTable<BatchRun>
          caption="Batch runs"
          loading={runs.isLoading}
          rows={runs.data?.content ?? []}
          rowKey={(r) => r.id}
          onRowClick={(r) => void navigate(`/booking/batch-runs/${encodeURIComponent(r.runNo)}`)}
          emptyMessage="No items to display"
          columns={[
            { key: 'no', header: 'Run No.', render: (r) => r.runNo },
            {
              key: 'trigger',
              header: 'Started by',
              render: (r) => `${humanize(r.trigger)} · ${r.startedBy}`,
            },
            { key: 'date', header: 'Business Date', render: (r) => formatDate(r.businessDate) },
            { key: 'at', header: 'Started', render: (r) => formatDateTime(r.startedAt) },
            { key: 'booked', header: 'Booked', numeric: true, render: (r) => r.bookedCount },
            { key: 'failed', header: 'Failed', numeric: true, render: (r) => r.failedCount },
            {
              key: 'status',
              header: 'Status',
              render: (r) => <StatusBadge status={r.failedCount > 0 ? 'PARTIAL' : 'COMPLETED'} />,
            },
          ]}
        />
        <PageFooter data={runs.data} noun="runs" onPage={setPage} />
      </Card>
    </div>
  );
}

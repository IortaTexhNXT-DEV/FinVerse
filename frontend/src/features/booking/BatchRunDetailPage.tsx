import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { bookingApi } from '@/api/booking';
import type { BatchRow } from '@/api/booking';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime, humanize } from '@/utils/format';

/** One booking batch run (BRNB.036): the outcome of every account, with the failure reasons. */
export default function BatchRunDetailPage() {
  const runNo = decodeURIComponent(useParams().runNo ?? '');
  const run = useQuery({
    queryKey: ['booking', 'run', runNo],
    queryFn: () => bookingApi.run(runNo),
  });
  if (run.data === undefined) {
    return run.error ? (
      <ErrorAlert error={run.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const r = run.data;
  return (
    <div className="stack">
      <PageHeader
        section="Booking · Batch Run"
        backTo="/booking/batch-runs"
        title={r.runNo}
        description={`${humanize(r.trigger)} run by ${r.startedBy} on ${formatDateTime(r.startedAt)}, business date ${formatDate(r.businessDate)}`}
      />
      <div className="grid-4">
        <Kpi label="Accounts" value={r.rows.length} />
        <Kpi label="Booked" value={r.bookedCount} />
        <Kpi label="Failed" value={r.failedCount} accent={r.failedCount > 0} />
        <Kpi label="Finished" value={formatDateTime(r.finishedAt)} />
      </div>
      <Card title="Results" flush>
        <DataTable<BatchRow>
          caption="Results"
          rows={r.rows}
          rowKey={(row) => row.arn}
          emptyMessage="No items to display"
          columns={[
            { key: 'arn', header: 'Proposal No. (ARN)', render: (row) => row.arn },
            {
              key: 'outcome',
              header: 'Result',
              render: (row) => <StatusBadge status={row.outcome} />,
            },
            {
              key: 'invoice',
              header: 'Invoice No.',
              render: (row) =>
                row.invoiceNo ? (
                  <Link to={`/booking/invoices/no/${encodeURIComponent(row.invoiceNo)}`}>
                    {row.invoiceNo}
                  </Link>
                ) : (
                  ''
                ),
            },
            { key: 'message', header: 'Reason', render: (row) => row.message ?? '' },
          ]}
        />
      </Card>
    </div>
  );
}

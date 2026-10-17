import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { reportSubscriptionApi, scheduleText } from '@/api/reportSubscriptions';
import type { ReportSubscription, SubscriptionRun } from '@/api/reportSubscriptions';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime } from '@/utils/format';

function Runs({ subscription }: Readonly<{ subscription: ReportSubscription }>) {
  const runs = useQuery({
    queryKey: ['report-subscriptions', 'runs', subscription.id],
    queryFn: () => reportSubscriptionApi.runs(subscription.id),
  });
  return (
    <Card title={`Runs of ${subscription.reportCode}`} flush>
      <ErrorAlert error={runs.error} />
      <DataTable<SubscriptionRun>
        loading={runs.isLoading}
        rows={runs.data ?? []}
        rowKey={(r) => r.id}
        emptyMessage="Not run yet."
        columns={[
          { key: 'd', header: 'Date', render: (r) => formatDate(r.runDate) },
          { key: 's', header: 'Outcome', render: (r) => <StatusBadge status={r.status} /> },
          { key: 'n', header: 'Rows', numeric: true, render: (r) => r.rowCount },
          { key: 'x', header: 'File or reason', render: (r) => r.detail ?? '' },
          { key: 't', header: 'Run at', render: (r) => formatDateTime(r.runAt) },
        ]}
      />
    </Card>
  );
}

/**
 * The report subscriptions of the user (G04): report, parameters, schedule, format and recipients,
 * with the runs of each and Stop. Subscribe from a report screen.
 */
export default function ReportSubscriptionsPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState<ReportSubscription | null>(null);
  const mine = useQuery({
    queryKey: ['report-subscriptions'],
    queryFn: () => reportSubscriptionApi.mine(),
  });
  const stop = useMutation({
    mutationFn: (s: ReportSubscription) => reportSubscriptionApi.deactivate(s.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['report-subscriptions'] }),
  });
  return (
    <div className="stack">
      <PageHeader
        section="Reports"
        backTo="/reports"
        title="My Report Subscriptions"
        description="Reports sent to you and your recipients on a schedule. Subscribe from a report screen."
      />
      <ErrorAlert error={mine.error ?? stop.error} />
      <Card flush>
        <DataTable<ReportSubscription>
          loading={mine.isLoading}
          rows={mine.data ?? []}
          rowKey={(s) => s.id}
          emptyMessage="No subscription yet."
          columns={[
            {
              key: 'r',
              header: 'Report',
              render: (s) => (
                <Link to={`/reports/${encodeURIComponent(s.reportCode)}`}>{s.reportCode}</Link>
              ),
            },
            {
              key: 'p',
              header: 'Parameters',
              render: (s) =>
                Object.entries(s.parameters)
                  .map(([k, v]) => `${k}: ${v}`)
                  .join(', '),
            },
            { key: 'f', header: 'Schedule', render: (s) => scheduleText(s) },
            { key: 'o', header: 'Format', render: (s) => s.format },
            { key: 'e', header: 'Recipients', render: (s) => s.recipients.join(', ') },
            { key: 'l', header: 'Last run', render: (s) => formatDate(s.lastRunDate) },
            {
              key: 's',
              header: 'Status',
              render: (s) => <StatusBadge status={s.active ? 'ACTIVE' : 'INACTIVE'} />,
            },
            {
              key: 'x',
              header: 'Actions',
              render: (s) => (
                <>
                  <Button size="sm" variant="ghost" onClick={() => setOpen(s)}>
                    Runs
                  </Button>
                  {s.active && (
                    <Button
                      size="sm"
                      variant="secondary"
                      busy={stop.isPending && stop.variables.id === s.id}
                      onClick={() => stop.mutate(s)}
                    >
                      Stop
                    </Button>
                  )}
                </>
              ),
            },
          ]}
        />
      </Card>
      {open && <Runs subscription={open} />}
    </div>
  );
}

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Play, RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { systemApi } from '@/api/system';
import type { JobRun, ScheduledJob } from '@/api/system';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';

/** Duration of a run in seconds ('' while running). */
function duration(run: JobRun): string {
  if (run.finishedAt === undefined) {
    return '';
  }
  const ms = new Date(run.finishedAt).getTime() - new Date(run.startedAt).getTime();
  return `${(ms / 1000).toFixed(1)} s`;
}

function RunStatus({ run }: Readonly<{ run?: JobRun }>) {
  if (run === undefined) {
    return <span className="muted">Never run</span>;
  }
  return (
    <span title={run.message}>
      <StatusBadge status={run.status} />
    </span>
  );
}

/**
 * Job scheduler monitor: every background job with its schedule, last and next run, a manual
 * "run now" and the run history (batch processing report).
 */
export default function ScheduledJobsPage() {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<string>();
  const jobs = useQuery({ queryKey: ['system', 'jobs'], queryFn: systemApi.jobs });
  const runs = useQuery({
    queryKey: ['system', 'runs', selected],
    queryFn: () => systemApi.runs(selected),
  });
  const run = useMutation({
    mutationFn: systemApi.runJob,
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['system'] });
      if (r.status === 'FAILED') {
        toast.error(`${r.jobName} failed: ${r.message ?? ''}`);
      } else {
        toast.success(`${r.jobName}: ${r.message ?? 'completed'}`);
      }
    },
  });
  const canRun = can('SYSTEM_PARAMETER_MANAGE');

  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Scheduled Jobs"
        description="Background jobs, their schedules (UTC) and execution history. Failed runs raise a JOB_FAILURE alert."
        actions={
          <Button
            variant="secondary"
            icon={<RefreshCw size={16} />}
            onClick={() => void queryClient.invalidateQueries({ queryKey: ['system'] })}
          >
            Refresh
          </Button>
        }
      />
      <ErrorAlert error={jobs.error ?? run.error} />
      <Card title="Jobs" flush>
        <DataTable<ScheduledJob>
          loading={jobs.isLoading}
          rows={jobs.data ?? []}
          rowKey={(j) => j.name}
          onRowClick={(j) => setSelected(j.name)}
          columns={[
            { key: 'n', header: 'Job', render: (j) => <strong>{j.name}</strong> },
            { key: 'd', header: 'Description', render: (j) => j.description },
            { key: 'c', header: 'Schedule', render: (j) => <code>{j.cron}</code> },
            { key: 'l', header: 'Last Run', render: (j) => formatDateTime(j.lastRun?.startedAt) },
            { key: 's', header: 'Status', render: (j) => <RunStatus run={j.lastRun} /> },
            { key: 'x', header: 'Next Run', render: (j) => formatDateTime(j.nextRun) },
            {
              key: 'a',
              header: 'Actions',
              render: (j) =>
                canRun && (
                  <Button
                    size="sm"
                    variant="secondary"
                    icon={<Play size={14} />}
                    busy={run.isPending && run.variables === j.name}
                    onClick={(e) => {
                      e.stopPropagation();
                      run.mutate(j.name);
                    }}
                  >
                    Run Now
                  </Button>
                ),
            },
          ]}
        />
      </Card>
      <Card
        title={selected === undefined ? 'Run history (all jobs)' : `Run history – ${selected}`}
        actions={
          selected !== undefined && (
            <Button size="sm" variant="ghost" onClick={() => setSelected(undefined)}>
              Show All Jobs
            </Button>
          )
        }
        flush
      >
        <DataTable<JobRun>
          loading={runs.isLoading}
          rows={runs.data?.content ?? []}
          rowKey={(r) => r.id}
          emptyMessage="No runs recorded in the history window."
          columns={[
            { key: 'j', header: 'Job', render: (r) => r.jobName },
            { key: 't', header: 'Started', render: (r) => formatDateTime(r.startedAt) },
            { key: 'd', header: 'Duration', numeric: true, render: duration },
            { key: 'g', header: 'Trigger', render: (r) => `${r.trigger} (${r.triggeredBy})` },
            { key: 's', header: 'Status', render: (r) => <RunStatus run={r} /> },
            { key: 'i', header: 'Items', numeric: true, render: (r) => r.itemsProcessed },
            { key: 'm', header: 'Message', render: (r) => r.message ?? '' },
          ]}
        />
      </Card>
    </div>
  );
}

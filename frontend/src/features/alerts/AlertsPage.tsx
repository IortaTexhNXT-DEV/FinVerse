import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { PlayCircle } from 'lucide-react';
import { useState } from 'react';
import { alertsApi } from '@/api/alerts';
import type { AlertFilters, AlertItem, AlertSeverity, AlertStatus } from '@/api/alerts';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Kpi } from '@/components/ui/Kpi';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { SeverityBadge } from './SeverityBadge';

const SEVERITIES: AlertSeverity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
const STATUSES: AlertStatus[] = ['OPEN', 'ACKNOWLEDGED', 'RESOLVED'];

interface Action {
  alert: AlertItem;
  kind: 'acknowledge' | 'resolve';
}

/**
 * Alerts inbox (exception log): conditions raised by the Exception Codes Master rules at posting
 * time and by the daily checks. Controllers acknowledge and resolve them with a comment.
 */
export default function AlertsPage() {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<AlertFilters>({ status: 'OPEN' });
  const [action, setAction] = useState<Action | null>(null);
  const [comment, setComment] = useState('');
  const canManage = can('ALERT_MANAGE');

  const alerts = useQuery({
    queryKey: ['alerts', filters],
    queryFn: () => alertsApi.search(filters),
  });
  const summary = useQuery({ queryKey: ['alerts', 'summary'], queryFn: alertsApi.summary });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['alerts'] });

  const act = useMutation({
    mutationFn: (a: Action) =>
      a.kind === 'acknowledge'
        ? alertsApi.acknowledge(a.alert.id, comment || undefined)
        : alertsApi.resolve(a.alert.id, comment),
    onSuccess: async (a) => {
      setAction(null);
      setComment('');
      await refresh();
      toast.success(`${a.exceptionCode} ${humanize(a.status).toLowerCase()}`);
    },
  });
  const runChecks = useMutation({
    mutationFn: alertsApi.runChecks,
    onSuccess: async (run) => {
      await refresh();
      toast.success(run.message ?? 'Checks completed');
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="Overview"
        title="Alerts"
        description="Exceptions raised by the control rules. Acknowledge an alert when you take it over and resolve it with a comment."
        actions={
          canManage && (
            <Button
              variant="accent"
              icon={<PlayCircle size={16} />}
              busy={runChecks.isPending}
              onClick={() => runChecks.mutate()}
            >
              Run checks now
            </Button>
          )
        }
      />
      <ErrorAlert error={alerts.error ?? runChecks.error} />
      <div className="grid-4">
        {SEVERITIES.map((s) => (
          <Kpi
            key={s}
            label={`${humanize(s)} (live)`}
            value={summary.data?.bySeverity[s] ?? 0}
            accent={s === 'CRITICAL'}
          />
        ))}
      </div>
      <Card>
        <div className="form-grid">
          <Field label="Status">
            {(id) => (
              <select
                id={id}
                className="select"
                value={filters.status ?? ''}
                onChange={(e) =>
                  setFilters({ ...filters, status: (e.target.value || undefined) as AlertStatus })
                }
              >
                <option value="">All</option>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {humanize(s)}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Severity">
            {(id) => (
              <select
                id={id}
                className="select"
                value={filters.severity ?? ''}
                onChange={(e) =>
                  setFilters({
                    ...filters,
                    severity: (e.target.value || undefined) as AlertSeverity,
                  })
                }
              >
                <option value="">All</option>
                {SEVERITIES.map((s) => (
                  <option key={s} value={s}>
                    {humanize(s)}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Exception code">
            {(id) => (
              <input
                id={id}
                className="input"
                value={filters.code ?? ''}
                onChange={(e) =>
                  setFilters({ ...filters, code: e.target.value.toUpperCase() || undefined })
                }
              />
            )}
          </Field>
        </div>
      </Card>
      <Card flush>
        <DataTable<AlertItem>
          loading={alerts.isLoading}
          rows={alerts.data?.content ?? []}
          rowKey={(a) => a.id}
          emptyMessage="No alerts match the filters."
          columns={[
            { key: 't', header: 'Raised', render: (a) => formatDateTime(a.raisedAt) },
            {
              key: 'v',
              header: 'Severity',
              render: (a) => <SeverityBadge severity={a.severity} />,
            },
            { key: 'c', header: 'Code', render: (a) => <strong>{a.exceptionCode}</strong> },
            { key: 'e', header: 'Record', render: (a) => a.entityId ?? '' },
            { key: 'm', header: 'Message', render: (a) => a.message },
            {
              key: 'a',
              header: 'Amount',
              numeric: true,
              render: (a) => (a.amount === undefined ? '' : <Amount value={a.amount} />),
            },
            { key: 's', header: 'Status', render: (a) => <StatusBadge status={a.status} /> },
            {
              key: 'h',
              header: 'Handled by',
              render: (a) => a.resolvedBy ?? a.acknowledgedBy ?? '',
            },
            {
              key: 'x',
              header: 'Actions',
              render: (a) =>
                canManage &&
                a.status !== 'RESOLVED' && (
                  <div className="row">
                    {a.status === 'OPEN' && (
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => setAction({ alert: a, kind: 'acknowledge' })}
                      >
                        Acknowledge
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => setAction({ alert: a, kind: 'resolve' })}
                    >
                      Resolve
                    </Button>
                  </div>
                ),
            },
          ]}
        />
      </Card>
      <Modal
        title={action?.kind === 'resolve' ? 'Resolve alert' : 'Acknowledge alert'}
        open={action !== null}
        onClose={() => setAction(null)}
        footer={
          <Button
            variant="accent"
            busy={act.isPending}
            disabled={action?.kind === 'resolve' && comment.trim() === ''}
            onClick={() => action && act.mutate(action)}
          >
            {action?.kind === 'resolve' ? 'Resolve' : 'Acknowledge'}
          </Button>
        }
      >
        <ErrorAlert error={act.error} />
        {action !== null && (
          <div className="stack">
            <p>{action.alert.message}</p>
            <Field label="Comment" required={action.kind === 'resolve'}>
              {(id) => (
                <textarea
                  id={id}
                  className="textarea"
                  maxLength={200}
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                />
              )}
            </Field>
          </div>
        )}
      </Modal>
    </div>
  );
}

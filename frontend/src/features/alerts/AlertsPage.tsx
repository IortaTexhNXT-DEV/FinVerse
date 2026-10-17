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
import { Combobox } from '@/components/ui/Combobox';
import { Field } from '@/components/ui/Field';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActions } from '@/components/ui/RowActions';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { AlertActionDialog } from './AlertActionDialog';
import type { AlertActionKind } from './AlertActionDialog';
import { alertRef, recordKind, ruleName, ruleNames } from './alertWording';
import { SeverityBadge } from './SeverityBadge';
import { UserName } from '@/components/ui/UserName';

const SEVERITIES: AlertSeverity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
const STATUSES: AlertStatus[] = ['OPEN', 'ACKNOWLEDGED', 'RESOLVED'];

interface Action {
  alert: AlertItem;
  kind: AlertActionKind;
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
  const canManage = can('ALERT_MANAGE');
  const codes = useQuery({ queryKey: ['exception-codes'], queryFn: alertsApi.codes });
  const names = ruleNames(codes.data);

  const alerts = useQuery({
    queryKey: ['alerts', filters],
    queryFn: () => alertsApi.search(filters),
  });
  const summary = useQuery({ queryKey: ['alerts', 'summary'], queryFn: alertsApi.summary });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['alerts'] });

  const act = useMutation({
    mutationFn: ({ a, comment }: { a: Action; comment: string }) =>
      a.kind === 'acknowledge'
        ? alertsApi.acknowledge(a.alert.id, comment || undefined)
        : alertsApi.resolve(a.alert.id, comment),
    onSuccess: async (a) => {
      setAction(null);
      await refresh();
      toast.success(
        `Alert ${alertRef(a.id)} (${ruleName(names, a.exceptionCode)}) ${humanize(a.status).toLowerCase()}`,
      );
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
        description="Exceptions raised by the control rules."
        actions={
          canManage && (
            <Button
              variant="accent"
              icon={<PlayCircle size={16} />}
              busy={runChecks.isPending}
              onClick={() => runChecks.mutate()}
            >
              Run Checks Now
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
          <Field label="Rule">
            {(id) => (
              <Combobox
                id={id}
                value={filters.code ?? ''}
                emptyLabel="All"
                loading={codes.isLoading}
                options={(codes.data ?? []).map((c) => ({
                  value: c.code,
                  label: c.name,
                  hint: humanize(c.module),
                }))}
                onChange={(code) => setFilters({ ...filters, code: code || undefined })}
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
            {
              key: 'n',
              header: 'Alert No. / Raised',
              kind: 'code',
              render: (a) => (
                <>
                  <strong>{alertRef(a.id)}</strong>
                  <span className="cell-sub">{formatDateTime(a.raisedAt)}</span>
                </>
              ),
            },
            {
              key: 'v',
              header: 'Severity',
              render: (a) => <SeverityBadge severity={a.severity} />,
            },
            { key: 'c', header: 'Rule', render: (a) => ruleName(names, a.exceptionCode) },
            {
              key: 'e',
              header: 'Record',
              render: (a) =>
                a.entityId === undefined || /^\d+$/.test(a.entityId) ? (
                  recordKind(a.entityType)
                ) : (
                  <>
                    <span className="mono">{a.entityId}</span>
                    <span className="cell-sub">{recordKind(a.entityType)}</span>
                  </>
                ),
            },
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
              header: 'Handled By',
              render: (a) => <UserName login={a.resolvedBy ?? a.acknowledgedBy} />,
            },
            {
              key: 'x',
              header: 'Actions',
              kind: 'actions',
              render: (a) => (
                <RowActions
                  record={alertRef(a.id)}
                  actions={[
                    {
                      label: 'Acknowledge',
                      hidden: !canManage || a.status !== 'OPEN',
                      onSelect: () => setAction({ alert: a, kind: 'acknowledge' }),
                    },
                    {
                      label: 'Resolve',
                      hidden: !canManage || a.status === 'RESOLVED',
                      onSelect: () => setAction({ alert: a, kind: 'resolve' }),
                    },
                  ]}
                />
              ),
            },
          ]}
        />
      </Card>
      {action !== null && (
        <AlertActionDialog
          alert={action.alert}
          kind={action.kind}
          rule={ruleName(names, action.alert.exceptionCode)}
          busy={act.isPending}
          error={act.error}
          onConfirm={(comment) => act.mutate({ a: action, comment })}
          onClose={() => {
            setAction(null);
            act.reset();
          }}
        />
      )}
    </div>
  );
}

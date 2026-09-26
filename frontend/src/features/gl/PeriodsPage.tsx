import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { periodApi } from '@/api/periods';
import type { Period } from '@/api/periods';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { defaultYear } from '@/features/closing/periodDefaults';
import { formatDate, formatDateTime, today } from '@/utils/format';
import { PeriodActionDialog } from './PeriodActionDialog';
import type { PendingPeriodAction } from './PeriodActionDialog';
import { PERIOD_ACTIONS } from './periodActions';
import type { PeriodAction } from './periodActions';

/** Financial calendar and period status console (open, soft close, close, reopen). */
export default function PeriodsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [yearId, setYearId] = useState<number | undefined>();
  const [pending, setPending] = useState<PendingPeriodAction | null>(null);

  const years = useQuery({
    queryKey: ['years', companyId],
    queryFn: () => periodApi.years(companyId),
    enabled: companyId > 0,
  });
  const selectedYear = yearId ?? defaultYear(years.data ?? [], today())?.id;
  const periods = useQuery({
    queryKey: ['periods', selectedYear],
    queryFn: () => periodApi.periods(selectedYear ?? 0),
    enabled: selectedYear !== undefined,
  });

  const run = useMutation({
    mutationFn: ({ period, action, reason }: PendingPeriodAction & { reason: string }) =>
      action === 'reopen' ? periodApi.reopen(period.id, reason) : periodApi[action](period.id),
    onSuccess: async (p) => {
      setPending(null);
      await queryClient.invalidateQueries({ queryKey: ['periods'] });
      toast.success(`Period ${p.name} is now ${p.status}`);
    },
  });
  const ask = (period: Period, action: PeriodAction) => {
    run.reset();
    setPending({ period, action });
  };
  const createYear = useMutation({
    mutationFn: () =>
      periodApi.createYear(
        companyId,
        (years.data?.[0]?.yearCode ?? new Date().getFullYear() - 1) + 1,
      ),
    onSuccess: async (y) => {
      await queryClient.invalidateQueries({ queryKey: ['years'] });
      setYearId(y.id);
      toast.success(`Fiscal year ${y.yearCode} created`);
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="General Ledger"
        title="Financial Periods"
        description="Only OPEN periods accept normal postings. CLOSING allows system and adjustment journals; CLOSED blocks all postings."
        actions={
          can('PERIOD_MANAGE') && (
            <Button
              variant="secondary"
              busy={createYear.isPending}
              onClick={() => createYear.mutate()}
            >
              Create Next Fiscal Year
            </Button>
          )
        }
      />
      <ErrorAlert error={createYear.error} />
      <PeriodActionDialog
        key={pending === null ? 'none' : `${pending.period.id}-${pending.action}`}
        pending={pending}
        busy={run.isPending}
        error={run.error}
        onCancel={() => setPending(null)}
        onConfirm={(reason) => pending !== null && run.mutate({ ...pending, reason })}
      />
      <div className="row">
        {years.data?.map((y) => (
          <Button
            key={y.id}
            size="sm"
            variant={y.id === selectedYear ? 'primary' : 'secondary'}
            onClick={() => setYearId(y.id)}
          >
            FY {y.yearCode} {y.status === 'CLOSED' ? '(closed)' : ''}
          </Button>
        ))}
      </div>
      <Card flush>
        <DataTable<Period>
          loading={periods.isLoading}
          rows={periods.data ?? []}
          rowKey={(p) => p.id}
          columns={[
            { key: 'n', header: 'Period', render: (p) => <strong>{p.name}</strong> },
            { key: 'from', header: 'From', render: (p) => formatDate(p.startDate) },
            { key: 'to', header: 'To', render: (p) => formatDate(p.endDate) },
            { key: 'st', header: 'Status', render: (p) => <StatusBadge status={p.status} /> },
            {
              key: 'by',
              header: 'Last Change',
              render: (p) =>
                p.statusChangedBy
                  ? `${p.statusChangedBy} · ${formatDateTime(p.statusChangedAt)}`
                  : '',
            },
            { key: 'r', header: 'Reason', render: (p) => p.statusReason ?? '' },
            {
              key: 'a',
              header: 'Actions',
              render: (p) =>
                can('PERIOD_MANAGE') && (
                  <div className="row">
                    {PERIOD_ACTIONS[p.status].map((a) => (
                      <Button
                        key={a.action}
                        size="sm"
                        variant="secondary"
                        onClick={() => ask(p, a.action)}
                      >
                        {a.label}
                      </Button>
                    ))}
                  </div>
                ),
            },
          ]}
        />
      </Card>
    </div>
  );
}

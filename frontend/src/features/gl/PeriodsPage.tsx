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
import { formatDate, formatDateTime } from '@/utils/format';

type PeriodAction = 'open' | 'startClosing' | 'close' | 'reopen';

const ACTIONS: Record<Period['status'], { action: PeriodAction; label: string }[]> = {
  FUTURE: [{ action: 'open', label: 'Open' }],
  OPEN: [
    { action: 'startClosing', label: 'Start closing' },
    { action: 'close', label: 'Close' },
  ],
  CLOSING: [
    { action: 'open', label: 'Back to open' },
    { action: 'close', label: 'Close' },
  ],
  CLOSED: [{ action: 'reopen', label: 'Reopen' }],
  REOPENED: [{ action: 'close', label: 'Close again' }],
};

/** Financial calendar and period status console (open, soft close, close, reopen). */
export default function PeriodsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [yearId, setYearId] = useState<number | undefined>();

  const years = useQuery({
    queryKey: ['years', companyId],
    queryFn: () => periodApi.years(companyId),
    enabled: companyId > 0,
  });
  const selectedYear = yearId ?? years.data?.[0]?.id;
  const periods = useQuery({
    queryKey: ['periods', selectedYear],
    queryFn: () => periodApi.periods(selectedYear ?? 0),
    enabled: selectedYear !== undefined,
  });

  const run = useMutation({
    mutationFn: ({ period, action }: { period: Period; action: PeriodAction }) => {
      if (action === 'reopen') {
        const reason = globalThis.prompt(`Reason for reopening ${period.name}`) ?? '';
        return periodApi.reopen(period.id, reason);
      }
      return periodApi[action](period.id);
    },
    onSuccess: async (p) => {
      await queryClient.invalidateQueries({ queryKey: ['periods'] });
      toast.success(`Period ${p.name} is now ${p.status}`);
    },
  });
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
              Create next fiscal year
            </Button>
          )
        }
      />
      <ErrorAlert error={run.error ?? createYear.error} />
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
              header: 'Last change',
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
                    {ACTIONS[p.status].map((a) => (
                      <Button
                        key={a.action}
                        size="sm"
                        variant="secondary"
                        onClick={() => run.mutate({ period: p, action: a.action })}
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

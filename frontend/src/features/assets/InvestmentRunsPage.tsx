import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { investmentsApi } from '@/api/investments';
import type { InvestmentRun, RunLine, RunType } from '@/api/investments';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate, formatDateTime, humanize, today } from '@/utils/format';
import { periodOf } from './assetMath';
import { TextInput } from './FormControls';
import { summarize } from './runSummary';
import { useAssetLookups } from './useAssetLookups';

const TABS = [
  { id: 'ACCRUAL', label: 'Interest accrual' },
  { id: 'AMORTIZATION', label: 'Premium / discount amortization' },
] as const;

const EXPLANATION: Record<RunType, string> = {
  ACCRUAL:
    'Coupon interest from the last accrual to the period end (or maturity): face value x coupon rate x days / 365 (Actual/365) or / 360 (30E/360).',
  AMORTIZATION:
    'Effective interest: carrying amount x ((1 + effective rate)^(days/365) - 1) less the coupon accrued; straight line: remaining discount x days / days to maturity. The maturity period amortizes the remainder.',
};

/** Month-end interest accrual and amortization runs: preview, then post once per period. */
export default function InvestmentRunsPage() {
  const { companyId } = useAssetLookups();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [type, setType] = useState<RunType>('ACCRUAL');
  const [period, setPeriod] = useState(periodOf(today()));
  const preview = useQuery({
    queryKey: ['investment-run-preview', companyId, type, period],
    queryFn: () => investmentsApi.preview(companyId, type, period),
    enabled: companyId > 0 && period !== '',
  });
  const runs = useQuery({
    queryKey: ['investment-runs', companyId],
    queryFn: () => investmentsApi.runs(companyId),
    enabled: companyId > 0,
  });
  const post = useMutation({
    mutationFn: () => investmentsApi.postRun(companyId, type, period),
    onSuccess: async (run) => {
      await queryClient.invalidateQueries({ queryKey: ['investment-run-preview'] });
      await queryClient.invalidateQueries({ queryKey: ['investment-runs'] });
      await queryClient.invalidateQueries({ queryKey: ['holdings'] });
      toast.success(
        `${humanize(run.runType)} ${run.period} posted: ${formatAmount(run.totalAmount)}`,
      );
    },
  });
  const summary = summarize(preview.data);

  return (
    <div className="stack">
      <PageHeader
        section="Assets & Investments"
        title="Accrual & Amortization"
        description="Month-end investment income: one journal per holding, valued at the period end. Each run is posted once per period."
        actions={
          can('PERIOD_END_RUN') && (
            <Button
              variant="accent"
              busy={post.isPending}
              disabled={!summary.postable}
              onClick={() => post.mutate()}
            >
              Post {type === 'ACCRUAL' ? 'accrual' : 'amortization'}
            </Button>
          )
        }
      />
      <Tabs tabs={TABS} active={type} onChange={setType} />
      <Card>
        <p className="muted">{EXPLANATION[type]}</p>
        <div className="form-grid">
          <TextInput label="Period" type="month" required value={period} onChange={setPeriod} />
          <Kpi label="Holdings" value={summary.count} />
          <Kpi label="Total" accent value={formatAmount(summary.total)} />
          <Kpi label="Status" value={<StatusBadge status={summary.status} />} />
        </div>
      </Card>
      <ErrorAlert error={preview.error ?? post.error} />
      <Card title="Amounts per holding" flush>
        <DataTable<RunLine>
          loading={preview.isLoading}
          rows={summary.lines}
          rowKey={(l) => l.holdingId}
          emptyMessage="Nothing due for this period."
          columns={[
            { key: 'h', header: 'Holding', render: (l) => <strong>{l.holdingNo}</strong> },
            { key: 'd', header: 'Description', render: (l) => l.description },
            { key: 'f', header: 'From', render: (l) => formatDate(l.fromDate) },
            { key: 't', header: 'To', render: (l) => formatDate(l.toDate) },
            { key: 'n', header: 'Days', numeric: true, render: (l) => l.days },
            {
              key: 'a',
              header: 'Amount',
              numeric: true,
              render: (l) => <Amount value={l.amount} />,
            },
            { key: 'j', header: 'Journal', render: (l) => l.batchNo ?? '' },
          ]}
        />
      </Card>
      <Card title="Posted runs" flush>
        <DataTable<InvestmentRun>
          loading={runs.isLoading}
          rows={runs.data ?? []}
          rowKey={(r) => r.id}
          onRowClick={(r) => {
            setType(r.runType);
            setPeriod(r.period);
          }}
          columns={[
            { key: 'p', header: 'Period', render: (r) => <strong>{r.period}</strong> },
            { key: 't', header: 'Run', render: (r) => humanize(r.runType) },
            { key: 'n', header: 'Holdings', numeric: true, render: (r) => r.holdingCount },
            {
              key: 'a',
              header: 'Total',
              numeric: true,
              render: (r) => <Amount value={r.totalAmount} />,
            },
            { key: 'u', header: 'Posted by', render: (r) => r.createdBy },
            { key: 'w', header: 'Posted at', render: (r) => formatDateTime(r.createdAt) },
          ]}
        />
      </Card>
    </div>
  );
}

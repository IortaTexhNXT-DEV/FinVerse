import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { assetsApi } from '@/api/assets';
import type { DepreciationLine, DepreciationRun } from '@/api/assets';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDateTime, today } from '@/utils/format';
import { periodOf } from './assetMath';
import { TextInput } from './FormControls';
import { summarize } from './runSummary';
import { useAssetLookups } from './useAssetLookups';

/** Monthly depreciation: preview the charge of a period, then post it once (idempotent). */
export default function DepreciationRunPage() {
  const { companyId, branchName } = useAssetLookups();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [period, setPeriod] = useState(periodOf(today()));
  const preview = useQuery({
    queryKey: ['depreciation-preview', companyId, period],
    queryFn: () => assetsApi.preview(companyId, period),
    enabled: companyId > 0 && period !== '',
  });
  const runs = useQuery({
    queryKey: ['depreciation-runs', companyId],
    queryFn: () => assetsApi.runs(companyId),
    enabled: companyId > 0,
  });
  const post = useMutation({
    mutationFn: () => assetsApi.postRun(companyId, period),
    onSuccess: async (run) => {
      await queryClient.invalidateQueries({ queryKey: ['depreciation-preview'] });
      await queryClient.invalidateQueries({ queryKey: ['depreciation-runs'] });
      await queryClient.invalidateQueries({ queryKey: ['fixed-assets'] });
      toast.success(`Depreciation ${run.period} posted: ${formatAmount(run.totalDepreciation)}`);
    },
  });
  const summary = summarize(preview.data);

  return (
    <div className="stack">
      <PageHeader
        section="Assets & Investments"
        title="Depreciation Run"
        description="Charges every capitalized asset up to the period end (catching up missed months) with one journal per branch, category and cost centre. A period is posted only once."
        actions={
          can('PERIOD_END_RUN') && (
            <Button
              variant="accent"
              busy={post.isPending}
              disabled={!summary.postable}
              onClick={() => post.mutate()}
            >
              Post depreciation
            </Button>
          )
        }
      />
      <Card>
        <div className="form-grid">
          <TextInput label="Period" type="month" required value={period} onChange={setPeriod} />
          <Kpi label="Assets" value={summary.count} />
          <Kpi label="Depreciation" accent value={formatAmount(summary.total)} />
          <Kpi
            label="Status"
            value={<StatusBadge status={summary.status} />}
            hint={summary.postedBy === undefined ? 'Preview' : `by ${summary.postedBy}`}
          />
        </div>
      </Card>
      <ErrorAlert error={preview.error ?? post.error} />
      <Card title="Charges" flush>
        <DataTable<DepreciationLine>
          loading={preview.isLoading}
          rows={summary.lines}
          rowKey={(l) => l.assetId}
          emptyMessage="Nothing to depreciate for this period."
          columns={[
            { key: 't', header: 'Tag', render: (l) => <strong>{l.tagNo}</strong> },
            { key: 'd', header: 'Description', render: (l) => l.description },
            { key: 'c', header: 'Category', render: (l) => l.categoryCode },
            { key: 'b', header: 'Branch', render: (l) => branchName(l.branchId) },
            { key: 'cc', header: 'Cost centre', render: (l) => l.costCenter ?? '' },
            { key: 'm', header: 'Months', numeric: true, render: (l) => l.months },
            {
              key: 'a',
              header: 'Depreciation',
              numeric: true,
              render: (l) => <Amount value={l.amount} />,
            },
            {
              key: 'ac',
              header: 'Accumulated',
              numeric: true,
              render: (l) => <Amount value={l.accumulatedAfter} />,
            },
            {
              key: 'n',
              header: 'NBV',
              numeric: true,
              render: (l) => <Amount value={l.netBookValueAfter} />,
            },
            { key: 'j', header: 'Journal', render: (l) => l.batchNo ?? '' },
          ]}
        />
      </Card>
      <Card title="Posted runs" flush>
        <DataTable<DepreciationRun>
          loading={runs.isLoading}
          rows={runs.data ?? []}
          rowKey={(r) => r.id}
          onRowClick={(r) => setPeriod(r.period)}
          columns={[
            { key: 'p', header: 'Period', render: (r) => <strong>{r.period}</strong> },
            { key: 'n', header: 'Assets', numeric: true, render: (r) => r.assetCount },
            {
              key: 't',
              header: 'Depreciation',
              numeric: true,
              render: (r) => <Amount value={r.totalDepreciation} />,
            },
            { key: 'u', header: 'Posted by', render: (r) => r.createdBy },
            { key: 'w', header: 'Posted at', render: (r) => formatDateTime(r.createdAt) },
          ]}
        />
      </Card>
    </div>
  );
}

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Calculator } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { reservesApi } from '@/api/reserves';
import type { ValuationRun } from '@/api/reserves';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { TextInput } from '@/features/assets/FormControls';
import { formatDate, formatDateTime, today } from '@/utils/format';
import { monthEnd } from './reserveMath';

/** Valuation runs of the company: history and preview of a new month. */
export default function ValuationRunsPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [month, setMonth] = useState(today());

  const runs = useQuery({
    queryKey: ['reserve-runs', companyId],
    queryFn: () => reservesApi.runs(companyId),
    enabled: companyId > 0,
  });
  const create = useMutation({
    mutationFn: () => reservesApi.createRun(companyId, month),
    onSuccess: async (d) => {
      await queryClient.invalidateQueries({ queryKey: ['reserve-runs'] });
      toast.success(`Valuation run ${d.run.periodName} calculated (preview)`);
      void navigate(`/reserves/runs/${d.run.id}`);
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="Actuarial Reserves"
        title="Valuation Runs"
        description="Monthly valuation of UPR, DAC / UCR, OSLR, IBNR, ULAE, MfAD and premium deficiency. A run is previewed by the preparer, approved by a checker and posted as movement journals against the previous posted run."
      />
      {can('RESERVE_PREPARE') && (
        <Card title="New valuation run">
          <div className="form-grid">
            <TextInput
              label="Valuation month (any date)"
              type="date"
              value={month}
              onChange={setMonth}
              hint={month ? `Valuation date ${formatDate(monthEnd(month))}` : undefined}
            />
            <Button
              variant="accent"
              icon={<Calculator size={16} />}
              busy={create.isPending}
              disabled={month === '' || companyId === 0}
              onClick={() => create.mutate()}
              style={{ alignSelf: 'end' }}
            >
              Calculate Preview
            </Button>
          </div>
        </Card>
      )}
      <ErrorAlert error={create.error ?? runs.error} />
      <Card title="Runs" flush>
        <DataTable<ValuationRun>
          loading={runs.isLoading}
          rows={runs.data ?? []}
          rowKey={(r) => r.id}
          onRowClick={(r) => void navigate(`/reserves/runs/${r.id}`)}
          emptyMessage="No valuation run yet."
          columns={[
            { key: 'p', header: 'Month', render: (r) => <strong>{r.periodName}</strong> },
            { key: 'd', header: 'Valuation Date', render: (r) => formatDate(r.valuationDate) },
            { key: 's', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
            { key: 'm', header: 'Prepared by', render: (r) => r.submittedBy ?? r.preparedBy },
            { key: 'a', header: 'Approved by', render: (r) => r.approvedBy ?? '' },
            { key: 'j', header: 'Journals', numeric: true, render: (r) => r.journalCount },
            { key: 'c', header: 'Calculated', render: (r) => formatDateTime(r.calculatedAt) },
          ]}
        />
      </Card>
    </div>
  );
}

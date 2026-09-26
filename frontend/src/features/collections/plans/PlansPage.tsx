import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize } from '@/utils/format';
import type { NewPlan, Plan } from './api';
import { plansApi } from './api';
import type { PlanTab } from './labels';
import { PLAN_TABS, SOURCE_LABELS, statusesOf } from './labels';
import { NewPlanDialog } from './PlanDialogs';

const COLUMNS: Column<Plan>[] = [
  {
    key: 'no',
    header: 'Plan No.',
    render: (p) => (
      <>
        <strong>{p.planNo}</strong>
        <div className="muted">{p.invoiceNo ?? 'All policy years'}</div>
      </>
    ),
  },
  { key: 'arn', header: 'ARN', render: (p) => p.arn },
  { key: 'assured', header: 'Name of Assured', render: (p) => p.assuredName },
  { key: 'freq', header: 'Frequency', render: (p) => humanize(p.frequency) },
  {
    key: 'source',
    header: 'Basis',
    render: (p) => <span className="tag">{SOURCE_LABELS[p.source]}</span>,
  },
  { key: 'first', header: 'First Due', render: (p) => formatDate(p.firstDue) },
  { key: 'count', header: 'Installments', numeric: true, render: (p) => p.installmentCount },
  { key: 'total', header: 'Total', numeric: true, render: (p) => <Amount value={p.total} /> },
  { key: 'paid', header: 'Paid', numeric: true, render: (p) => <Amount value={p.paidTotal} /> },
  { key: 'status', header: 'Status', render: (p) => <StatusBadge status={p.status} /> },
];

/**
 * Installment plans of collection accounts (BRCLXN.053/058): the plans by status, and a new plan
 * over the policy years of a multi-year account or splitting one invoice (CLX_BILLING).
 */
export default function PlansPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<PlanTab>('ACTIVE');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const rows = useQuery({
    queryKey: ['collections', 'plans', companyId, tab, query, page],
    queryFn: () => plansApi.plans(companyId, statusesOf(PLAN_TABS, tab), query, page),
    enabled: companyId > 0,
  });
  const create = useMutation({
    mutationFn: (plan: NewPlan) => plansApi.create(plan),
    onSuccess: async (plan) => {
      setCreating(false);
      await queryClient.invalidateQueries({ queryKey: ['collections', 'plans'] });
      toast.success(`${plan.planNo} created with ${plan.installmentCount} installments`);
      void navigate(`/collections/plans/${plan.id}`);
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections"
        title="Installment Plans"
        description="Billing cycles of multi-year and installment accounts, with the payments allocated from the invoice ledger."
        actions={
          can('CLX_BILLING') ? (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => setCreating(true)}>
              New Installment Plan
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={rows.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={PLAN_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          <WorklistToolbar
            placeholder="Search Plan No., ARN or Invoice No."
            onSearch={(text) => {
              setQuery(text);
              setPage(0);
            }}
          />
          <DataTable
            caption="Installment plans"
            columns={COLUMNS}
            rows={rows.data?.content ?? []}
            rowKey={(p) => p.id}
            loading={rows.isLoading}
            emptyMessage="No installment plans to display"
            onRowClick={(p) => void navigate(`/collections/plans/${p.id}`)}
          />
          <PageFooter data={rows.data} noun="plans" onPage={setPage} />
        </div>
      </Card>
      {creating && (
        <NewPlanDialog
          companyId={companyId}
          busy={create.isPending}
          error={create.error}
          onClose={() => setCreating(false)}
          onSave={(plan) => create.mutate(plan)}
        />
      )}
    </div>
  );
}

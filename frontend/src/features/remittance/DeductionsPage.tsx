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
import { DeductionDialog } from './DeductionDialog';
import { DEDUCTION_TABS, inputOf } from './deductionForm';
import type { DeductionForm, DeductionTab } from './deductionForm';
import { deductionsApi } from './deductionsApi';
import type { Deduction } from './deductionsApi';
import { stagesOf } from './remittanceLabels';
import './remittance.css';

const COLUMNS: Column<Deduction>[] = [
  {
    key: 'no',
    header: 'Deduction No.',
    render: (d) => (
      <>
        <strong>{d.deductionNo}</strong>
        <div className="remit-muted">{d.createdBy}</div>
      </>
    ),
  },
  { key: 'ins', header: 'Insurer', render: (d) => `${d.insurerCode} · ${d.currency}` },
  {
    key: 'source',
    header: 'Source',
    render: (d) => (
      <>
        {humanize(d.sourceType)}
        <div className="remit-muted">{d.sourceRef}</div>
      </>
    ),
  },
  {
    key: 'conf',
    header: 'Insurer Confirmation',
    render: (d) =>
      d.confirmationRef === undefined
        ? 'Not recorded'
        : `${d.confirmationRef} · ${formatDate(d.confirmationDate)}`,
  },
  { key: 'amt', header: 'Amount', numeric: true, render: (d) => <Amount value={d.amount} /> },
  {
    key: 'left',
    header: 'Remaining',
    numeric: true,
    render: (d) => <Amount value={d.remaining} />,
  },
  { key: 'stage', header: 'Status', render: (d) => <StatusBadge status={d.stage} /> },
];

/**
 * Remittance deductions (ACSL 2.9.2): ACSL records an amount the insurer confirmed in writing (an
 * AR insurer's refund, an over-remittance), another user confirms it, and the next approved
 * batches of that insurer deduct it, never more than they pay.
 */
export default function DeductionsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<DeductionTab>('CONFIRMATION');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const rows = useQuery({
    queryKey: ['remittance', 'deductions', companyId, tab, query, page],
    queryFn: () =>
      deductionsApi.list(companyId, stagesOf(DEDUCTION_TABS, tab), { q: query, insurer: '' }, page),
    enabled: companyId > 0,
  });
  const create = useMutation({
    mutationFn: (form: DeductionForm) => deductionsApi.create(inputOf(companyId, form)),
    onSuccess: async (d) => {
      setCreating(false);
      await queryClient.invalidateQueries({ queryKey: ['remittance', 'deductions'] });
      toast.success(`${d.deductionNo} created`);
      void navigate(`/remittance/deductions/${d.id}`);
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Remittance"
        title="Remittance Deductions"
        description="Amounts the insurer confirmed, deducted from its next remittance batches and capped at what each batch pays."
        actions={
          can('ACSL_PROCESS') ? (
            <Button icon={<Plus size={16} />} onClick={() => setCreating(true)}>
              New Deduction
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={rows.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={DEDUCTION_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          <WorklistToolbar
            placeholder="Search Deduction No. or Source Reference"
            onSearch={(text) => {
              setQuery(text);
              setPage(0);
            }}
          />
          <DataTable
            caption="Remittance deductions"
            columns={COLUMNS}
            rows={rows.data?.content ?? []}
            rowKey={(d) => d.id}
            loading={rows.isLoading}
            emptyMessage="No deductions in this status"
            onRowClick={(d) => void navigate(`/remittance/deductions/${d.id}`)}
          />
          <PageFooter data={rows.data} noun="deductions" onPage={setPage} />
        </div>
      </Card>
      {creating && (
        <DeductionDialog
          title="New Remittance Deduction"
          busy={create.isPending}
          error={create.error}
          onClose={() => setCreating(false)}
          onSave={(form) => create.mutate(form)}
        />
      )}
    </div>
  );
}

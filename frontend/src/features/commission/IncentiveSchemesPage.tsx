import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize } from '@/utils/format';
import { commissionApi } from './commissionApi';
import type { Scheme, SchemeTerms } from './commissionApi';
import { describeTier } from './commissionLogic';
import { SchemeDialog } from './SchemeDialog';

const COLUMNS: Column<Scheme>[] = [
  {
    key: 'name',
    header: 'Scheme',
    render: (s) => (
      <>
        <strong>{s.terms.name}</strong>
        <div className="muted">{s.code}</div>
      </>
    ),
  },
  { key: 'type', header: 'Type', render: (s) => humanize(s.terms.schemeType) },
  { key: 'insurer', header: 'Insurer', render: (s) => s.terms.insurerCode ?? 'All' },
  { key: 'period', header: 'Period', render: (s) => humanize(s.terms.periodType) },
  { key: 'beneficiary', header: 'Beneficiary', render: (s) => humanize(s.terms.beneficiary) },
  {
    key: 'tiers',
    header: 'Tiers',
    render: (s) =>
      s.terms.tiers.length === 0
        ? 'No tiers yet'
        : s.terms.tiers.map((t) => describeTier(s.terms.calculation, t)).join('; '),
  },
  {
    key: 'effective',
    header: 'Effective',
    render: (s) => `${formatDate(s.terms.effectiveFrom)} – ${formatDate(s.terms.effectiveTo)}`,
  },
  {
    key: 'status',
    header: 'Status',
    render: (s) => <StatusBadge status={s.terms.active ? 'ACTIVE' : 'INACTIVE'} />,
  },
];

/**
 * Incentive schemes (CMRID.003/005/006): the insurer incentive programmes (No Touch, Top Up,
 * Motor Mania...) with their production target tiers or fixed amounts per policy.
 */
export default function IncentiveSchemesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<{ scheme?: Scheme }>();
  const schemes = useQuery({
    queryKey: ['commission', 'schemes', companyId],
    queryFn: () => commissionApi.schemes(companyId),
    enabled: companyId > 0,
  });
  const save = useMutation({
    mutationFn: (v: { code: string; terms: SchemeTerms }) =>
      editing?.scheme
        ? commissionApi.updateScheme(editing.scheme.id, v.terms)
        : commissionApi.createScheme(companyId, v.code, v.terms),
    onSuccess: async (s) => {
      setEditing(undefined);
      await queryClient.invalidateQueries({ queryKey: ['commission', 'schemes'] });
      toast.success(`${s.terms.name} saved`);
    },
  });
  const mayEdit = can('INCENTIVE_MANAGE');
  return (
    <div className="stack">
      <PageHeader
        section="Commission Receivables"
        title="Incentive Schemes"
        description="Insurer incentive programmes with their targets, rates and amounts per policy."
        actions={
          mayEdit ? (
            <Button icon={<Plus size={16} />} onClick={() => setEditing({})}>
              New Scheme
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={schemes.error} />
      <Card>
        <DataTable
          caption="Incentive schemes"
          columns={COLUMNS}
          rows={schemes.data ?? []}
          rowKey={(s) => s.id}
          loading={schemes.isLoading}
          onRowClick={mayEdit ? (s) => setEditing({ scheme: s }) : undefined}
          emptyMessage="No items to display"
        />
      </Card>
      {editing !== undefined && (
        <SchemeDialog
          scheme={editing.scheme}
          busy={save.isPending}
          error={save.error}
          onClose={() => setEditing(undefined)}
          onSave={(code, terms) => save.mutate({ code, terms })}
        />
      )}
    </div>
  );
}

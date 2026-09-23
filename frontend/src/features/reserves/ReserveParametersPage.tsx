import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { reservesApi } from '@/api/reserves';
import type { ReserveParameter } from '@/api/reserves';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize, today } from '@/utils/format';
import { ParameterFormModal } from './ParameterFormModal';
import { NEW_PARAMETERS } from './reserveMath';
import type { ParameterForm } from './reserveMath';
import { TakafulSettingsCard } from './TakafulSettingsCard';
import { awaitsOtherChecker } from '@/utils/makerChecker';

function method(p: ReserveParameter): string {
  return p.ibnrMethod === 'RATE'
    ? `Rate ${p.ibnrRate}%`
    : `Chain-ladder ${humanize(p.triangleBasis)} / ${humanize(p.developmentPeriod)} × ${p.accidentPeriods}`;
}

/** Reserve parameters per line of business (effective dated, maker-checker) and takaful. */
export default function ReserveParametersPage() {
  const companyId = useCompanyId();
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<ParameterForm | null>(null);
  const list = useQuery({
    queryKey: ['reserve-parameters', companyId],
    queryFn: () => reservesApi.parameters(companyId),
    enabled: companyId > 0,
  });
  const action = useMutation({
    mutationFn: ({ id, kind }: { id: number; kind: 'authorize' | 'deactivate' }) =>
      kind === 'authorize'
        ? reservesApi.authorizeParameter(id)
        : reservesApi.deactivateParameter(id),
    onSuccess: async (p) => {
      await queryClient.invalidateQueries({ queryKey: ['reserve-parameters'] });
      toast.success(
        `Parameters ${p.businessLine} from ${p.effectiveFrom}: ${humanize(p.recordStatus)}`,
      );
    },
  });
  const pending = (p: ReserveParameter) => p.recordStatus === 'PENDING_AUTHORIZATION';

  return (
    <div className="stack">
      <PageHeader
        section="Actuarial Reserves"
        title="Reserve Parameters"
        description="IBNR method (rate or chain-ladder), margins, ULAE, expected loss ratio and reinsurance commission per line of business. A change is a new record with a later effective date; valuation runs use the authorized record in force at their date."
        actions={
          can('MASTER_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => setForm({ ...NEW_PARAMETERS, effectiveFrom: today() })}
            >
              New parameters
            </Button>
          )
        }
      />
      <ErrorAlert error={list.error ?? action.error} />
      <Card title="Parameters by line of business" flush>
        <DataTable<ReserveParameter>
          loading={list.isLoading}
          rows={list.data ?? []}
          rowKey={(p) => p.id}
          onRowClick={can('MASTER_MAINTAIN') ? (p) => pending(p) && setForm({ ...p }) : undefined}
          emptyMessage="No reserve parameters: IBNR and margins are zero until parameters are authorized."
          columns={[
            { key: 'l', header: 'Line', render: (p) => <strong>{p.businessLine}</strong> },
            { key: 'e', header: 'Effective from', render: (p) => formatDate(p.effectiveFrom) },
            { key: 'm', header: 'IBNR method', render: method },
            { key: 'f', header: 'MfAD %', numeric: true, render: (p) => p.mfadPct },
            { key: 'u', header: 'ULAE %', numeric: true, render: (p) => p.ulaePct },
            { key: 'r', header: 'Loss ratio %', numeric: true, render: (p) => p.expectedLossRatio },
            {
              key: 'c',
              header: 'RI comm. treaty / FAC %',
              numeric: true,
              render: (p) => `${p.treatyCommissionPct} / ${p.facCommissionPct}`,
            },
            { key: 's', header: 'Status', render: (p) => <StatusBadge status={p.recordStatus} /> },
            { key: 'k', header: 'Maker', render: (p) => p.maker },
            {
              key: 'x',
              header: 'Actions',
              render: (p) => (
                <div className="row" style={{ gap: 'var(--space-1)' }}>
                  {awaitsOtherChecker(p, user?.username) && can('MASTER_AUTHORIZE') && (
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={(e) => {
                        e.stopPropagation();
                        action.mutate({ id: p.id, kind: 'authorize' });
                      }}
                    >
                      Authorize
                    </Button>
                  )}
                  {p.recordStatus === 'ACTIVE' && can('MASTER_MAINTAIN') && (
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={(e) => {
                        e.stopPropagation();
                        action.mutate({ id: p.id, kind: 'deactivate' });
                      }}
                    >
                      Deactivate
                    </Button>
                  )}
                </div>
              ),
            },
          ]}
        />
      </Card>
      <TakafulSettingsCard companyId={companyId} />
      <ParameterFormModal companyId={companyId} form={form} onChange={setForm} />
    </div>
  );
}

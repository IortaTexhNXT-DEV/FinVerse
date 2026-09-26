import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { reinsuranceApi } from '@/api/reinsurance';
import type { ClaimMovement } from '@/api/reinsurance';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { DateField } from '@/features/underwriting/FormFields';
import { formatDate, humanize, today } from '@/utils/format';
import { sharesByLayer } from './allocation';

/** Reinsurers' share of claim reserves, recoveries on payments and salvage shared back. */
export default function ClaimRecoveriesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [from, setFrom] = useState(`${today().slice(0, 4)}-01-01`);
  const [to, setTo] = useState(today());
  const movements = useQuery({
    queryKey: ['ri-claims', companyId, from, to],
    queryFn: () => reinsuranceApi.claimMovements(companyId, from, to),
    enabled: companyId > 0 && from !== '' && to !== '',
  });
  const catchUp = useMutation({
    mutationFn: () => reinsuranceApi.catchUp(companyId, from, to),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['ri-claims'] });
      toast.success(`${r.processed} claim movement(s) processed`);
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="Reinsurance"
        title="Claims Recoveries"
        description="Every posted claim movement with the reinsurers' share: reserve shares, recoveries due on payments (open items on the reinsurers), salvage shared back and excess of loss recoveries."
        actions={
          can('REINSURANCE_MAINTAIN') && (
            <Button
              variant="secondary"
              icon={<RefreshCw size={16} />}
              busy={catchUp.isPending}
              onClick={() => catchUp.mutate()}
            >
              Process Missed Movements
            </Button>
          )
        }
      />
      <ErrorAlert error={movements.error ?? catchUp.error} />
      <Card>
        <div className="form-grid">
          <DateField label="Movement date from" required value={from} onChange={setFrom} />
          <DateField label="Movement date to" required value={to} onChange={setTo} />
        </div>
      </Card>
      <Card flush>
        <DataTable<ClaimMovement>
          rows={movements.data ?? []}
          loading={movements.isLoading}
          rowKey={(m) => m.id}
          caption="Reinsurance share of claim movements"
          emptyMessage="No claim movement in the period."
          columns={[
            { key: 'c', header: 'Claim', render: (m) => <strong>{m.claimNo}</strong> },
            { key: 'l', header: 'Class', render: (m) => m.businessLine },
            { key: 'd', header: 'Loss Date', render: (m) => formatDate(m.lossDate) },
            { key: 'md', header: 'Date', render: (m) => formatDate(m.movementDate) },
            { key: 't', header: 'Movement', render: (m) => humanize(m.movementType) },
            {
              key: 'a',
              header: 'Amount',
              numeric: true,
              render: (m) => <Amount value={m.baseAmount} />,
            },
            {
              key: 'q',
              header: 'QS + Surplus',
              numeric: true,
              render: (m) => {
                const s = sharesByLayer(m);
                return <Amount value={(s.QUOTA_SHARE ?? 0) + (s.SURPLUS ?? 0)} />;
              },
            },
            {
              key: 'f',
              header: 'FAC',
              numeric: true,
              render: (m) => <Amount value={sharesByLayer(m).FAC ?? 0} />,
            },
            {
              key: 'x',
              header: 'XOL',
              numeric: true,
              render: (m) => <Amount value={sharesByLayer(m).XOL ?? 0} />,
            },
            {
              key: 'r',
              header: 'Reinsurers',
              numeric: true,
              render: (m) => <Amount value={m.ceded} />,
            },
            {
              key: 'n',
              header: 'Net Retained',
              numeric: true,
              render: (m) => <Amount value={m.netRetained} />,
            },
          ]}
        />
      </Card>
    </div>
  );
}

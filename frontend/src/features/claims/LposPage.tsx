import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { claimsApi } from '@/api/claims';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { LpoTable } from './LpoTable';

/** LPO register: every local purchase order issued to garages, newest first. */
export default function LposPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const lpos = useQuery({
    queryKey: ['claim-lpos', companyId],
    queryFn: () => claimsApi.lpoRegister(companyId),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Claims"
        title="Local purchase orders"
        description="Repair orders issued to garages under motor claims. Issue new LPOs from the claim."
      />
      <ErrorAlert error={lpos.error} />
      <Card flush>
        <LpoTable
          lpos={lpos.data ?? []}
          loading={lpos.isLoading}
          showClaim
          onChange={() => queryClient.invalidateQueries({ queryKey: ['claim-lpos'] })}
          onOpenClaim={(l) => void navigate(`/claims/${String(l.claimId)}`)}
        />
      </Card>
    </div>
  );
}

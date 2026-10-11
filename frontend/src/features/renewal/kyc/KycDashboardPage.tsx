import { UserName } from '@/components/ui/UserName';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { KYC_STATUS, renewalProposalApi } from '@/api/renewalProposal';
import type { KycAccount } from '@/api/renewalProposal';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import '../renewal.css';

const CARDS = [
  { status: 'KYC_DUE', key: 'due', label: 'Total Accounts Due for KYC Review' },
  { status: 'KYC_UPCOMING', key: 'upcoming', label: 'Total Accounts with Upcoming KYC Reviews' },
  { status: 'KYC_COMPLETED', key: 'completed', label: 'Total Accounts Completed KYC' },
  { status: 'PENDING_FOLLOW_UP', key: 'follow_up', label: 'Total Accounts Pending Follow-up' },
] as const;

/**
 * KYC Monitoring Dashboard (FRRN.039.03): the accounts due for KYC review, with upcoming reviews,
 * completed and pending follow-up, each count opening the accounts.
 */
export default function KycDashboardPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<string>('KYC_DUE');
  const counts = useQuery({
    queryKey: ['renewal', 'kyc', 'dashboard', companyId],
    queryFn: () => renewalProposalApi.kycDashboard(companyId),
  });
  const accounts = useQuery({
    queryKey: ['renewal', 'kyc', 'accounts', companyId, status],
    queryFn: () => renewalProposalApi.kycAccounts(companyId, status),
  });
  const refresh = useMutation({
    mutationFn: () => renewalProposalApi.refreshKyc(companyId),
    onSuccess: async (r) => {
      toast.success(`${String(r.changed)} KYC status change(s)`);
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'kyc'] });
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="KYC Monitoring"
        description="Renewal accounts whose client is due for KYC review."
        actions={
          (can('RNW_DISPOSE') || can('RNW_PROCESS')) && (
            <Button variant="secondary" busy={refresh.isPending} onClick={() => refresh.mutate()}>
              Refresh KYC Status
            </Button>
          )
        }
      />
      <ErrorAlert error={counts.error ?? accounts.error ?? refresh.error} />
      <ul className="rnw-kpi-cards" aria-label="KYC counts">
        {CARDS.map((c) => (
          <li key={c.status}>
            <button
              type="button"
              className={`card kpi rnw-kpi-card${status === c.status ? ' active' : ''}`}
              aria-pressed={status === c.status}
              onClick={() => setStatus(c.status)}
            >
              <span className="kpi-label">{c.label}</span>
              <span className="kpi-value">{(counts.data?.[c.key] ?? 0).toLocaleString()}</span>
            </button>
          </li>
        ))}
      </ul>
      <Card title={KYC_STATUS[status] ?? status} flush>
        <DataTable<KycAccount>
          loading={accounts.isLoading}
          rows={accounts.data ?? []}
          rowKey={(a) => a.renewalRef}
          onRowClick={(a) =>
            void navigate(`/renewal/candidates/${encodeURIComponent(a.renewalRef)}`)
          }
          emptyMessage="No account"
          columns={[
            { key: 'ref', header: 'Reference Number', kind: 'code', render: (a) => a.renewalRef },
            { key: 'client', header: 'Client', render: (a) => a.clientName },
            {
              key: 'due',
              header: 'KYC Review Date',
              kind: 'date',
              render: (a) => formatDate(a.kycReviewDue),
            },
            {
              key: 'expiry',
              header: 'Expiry Date',
              kind: 'date',
              render: (a) => formatDate(a.expiryDate),
            },
            {
              key: 'ao',
              header: 'Account Officer',
              render: (a) => <UserName login={a.accountOfficer} />,
            },
          ]}
        />
      </Card>
    </div>
  );
}

import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import type { HomeCount } from '@/api/renewal';
import { WorkTiles } from '@/components/broking/WorkTiles';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { LandingUploads } from '../uploads/LandingUploads';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { BucketPill } from '../common/RenewalBits';
import { RENEWAL_SECTION, stageLink } from '../common/renewalCodes';
import '../renewal.css';

function CountTable({
  title,
  rows,
  pill,
  onOpen,
}: Readonly<{
  title: string;
  rows: HomeCount[];
  pill?: boolean;
  onOpen?: (row: HomeCount) => void;
}>) {
  return (
    <Card title={title} flush>
      <DataTable<HomeCount>
        rows={rows}
        rowKey={(r) => r.code}
        onRowClick={onOpen}
        emptyMessage="No renewals"
        columns={[
          {
            key: 'label',
            header: 'Status',
            render: (r) => (pill === true ? <BucketPill bucket={r.code} /> : r.label),
          },
          { key: 'count', header: 'Renewals', kind: 'amount', render: (r) => r.count },
        ]}
      />
    </Card>
  );
}

/**
 * Renewal Home (FR-RN-100): renewals due in 30, 60, 90 and 140 days, at risk, urgent, returned,
 * NRNS, insurer replies overdue and failed letters; the renewals by status and classification and
 * the workload of the officers, within the user's scope.
 */
export default function RenewalHomePage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const home = useQuery({
    queryKey: ['renewal', 'home', companyId],
    queryFn: () => renewalApi.home(companyId),
    enabled: companyId > 0,
  });
  const t = home.data?.tiles;
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Renewal Home"
        description="Expiring accounts in your scope: what is due, at risk and waiting on you."
      />
      <ErrorAlert error={home.error} onRetry={() => void home.refetch()} />
      {t && (
        <>
          <div className="rnw-tiles">
            <Kpi label="Due in 30 days" value={t.due30} accent />
            <Kpi label="Due in 60 days" value={t.due60} />
            <Kpi label="Due in 90 days" value={t.due90} />
            <Kpi label="Due in 140 days" value={t.due140} />
          </div>
          <WorkTiles
            label="Renewals needing attention"
            tiles={[
              {
                key: 'risk',
                label: 'Need attention',
                value: t.atRisk,
                alert: true,
                onClick: () => void navigate('/renewal/expiry?tab=ATTENTION'),
              },
              {
                key: 'urgent',
                label: 'Urgent',
                value: t.urgent,
                alert: true,
                onClick: () => void navigate('/renewal/expiry?tab=ALL'),
              },
              {
                key: 'returned',
                label: 'Returned',
                value: t.returned,
                alert: true,
                onClick: () => void navigate('/renewal/mine'),
              },
              {
                key: 'nrns',
                label: 'No Response (NRNS)',
                value: t.nrns,
                onClick: () => void navigate('/renewal/letters?tab=NRNS'),
              },
              {
                key: 'insurer',
                label: 'Insurer reply overdue',
                value: t.insurerOverdue,
                alert: true,
                onClick: () => void navigate('/renewal/processing?tab=WITH_INSURER'),
              },
              {
                key: 'letters',
                label: 'Letters failed',
                value: t.lettersFailed,
                alert: true,
                onClick: () => void navigate('/renewal/letters?tab=RA_GENERATED'),
              },
            ]}
          />
        </>
      )}
      <div className="rnw-grid">
        <CountTable
          title="By status"
          rows={home.data?.stages ?? []}
          onOpen={(r) => void navigate(stageLink(r.code))}
        />
        <CountTable title="By classification" rows={home.data?.buckets ?? []} pill />
        <Card title="Workload" flush>
          <DataTable
            rows={home.data?.workload ?? []}
            rowKey={(r) => `${r.role}-${r.username}`}
            emptyMessage="No renewals assigned"
            columns={[
              { key: 'user', header: 'Officer', render: (r) => <UserName login={r.username} /> },
              {
                key: 'role',
                header: 'Role',
                render: (r) => (r.role === 'PO' ? 'Processing' : 'Marketing'),
              },
              { key: 'count', header: 'Open renewals', kind: 'amount', render: (r) => r.count },
            ]}
          />
        </Card>
      </div>
      <LandingUploads />
    </div>
  );
}

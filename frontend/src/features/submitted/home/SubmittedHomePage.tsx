import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { submittedApi } from '@/api/submitted';
import { WorkTiles } from '@/components/broking/WorkTiles';
import type { WorkTile } from '@/components/broking/WorkTiles';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { MASTERLIST_TABS, SUBMITTED_SECTION } from '../common/submittedCodes';

/**
 * Submitted Policies Home (design section 12): the masterlist by tab and the work waiting in the
 * reviews, the renewal hand-off, the letters and the handling fees, within the user's scope.
 */
export default function SubmittedHomePage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const counts = useQuery({
    queryKey: ['submitted', 'counts', companyId],
    queryFn: () => submittedApi.counts(companyId),
    enabled: companyId > 0,
  });
  const home = useQuery({
    queryKey: ['submitted', 'home', companyId],
    queryFn: () => submittedApi.home(companyId),
    enabled: companyId > 0,
  });
  const masterlist: WorkTile[] = MASTERLIST_TABS.map((t) => ({
    key: t.id,
    label: t.label,
    value: counts.data?.[t.id] ?? 0,
    alert: t.id === 'FALLOUT' || t.id === 'MANUAL_DISPOSITION',
    onClick: () => navigate(`/submitted/masterlist?tab=${t.id}`),
  }));
  const h = home.data;
  const work: WorkTile[] = [
    {
      key: 'iaaf',
      label: 'IAAF for Approval',
      value: h?.iaafForApproval ?? 0,
      onClick: () => navigate('/submitted/reviews?tab=FOR_APPROVAL'),
    },
    {
      key: 'iaafApproved',
      label: 'IAAF to Send',
      value: h?.iaafApproved ?? 0,
      onClick: () => navigate('/submitted/reviews?tab=APPROVED'),
    },
    {
      key: 'tor',
      label: 'TOR for Approval',
      value: h?.torForApproval ?? 0,
      onClick: () => navigate('/submitted/tors'),
    },
    {
      key: 'pending',
      label: 'Hand-offs Pending',
      value: h?.renewalsPending ?? 0,
      alert: true,
      onClick: () => navigate('/submitted/renewals'),
    },
    {
      key: 'letters',
      label: 'Letters Refused',
      value: h?.lettersFailed ?? 0,
      alert: true,
      onClick: () => navigate('/submitted/letters'),
    },
    {
      key: 'billed',
      label: 'Fees to Tag',
      value: h?.feesBilled ?? 0,
      onClick: () => navigate('/submitted/fees'),
    },
    {
      key: 'tagged',
      label: 'Fees to Apply',
      value: h?.feesTagged ?? 0,
      onClick: () => navigate('/submitted/fees?tab=TAGGED'),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title="Submitted Policies"
        description="The bank-submitted policies from intake to renewal, with the reviews, letters and fees."
      />
      <ErrorAlert error={counts.error ?? home.error} onRetry={() => void counts.refetch()} />
      <Card title="Masterlist">
        <WorkTiles tiles={masterlist} label="Masterlist by tab" />
      </Card>
      <Card title="Work Waiting">
        <WorkTiles tiles={work} label="Work waiting" />
      </Card>
    </div>
  );
}

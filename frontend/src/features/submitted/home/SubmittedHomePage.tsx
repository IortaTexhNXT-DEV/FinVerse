import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { submittedApi } from '@/api/submitted';
import type { HomeCounts } from '@/api/submitted';
import { WorkTiles } from '@/components/broking/WorkTiles';
import type { WorkTile } from '@/components/broking/WorkTiles';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { MASTERLIST_TABS, SUBMITTED_SECTION } from '../common/submittedCodes';

const WORK: { key: keyof HomeCounts; label: string; to: string; alert?: boolean }[] = [
  { key: 'iaafForApproval', label: 'IAAF for Approval', to: '/submitted/reviews?tab=FOR_APPROVAL' },
  { key: 'iaafApproved', label: 'IAAF to Send', to: '/submitted/reviews?tab=APPROVED' },
  { key: 'torForApproval', label: 'TOR for Approval', to: '/submitted/tors' },
  {
    key: 'renewalsPending',
    label: 'Hand-offs Pending',
    to: '/submitted/renewals?tab=PENDING',
    alert: true,
  },
  { key: 'lettersFailed', label: 'Letters Refused', to: '/submitted/letters', alert: true },
  { key: 'feesBilled', label: 'Fees to Tag', to: '/submitted/fees' },
  { key: 'feesTagged', label: 'Fees to Apply', to: '/submitted/fees?tab=TAGGED' },
];

function workTiles(counts: HomeCounts | undefined, open: (to: string) => void): WorkTile[] {
  return WORK.map((w) => ({
    key: w.key,
    label: w.label,
    value: counts?.[w.key] ?? 0,
    alert: w.alert,
    onClick: () => open(w.to),
  }));
}

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
    onClick: () => void navigate(`/submitted/masterlist?tab=${t.id}`),
  }));
  const work = workTiles(home.data, (to) => void navigate(to));
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

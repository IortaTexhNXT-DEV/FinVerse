import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { claimsApi } from '@/api/claims';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { ClaimDecisions } from './ClaimDecisions';
import { ClaimOverview } from './ClaimOverview';
import { claimActions } from './claimWorkflow';
import { LposTab } from './LposTab';
import { MovementsTab } from './MovementsTab';
import { RecoveriesTab } from './RecoveriesTab';
import { ReservesTab } from './ReservesTab';
import { SettlementsTab } from './SettlementsTab';

const TABS = [
  { id: 'overview', label: 'Overview' },
  { id: 'reserves', label: 'Reserves' },
  { id: 'settlements', label: 'Settlements' },
  { id: 'recoveries', label: 'Recoveries' },
  { id: 'lpos', label: 'LPOs' },
  { id: 'movements', label: 'Movement history' },
  { id: 'attachments', label: 'Attachments' },
] as const;

type TabId = (typeof TABS)[number]['id'];

/** Claim file: overview, reserves, settlements, recoveries, LPOs, movements and documents. */
export default function ClaimDetailPage() {
  const id = Number(useParams().id);
  const { user, can } = useAuth();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('overview');
  const claim = useQuery({ queryKey: ['claim', id], queryFn: () => claimsApi.claim(id) });
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['claim', id] });
    await queryClient.invalidateQueries({ queryKey: ['claim-docs', id] });
    await queryClient.invalidateQueries({ queryKey: ['claims'] });
  };

  if (claim.data === undefined) {
    return claim.error ? (
      <ErrorAlert error={claim.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const c = claim.data;
  const actions = claimActions(
    { status: c.status, createdBy: c.createdBy, ourPaid: c.totals.ourPaid },
    c.businessLine === 'MOTOR',
    user?.username,
    can,
  );
  const tabProps = { claim: c, actions, onChange: refresh };

  return (
    <div className="stack">
      <PageHeader
        section="Claims · Claim"
        title={c.claimNo}
        description={`${c.insuredName} · policy ${c.policyNo}`}
        actions={
          <>
            <StatusBadge status={c.status} />
            <ClaimDecisions claim={c} actions={actions} onChange={refresh} />
          </>
        }
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'overview' && <ClaimOverview claim={c} />}
      {tab === 'reserves' && <ReservesTab {...tabProps} />}
      {tab === 'settlements' && <SettlementsTab {...tabProps} />}
      {tab === 'recoveries' && <RecoveriesTab {...tabProps} />}
      {tab === 'lpos' && <LposTab {...tabProps} />}
      {tab === 'movements' && <MovementsTab claim={c} />}
      {tab === 'attachments' && <Attachments entityType="Claim" entityId={c.id} />}
    </div>
  );
}

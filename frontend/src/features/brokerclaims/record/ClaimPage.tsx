import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Attachments } from '@/components/attachments/Attachments';
import { SentMessages } from '@/components/broking/SentMessages';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount } from '@/utils/format';
import { CLAIMS_SECTION } from '../ClaimsPlaceholder';
import { PremiumPanel } from './CoverCard';
import { InsurersTab } from '../insurer/InsurersTab';
import { ReserveTab } from '../insurer/ReserveTab';
import { LocationsTab } from '../location/LocationsTab';
import type { Claim } from './api';
import { CLAIM_ENTITY, claimApi } from './api';
import { ClaimActions } from './ClaimActions';
import { ClaimSummary } from './ClaimSummary';
import { DetailsTab } from './DetailsTab';

type TabId = 'details' | 'locations' | 'insurers' | 'reserve' | 'documents';

const TABS: { id: TabId; label: string }[] = [
  { id: 'details', label: 'Details' },
  { id: 'locations', label: 'Locations' },
  { id: 'insurers', label: 'Insurers & Updates' },
  { id: 'reserve', label: 'Reserve & Settlement' },
  { id: 'documents', label: 'Documents' },
];

/** Premium and special remittance notes of a claim (BRCLM.001, OQ46). */
function PremiumNotes({ claim }: Readonly<{ claim: Claim }>) {
  const p = claim.premium;
  return (
    <>
      {claim.flags.unpaidPremium && <PremiumPanel premium={p.live} />}
      {claim.flags.awaitingPremiumRemittance && p.unremittedInvoices.length > 0 && (
        <div className="alert warning">
          The claim waits for the premium remittance. Request the special remittance of{' '}
          {p.unremittedInvoices.map((invoiceNo, i) => (
            <span key={invoiceNo}>
              {i > 0 && ', '}
              <Link
                to={`/remittance/special?invoiceNo=${encodeURIComponent(invoiceNo)}&condition=CLAIMS`}
              >
                {invoiceNo}
              </Link>
            </span>
          ))}
          .
        </div>
      )}
    </>
  );
}

function ClaimTabs({ claim, companyId }: Readonly<{ claim: Claim; companyId: number }>) {
  const [tab, setTab] = useState<TabId>('details');
  return (
    <Card>
      <div className="stack">
        <Tabs tabs={TABS} active={tab} onChange={setTab} />
        {tab === 'details' && <DetailsTab claim={claim} companyId={companyId} />}
        {tab === 'locations' && <LocationsTab claim={claim} companyId={companyId} />}
        {tab === 'insurers' && <InsurersTab claim={claim} companyId={companyId} />}
        {tab === 'reserve' && <ReserveTab claim={claim} companyId={companyId} />}
        {tab === 'documents' && (
          <div className="stack">
            <Attachments
              entityType={CLAIM_ENTITY}
              entityId={claim.id}
              reference={claim.claimNo}
              documentTypes
            />
            <SentMessages entityType={CLAIM_ENTITY} entityId={claim.id} />
          </div>
        )}
      </div>
    </Card>
  );
}

/**
 * Claim record (BRCLM.001-043; design 11): header with the claim number, the summary card with the
 * status pill and flags, the workflow panel, the actions (authorization code, loss advice, cover
 * refresh, latest version) and the tabs Details, Locations, Insurers & Updates, Reserve &
 * Settlement and Documents. Wave CL1-B adds the status actions and the Diary and History tabs.
 */
export default function ClaimPage() {
  const id = Number(useParams().id);
  const companyId = useCompanyId();
  const claim = useQuery({
    queryKey: ['broker-claims', 'claim', id],
    queryFn: () => claimApi.get(companyId, id),
  });
  const c = claim.data;
  return (
    <div className="stack">
      <PageHeader
        section={CLAIMS_SECTION}
        backTo="/claims-handling/worklist"
        title={c?.claimNo ?? 'Claim'}
        description={
          c
            ? `${c.cover.assuredName ?? ''} · ${c.cover.arn} · reserve ${c.cover.currency} ${formatAmount(c.totalReserve)}`
            : 'The claim case file'
        }
        actions={c ? <ClaimActions claim={c} companyId={companyId} /> : undefined}
      />
      <ErrorAlert error={claim.error} />
      {claim.isLoading && <span className="spinner" aria-label="Loading" />}
      {c && (
        <>
          <ClaimSummary claim={c} />
          <PremiumNotes claim={c} />
          <WorkflowPanel
            entityType={CLAIM_ENTITY}
            entityId={c.id}
            onChanged={() => void claim.refetch()}
          />
          <ClaimTabs claim={c} companyId={companyId} />
        </>
      )}
    </div>
  );
}

import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useTabParam } from '@/components/ui/useTabParam';
import { SUBMITTED_SECTION } from '../common/submittedCodes';
import { CreateProposals } from './CreateProposals';
import { NominatedRates } from './NominatedRates';
import { ProposalBatches } from './ProposalBatches';

const TABS = [
  { id: 'create', label: 'Create Proposals' },
  { id: 'batches', label: 'Proposal Batches' },
  { id: 'rates', label: 'Nominated Rates' },
] as const;

type TabId = (typeof TABS)[number]['id'];

/**
 * Renewal Proposals: proposals of the policies for renewal at the nominated package rates, with
 * the rate edited by vehicle classification or insurer and a reason, the batches released or
 * returned by the Team Lead, the preferred insurer, and the nominated rates.
 */
export default function ProposalsPage() {
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'create',
  );
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title="Renewal Proposals"
        description="Proposals of the policies for renewal at the nominated package rates, released by the Team Lead."
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'create' && <CreateProposals />}
      {tab === 'batches' && <ProposalBatches />}
      {tab === 'rates' && <NominatedRates />}
    </div>
  );
}

import type { ComponentType } from 'react';
import type { ProgrammeView } from '@/api/eb';
import { Card } from '@/components/ui/Card';
import { ComparativesTab } from '../confirmation/ComparativesTab';
import { ConfirmationTab } from '../confirmation/ConfirmationTab';
import { FranchiseTab } from '../market/FranchiseTab';
import { MarketTab } from '../market/MarketTab';
import { ProposalsTab } from '../market/ProposalsTab';
import { MembersTab } from '../members/MembersTab';
import { MemberChangesTab } from '../members/MemberChangesTab';
import { PendingItemsTable } from '../pending/PendingItemsTable';
import { BillingTab } from '../soa/BillingTab';
import { SubmissionsTab } from '../submission/SubmissionsTab';
import { BorTab } from './BorTab';
import { CycleTab } from './CycleTab';
import { DocumentsTab } from './DocumentsTab';
import { ContactsTab, LinesTab } from './LinesContactsTabs';
import { AccountsTab, HistoryTab } from './RecordsTabs';
import type { TabId } from './programmeTabList';

type TabProps = Readonly<{ programme: ProgrammeView }>;

function PendingTab({ programme }: TabProps) {
  return (
    <Card title="Pending Items">
      <PendingItemsTable filters={{ programmeId: programme.id }} showProgramme={false} />
    </Card>
  );
}

function Accounts({ programme }: TabProps) {
  return <AccountsTab programmeId={programme.id} />;
}

function History({ programme }: TabProps) {
  return <HistoryTab programmeId={programme.id} />;
}

const CONTENT: Record<TabId, ComponentType<TabProps>> = {
  cycle: CycleTab,
  lines: LinesTab,
  contacts: ContactsTab,
  documents: DocumentsTab,
  bor: BorTab,
  franchise: FranchiseTab,
  market: MarketTab,
  proposals: ProposalsTab,
  comparative: ComparativesTab,
  confirmation: ConfirmationTab,
  submissions: SubmissionsTab,
  accounts: Accounts,
  members: MembersTab,
  changes: MemberChangesTab,
  billing: BillingTab,
  pending: PendingTab,
  history: History,
};

/** The content of the active tab. */
export function TabContent({ tab, programme }: Readonly<{ tab: TabId; programme: ProgrammeView }>) {
  const Content = CONTENT[tab];
  return <Content programme={programme} />;
}

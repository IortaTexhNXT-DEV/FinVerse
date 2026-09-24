import { useQuery, useQueryClient } from '@tanstack/react-query';
import { FilePlus2 } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { clientsApi } from '@/api/clients';
import type { ClientDetail } from '@/api/clients';
import { useAuth } from '@/auth/authContext';
import { InstructionsBanner } from '@/components/broking/InstructionsBanner';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { ClientProposalsTab } from '@/features/proposals/ClientProposalsTab';
import { ClientQuotationsTab } from '@/features/quotations/ClientQuotationsTab';
import { humanize } from '@/utils/format';
import { ClientActionsBar } from './ClientActionsBar';
import { ClientDetailsTab } from './ClientDetailsTab';
import { ClientHistoryTab } from './ClientHistoryTab';
import { KycTab } from './KycTab';
import { LinkedRecordsTab } from './LinkedRecordsTab';
import { NotesTab } from './NotesTab';

type TabId = 'details' | 'kyc' | 'notes' | 'quotations' | 'proposals' | 'records' | 'history';

const TABS: readonly { id: TabId; label: string }[] = [
  { id: 'details', label: 'Details' },
  { id: 'kyc', label: 'KYC & Documents' },
  { id: 'notes', label: 'Tags & Instructions' },
  { id: 'quotations', label: 'Quotation' },
  { id: 'proposals', label: 'Confirmed Proposals' },
  { id: 'records', label: 'Linked Records' },
  { id: 'history', label: 'History' },
];

function Facts({ client: c }: Readonly<{ client: ClientDetail }>) {
  return (
    <div className="record-facts">
      <ReferenceChip label="Prospect" value={c.prospectCode} />
      {c.clientCode && <ReferenceChip label="Client" value={c.clientCode} />}
      <StatusBadge status={c.status} />
      <span className="muted">KYC</span>
      <StatusBadge status={c.kyc.status} />
      {!c.infoComplete && (
        <span className="badge warning" title={c.missingFields.join(', ')}>
          Information incomplete
        </span>
      )}
      {c.bankClient && <span className="badge">BDO bank client</span>}
    </div>
  );
}

function TabContent({ tab, client }: Readonly<{ tab: TabId; client: ClientDetail }>) {
  switch (tab) {
    case 'kyc':
      return <KycTab client={client} />;
    case 'notes':
      return <NotesTab client={client} />;
    case 'quotations':
      return <ClientQuotationsTab clientId={client.id} />;
    case 'proposals':
      return <ClientProposalsTab clientId={client.id} />;
    case 'records':
      return <LinkedRecordsTab clientId={client.id} />;
    case 'history':
      return <ClientHistoryTab clientId={client.id} />;
    default:
      return <ClientDetailsTab client={client} />;
  }
}

/**
 * Client 360 (BRNB.090/091/099/101): header with codes, status and KYC badges and the onboarding
 * actions and Generate Quotation; the onboarding workflow; tabs for details, KYC documents, tags
 * and instructions, quotations, confirmed proposals, linked records and history.
 */
export default function ClientDetailPage() {
  const id = Number(useParams().id);
  const queryClient = useQueryClient();
  const { can } = useAuth();
  const [tab, setTab] = useState<TabId>('details');
  const client = useQuery({ queryKey: ['crm', 'client', id], queryFn: () => clientsApi.get(id) });

  if (client.data === undefined) {
    return client.error ? (
      <ErrorAlert error={client.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const c = client.data;
  return (
    <div className="stack">
      <PageHeader
        section="Clients · Client"
        title={c.displayName}
        description={[`${humanize(c.clientType)} client`, c.marketSegment]
          .filter((part) => part !== undefined)
          .join(' · ')}
        actions={
          <>
            {can('QUOTE_MAINTAIN') && (
              <Link className="btn btn-secondary" to={`/quotations/new?client=${c.id}`}>
                <FilePlus2 size={16} aria-hidden="true" /> Generate Quotation
              </Link>
            )}
            <ClientActionsBar client={c} />
          </>
        }
      />
      <Facts client={c} />
      {!c.infoComplete && (
        <div className="alert warning" role="status">
          Client information incomplete: {c.missingFields.join(', ')}. Complete it before submitting
          the KYC.
        </div>
      )}
      <InstructionsBanner clientId={c.id} />
      <WorkflowPanel
        entityType="Client"
        entityId={c.id}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['crm'] })}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabContent tab={tab} client={c} />
    </div>
  );
}

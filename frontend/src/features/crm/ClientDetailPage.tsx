import { useQuery, useQueryClient } from '@tanstack/react-query';
import { FilePlus2 } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { clientsApi } from '@/api/clients';
import type { ClientDetail } from '@/api/clients';
import { useAuth } from '@/auth/authContext';
import { InstructionsBanner } from '@/components/broking/InstructionsBanner';
import { RecordHeader } from '@/components/broking/RecordHeader';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useTabParam } from '@/components/ui/useTabParam';
import { Tag } from '@/components/ui/Tag';
import { UserName } from '@/components/ui/UserName';
import { ClientProposalsTab } from '@/features/proposals/ClientProposalsTab';
import { ClientQuotationsTab } from '@/features/quotations/ClientQuotationsTab';
import { ClientScreeningTab } from '@/features/screening/cases/ClientScreeningTab';
import { formatDate, humanize } from '@/utils/format';
import { ClientActionsBar } from './ClientActionsBar';
import { ClientDetailsTab } from './ClientDetailsTab';
import { ClientHistoryTab } from './ClientHistoryTab';
import { KycTab } from './KycTab';
import { LinkedRecordsTab } from './LinkedRecordsTab';
import { NotesTab } from './NotesTab';
import { LovLabel } from '@/components/broking/LovLabel';

type TabId =
  'details' | 'kyc' | 'notes' | 'quotations' | 'proposals' | 'records' | 'screening' | 'history';

const TABS: readonly { id: TabId; label: string }[] = [
  { id: 'details', label: 'Details' },
  { id: 'kyc', label: 'KYC & Documents' },
  { id: 'notes', label: 'Tags & Instructions' },
  { id: 'quotations', label: 'Quotation' },
  { id: 'proposals', label: 'Confirmed Proposals' },
  { id: 'records', label: 'Linked Records' },
  { id: 'screening', label: 'Screening' },
  { id: 'history', label: 'History' },
];

/** Profile fields counted by the completeness indicator of the client header. */
function profileFields(c: ClientDetail): unknown[] {
  const common = [
    c.tin,
    c.idNumber,
    c.email ?? c.mobile,
    c.addressLine,
    c.marketSegment,
    c.profile.sourceOfFunds,
    c.profile.occupation,
  ];
  return c.clientType === 'CORPORATE' ? common : [...common, c.birthDate, c.profile.nationality];
}

function Header({ client: c }: Readonly<{ client: ClientDetail }>) {
  const fields = profileFields(c);
  const filled = fields.filter((v) => v !== undefined && v !== null && v !== '').length;
  return (
    <RecordHeader
      chips={
        <>
          <ReferenceChip label="Prospect" value={c.prospectCode} />
          {c.clientCode && <ReferenceChip label="Client" value={c.clientCode} />}
        </>
      }
      status={c.status}
      statuses={[{ label: 'KYC', status: c.kyc.status }]}
      flags={
        !c.infoComplete || c.bankClient ? (
          <>
            {!c.infoComplete && (
              <Tag title={`Missing: ${c.missingFields.join(', ')}`}>Information Incomplete</Tag>
            )}
            {c.bankClient && <Tag tone="info">BDO Bank Client</Tag>}
          </>
        ) : undefined
      }
      completeness={{ filled, total: fields.length }}
      facts={[
        { label: 'Client Type', value: humanize(c.clientType) },
        {
          label: 'Market Segment',
          value: c.marketSegment && <LovLabel type="MARKET_SEGMENT" code={c.marketSegment} />,
        },
        { label: 'Mobile', value: c.mobile },
        { label: 'E-mail', value: c.email },
        { label: 'TIN', value: c.tin },
        { label: 'Next KYC Review', value: formatDate(c.kyc.reviewDue) },
        { label: 'Created By', value: <UserName login={c.lifecycle.createdBy} /> },
        { label: 'Created', value: formatDate(c.lifecycle.createdAt) },
      ]}
    />
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
    case 'screening':
      return <ClientScreeningTab clientId={client.id} />;
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
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'details',
  );
  const client = useQuery({ queryKey: ['crm', 'client', id], queryFn: () => clientsApi.get(id) });

  if (client.data === undefined) {
    return client.error ? (
      <ErrorAlert error={client.error} onRetry={() => void client.refetch()} />
    ) : (
      <div className="card" aria-busy="true">
        <div className="card-body">
          <div className="skeleton-line wide" />
          <div className="skeleton-line" />
          <div className="skeleton-line" />
          <span className="visually-hidden" aria-label="Loading" />
        </div>
      </div>
    );
  }
  const c = client.data;
  return (
    <div className="stack">
      <PageHeader
        section="Clients · Client"
        title={c.displayName}
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
      <Header client={c} />
      {!c.infoComplete && c.kyc.status === 'NOT_STARTED' && (
        <div className="alert warning" role="status">
          Complete {c.missingFields.join(', ')} before submitting the KYC.
        </div>
      )}
      <InstructionsBanner clientId={c.id} />
      <WorkflowPanel
        entityType="Client"
        entityId={c.id}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['crm'] })}
      />
      <Tabs
        tabs={TABS.filter((t) => t.id !== 'screening' || can('SCR_VIEW'))}
        active={tab}
        onChange={setTab}
      />
      <TabContent tab={tab} client={c} />
    </div>
  );
}

import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Building2,
  CalendarRange,
  FileSignature,
  Layers,
  Pencil,
  ShieldCheck,
  User,
  Wallet,
} from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { PROPOSAL_ENTITY, proposalsApi } from '@/api/proposals';
import type { Proposal, ProposalStatus } from '@/api/proposals';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { InstructionsBanner } from '@/components/broking/InstructionsBanner';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { SentMessages } from '@/components/broking/SentMessages';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { RecordSummary } from '@/features/quotations/RecordSummary';
import type { Fact } from '@/features/quotations/RecordSummary';
import { formatAmount, formatDate } from '@/utils/format';
import { ProposalActions } from './ProposalActions';
import {
  ComparativeTab,
  DetailsTab,
  HistoryTab,
  ProposalSlipTab,
  QuotationSlipTab,
} from './ProposalTabs';
import { ResponsesTab } from './ResponsesTab';
import '@/styles/quotation.css';

const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'qs', label: 'Quotation Slip' },
  { id: 'responses', label: 'Insurer Responses' },
  { id: 'comparative', label: 'Comparative Table' },
  { id: 'ps', label: 'Proposal Slip' },
  { id: 'documents', label: 'Documents' },
  { id: 'emails', label: 'E-mails' },
  { id: 'history', label: 'History' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const MARKETING_EDIT: ProposalStatus[] = ['DRAFT'];
const TSU_EDIT: ProposalStatus[] = ['WITH_TSU', 'QS_PREPARATION'];

function facts(p: Proposal): Fact[] {
  return [
    { icon: User, label: 'Client', value: `${p.clientCode} – ${p.clientName}` },
    { icon: Layers, label: 'Product', value: `${p.productCode} (${p.lineCode})` },
    {
      icon: CalendarRange,
      label: 'Period',
      value: `${formatDate(p.periodFrom)} – ${formatDate(p.periodTo)}`,
    },
    {
      icon: Wallet,
      label: 'Total sum insured',
      value: `${p.currency} ${formatAmount(p.totalSumInsured)}`,
    },
    { icon: Building2, label: 'Insurers', value: p.insurers.join(', ') || 'To be selected' },
    {
      icon: FileSignature,
      label: 'Slips',
      value: [p.slips.qsNo, p.slips.psNo].filter(Boolean).join(' · ') || '—',
    },
  ];
}

function canEdit(p: Proposal, can: (permission: string) => boolean): boolean {
  return (
    (MARKETING_EDIT.includes(p.status) && can('PROPOSAL_REQUEST')) ||
    (TSU_EDIT.includes(p.status) && can('TSU_PROCESS'))
  );
}

function TabBody({ tab, proposal }: Readonly<{ tab: TabId; proposal: Proposal }>) {
  switch (tab) {
    case 'details':
      return <DetailsTab proposal={proposal} />;
    case 'qs':
      return <QuotationSlipTab key={proposal.status} proposal={proposal} />;
    case 'responses':
      return <ResponsesTab proposal={proposal} />;
    case 'comparative':
      return <ComparativeTab proposal={proposal} />;
    case 'ps':
      return <ProposalSlipTab proposal={proposal} />;
    case 'documents':
      return (
        <Attachments
          entityType={PROPOSAL_ENTITY}
          entityId={proposal.id}
          title="Documents"
          reference={proposal.arn}
        />
      );
    case 'emails':
      return (
        <Card title="E-mails" flush>
          <SentMessages entityType={PROPOSAL_ENTITY} entityId={proposal.id} />
        </Card>
      );
    default:
      return <HistoryTab proposalId={proposal.id} />;
  }
}

/**
 * One Proposal Request Form (BRNB.005-017): header with the PRF No., ARN chip and stage; the
 * client's special instructions; the workflow panel with the Marketing and TSU actions; and tabs
 * for details, quotation slip, insurer responses, comparative table, proposal slip, documents,
 * e-mails and history.
 */
export default function ProposalDetailPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('details');
  const proposal = useQuery({ queryKey: ['proposal', id], queryFn: () => proposalsApi.get(id) });
  if (proposal.data === undefined) {
    return proposal.error ? (
      <ErrorAlert error={proposal.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const p = proposal.data;
  return (
    <div className="stack">
      <PageHeader
        backTo="/proposals"
        section="Non-Package Management · Proposal Request"
        title={p.prfNo}
        description={`${p.productCode} proposal request for ${p.clientName}`}
        actions={
          canEdit(p, can) && (
            <Link className="btn btn-secondary" to={`/proposals/${p.id}/edit`}>
              <Pencil size={16} aria-hidden="true" /> Edit
            </Link>
          )
        }
      />
      <RecordSummary
        title={p.clientName}
        chips={
          <>
            <ReferenceChip label="ARN" value={p.arn} />
            <StatusBadge status={p.status} />
            {p.tsuReason && (
              <span className="badge warning" title={p.tsuReason}>
                <ShieldCheck size={12} aria-hidden="true" /> TSU
              </span>
            )}
          </>
        }
        facts={facts(p)}
      />
      <InstructionsBanner clientId={p.clientId} />
      <WorkflowPanel
        entityType={PROPOSAL_ENTITY}
        entityId={p.id}
        showHistory={false}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['proposal', id] })}
        renderBusinessActions={(actions) => <ProposalActions proposal={p} actions={actions} />}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabBody tab={tab} proposal={p} />
    </div>
  );
}

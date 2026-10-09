import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, CalendarClock, Download, Layers, User, Users, Wallet } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import type { CandidateDetail } from '@/api/renewal';
import { Attachments } from '@/components/attachments/Attachments';
import { RecordSummary } from '@/components/broking/RecordSummary';
import type { Fact } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useTabParam } from '@/components/ui/useTabParam';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate } from '@/utils/format';
import {
  BucketPill,
  FlagChips,
  RenewalInsurer,
  RenewalProduct,
  RenewalUnit,
} from '../common/RenewalBits';
import { RENEWAL_SECTION, dispositionLabel } from '../common/renewalCodes';
import { WORKFLOW_VIEW } from '../common/presentation';
import { RecordActions } from './RecordActions';
import { DetailsTab } from './DetailsTab';
import { HoldCoverTab } from './HoldCoverTab';
import { AccountHistoryTab, HistoryTab, NotesTab } from './HistoryTabs';
import { ChecksTab, ComputationsTab, InsurerTab, LettersTab } from './RecordTabs';
import '../renewal.css';
import { Notice } from '@/components/ui/Notice';

const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'checks', label: 'Checks' },
  { id: 'account', label: 'Account History' },
  { id: 'computations', label: 'Computations' },
  { id: 'insurer', label: 'Insurer' },
  { id: 'letters', label: 'Letters' },
  { id: 'hold-cover', label: 'Hold Cover' },
  { id: 'documents', label: 'Documents' },
  { id: 'notes', label: 'Remarks & Follow-ups' },
  { id: 'history', label: 'History' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const ENTITY = 'RenewalCandidate';

function facts(d: CandidateDetail): Fact[] {
  const { row } = d;
  return [
    { icon: User, label: 'Client', value: row.parties.clientName },
    { icon: Layers, label: 'Product', value: <RenewalProduct row={row} /> },
    { icon: Building2, label: 'Insurer', value: <RenewalInsurer row={row} /> },
    {
      icon: CalendarClock,
      label: 'Expiry',
      value: `${formatDate(row.expiry)} (${String(row.daysToExpiry)} days)`,
    },
    {
      icon: Wallet,
      label: 'Gross premium',
      value:
        row.money.grossPremium === null
          ? ''
          : `${row.money.currency ?? ''} ${formatAmount(row.money.grossPremium)}`,
    },
    { icon: Users, label: 'Unit', value: <RenewalUnit row={row} /> },
  ];
}

function Body({ tab, detail }: Readonly<{ tab: TabId; detail: CandidateDetail }>) {
  switch (tab) {
    case 'checks':
      return <ChecksTab detail={detail} />;
    case 'account':
      return <AccountHistoryTab detail={detail} />;
    case 'computations':
      return <ComputationsTab detail={detail} />;
    case 'insurer':
      return <InsurerTab detail={detail} />;
    case 'letters':
      return <LettersTab detail={detail} />;
    case 'hold-cover':
      return <HoldCoverTab detail={detail} />;
    case 'documents':
      return (
        <Attachments
          entityType={ENTITY}
          entityId={detail.lifecycle.id}
          title="Documents"
          reference={detail.row.renewalRef}
        />
      );
    case 'notes':
      return <NotesTab detail={detail} />;
    case 'history':
      return <HistoryTab detail={detail} />;
    default:
      return <DetailsTab detail={detail} />;
  }
}

/**
 * Renewal record (FR-RN-004, 041-048, 063, 064, 071, 084): the expiring account, the Classification and flags, the
 * workflow stage, the actions the user may take now, and the tabs of checks, account history,
 * computations, insurer responses, letters, documents, remarks and follow-ups and history.
 */
export default function CandidatePage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const ref = useParams().ref ?? '';
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'details',
  );
  const download = useFileDownload();
  const detail = useQuery({
    queryKey: ['renewal', 'candidate', companyId, ref],
    queryFn: () => renewalApi.get(companyId, ref),
    enabled: companyId > 0 && ref !== '',
  });
  if (detail.data === undefined) {
    return detail.error ? (
      <ErrorAlert error={detail.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const d = detail.data;
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['renewal'] });
  return (
    <div className="stack">
      <PageHeader
        backTo="/renewal/expiry"
        section={RENEWAL_SECTION}
        title={d.row.renewalRef}
        description={`Renewal of ${d.row.policy.policyNo ?? d.row.policy.expiringArn ?? ''} for ${d.row.parties.clientName}`}
        actions={
          can('RNW_EXPORT') && (
            <Button
              variant="secondary"
              icon={<Download size={16} />}
              busy={download.isPending}
              onClick={() => download.mutate(() => renewalApi.details(companyId, ref))}
            >
              Details PDF
            </Button>
          )
        }
      />
      <RecordSummary
        title={d.row.parties.clientName}
        chips={
          <>
            {d.row.policy.expiringArn ? (
              <ReferenceChip label="ARN" value={d.row.policy.expiringArn} />
            ) : null}
            <StatusBadge status={d.row.stage} label={d.row.stageLabel} />
            <BucketPill bucket={d.row.bucket} />
            {d.row.disposition !== null && (
              <span className="tag">{dispositionLabel(d.row.disposition)}</span>
            )}
          </>
        }
        flags={<FlagChips row={d.row} />}
        facts={facts(d)}
      />
      {d.blocking.length > 0 && (
        <Notice tone="warning" title="Blocking checks" items={d.blocking} />
      )}
      {WORKFLOW_VIEW.some((p) => can(p)) ? (
        <WorkflowPanel
          entityType={ENTITY}
          entityId={d.lifecycle.id}
          recordStatus={d.row.stage}
          showHistory={false}
          onChanged={refresh}
          renderBusinessActions={() => <RecordActions detail={d} />}
        />
      ) : (
        // Users who do not read the work queues (Contact Center, LAMD) still have the actions of
        // their role on the renewal.
        <div className="rnw-actions">
          <RecordActions detail={d} />
        </div>
      )}
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <Body tab={tab} detail={d} />
    </div>
  );
}

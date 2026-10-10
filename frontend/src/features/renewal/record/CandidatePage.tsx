import type { ReactNode } from 'react';
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
import { PlacementTab } from '../placement/PlacementTab';
import { HoldCoverTab } from './HoldCoverTab';
import { AccountHistoryTab, HistoryTab, NotesTab } from './HistoryTabs';
import { ChecksTab, ComputationsTab, InsurerTab, LettersTab } from './RecordTabs';
import { AuditLogTable } from '../audit/AuditLogTable';
import '../renewal.css';
import { Notice } from '@/components/ui/Notice';
import { LoadingPanel } from '@/components/ui/LoadingPanel';

const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'checks', label: 'Checks' },
  { id: 'account', label: 'Account History' },
  { id: 'computations', label: 'Computations' },
  { id: 'insurer', label: 'Insurer' },
  { id: 'letters', label: 'Letters' },
  { id: 'hold-cover', label: 'Hold Cover' },
  { id: 'placement', label: 'Placement' },
  { id: 'documents', label: 'Documents' },
  { id: 'notes', label: 'Remarks & Follow-ups' },
  { id: 'history', label: 'History' },
  { id: 'audit', label: 'Audit Logs' },
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

const BODIES: Partial<Record<TabId, (detail: CandidateDetail) => ReactNode>> = {
  checks: (d) => <ChecksTab detail={d} />,
  account: (d) => <AccountHistoryTab detail={d} />,
  computations: (d) => <ComputationsTab detail={d} />,
  insurer: (d) => <InsurerTab detail={d} />,
  letters: (d) => <LettersTab detail={d} />,
  'hold-cover': (d) => <HoldCoverTab detail={d} />,
  placement: (d) => <PlacementTab detail={d} />,
  documents: (d) => (
    <Attachments
      entityType={ENTITY}
      entityId={d.lifecycle.id}
      title="Documents"
      reference={d.row.renewalRef}
    />
  ),
  notes: (d) => <NotesTab detail={d} />,
  history: (d) => <HistoryTab detail={d} />,
  audit: (d) => <AuditLogTable filters={{ ref: d.row.renewalRef }} showReference={false} />,
};

function Body({ tab, detail }: Readonly<{ tab: TabId; detail: CandidateDetail }>) {
  const body = BODIES[tab];
  return body === undefined ? <DetailsTab detail={detail} /> : body(detail);
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
    return detail.error ? <ErrorAlert error={detail.error} /> : <LoadingPanel />;
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

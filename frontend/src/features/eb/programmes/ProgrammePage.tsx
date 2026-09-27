import { useQuery, useQueryClient } from '@tanstack/react-query';
import { FilePlus2, Pencil, Plus, Send } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { ebApi } from '@/api/eb';
import type { CycleStep, ProgrammeView } from '@/api/eb';
import type { WorkAction } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { RecordHeader } from '@/components/broking/RecordHeader';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { Tag } from '@/components/ui/Tag';
import { useTabParam } from '@/components/ui/useTabParam';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { EB_SECTION } from '../EbPlaceholder';
import { BenefitLines, EbLov } from '../common/EbLabels';
import { EB_LOV, ebLabel } from '../common/ebCodes';
import '../eb.css';
import { PendingItemsTable } from '../pending/PendingItemsTable';
import { BorTab } from './BorTab';
import { CycleTab } from './CycleTab';
import { DocumentsTab } from './DocumentsTab';
import { ContactsTab, LinesTab } from './LinesContactsTabs';
import { AccountsTab, HistoryTab } from './RecordsTabs';
import { ProgrammeActionDialogs } from './ProgrammeActionDialogs';
import type { ProgrammeDialog } from './ProgrammeActionDialogs';
import { nextExpiry, STEP_ACTIONS } from './programmeView';

type TabId =
  'cycle' | 'lines' | 'contacts' | 'documents' | 'bor' | 'accounts' | 'pending' | 'history';

const TABS: readonly { id: TabId; label: string }[] = [
  { id: 'cycle', label: 'Cycle' },
  { id: 'lines', label: 'Lines' },
  { id: 'contacts', label: 'Contacts' },
  { id: 'documents', label: 'Documents' },
  { id: 'bor', label: 'BOR' },
  { id: 'accounts', label: 'Accounts' },
  { id: 'pending', label: 'Pending Items' },
  { id: 'history', label: 'History' },
];

function Header({ p }: Readonly<{ p: ProgrammeView }>) {
  const current = p.cycles.find((c) => c.id === p.currentCycleId);
  const lines = p.lines
    .filter((l) => l.active)
    .map((l) => l.benefitLine)
    .join(',');
  return (
    <RecordHeader
      chips={
        <>
          <ReferenceChip label="Programme" value={p.programmeNo} />
          <ReferenceChip label="Client" value={p.client.code} />
          {current && <ReferenceChip label="Cycle" value={current.cycleNo} />}
        </>
      }
      status={p.status}
      statuses={current ? [{ label: 'Cycle', status: current.stage }] : []}
      flags={
        <>
          {p.renewalEligible && <Tag tone="info">Eligible for Renewal</Tag>}
          {current && <Tag tone="neutral">{ebLabel(current.businessType)}</Tag>}
        </>
      }
      facts={[
        { label: 'Client', value: p.client.name },
        { label: 'Team', value: <EbLov type={EB_LOV.team} code={p.teamCode} /> },
        { label: 'Funding', value: ebLabel(p.funding) },
        { label: 'Account Officer', value: <UserName login={p.accountOfficer} /> },
        { label: 'Benefit Lines', value: <BenefitLines codes={lines} /> },
        { label: 'Next Expiry', value: formatDate(nextExpiry(p)) },
        { label: 'Policy Year', value: current ? String(current.policyYear) : '' },
        { label: 'Created', value: formatDate(p.createdAt) },
      ]}
    />
  );
}

function TabContent({ tab, p }: Readonly<{ tab: TabId; p: ProgrammeView }>) {
  switch (tab) {
    case 'lines':
      return <LinesTab programme={p} />;
    case 'contacts':
      return <ContactsTab programme={p} />;
    case 'documents':
      return <DocumentsTab programme={p} />;
    case 'bor':
      return <BorTab programme={p} />;
    case 'accounts':
      return <AccountsTab programmeId={p.id} />;
    case 'pending':
      return (
        <Card title="Pending Items">
          <PendingItemsTable filters={{ programmeId: p.id }} showProgramme={false} />
        </Card>
      );
    case 'history':
      return <HistoryTab programmeId={p.id} />;
    default:
      return <CycleTab programme={p} />;
  }
}

/** The business actions of the current cycle offered in the workflow panel. */
function CycleActions({
  actions,
  onStep,
  onFeedback,
  onRa,
}: Readonly<{
  actions: WorkAction[];
  onStep: (step: CycleStep) => void;
  onFeedback: () => void;
  onRa: () => void;
}>) {
  const codes = new Set(actions.map((a) => a.action));
  return (
    <>
      {codes.has('send_ra') && (
        <Button variant="secondary" icon={<Send size={16} />} onClick={onRa}>
          Send RA
        </Button>
      )}
      {codes.has('record_feedback') && (
        <Button variant="secondary" onClick={onFeedback}>
          Record Feedback
        </Button>
      )}
      {STEP_ACTIONS.filter((s) => codes.has(s.action)).map((s) => (
        <Button key={s.step} variant="secondary" onClick={() => onStep(s.step)}>
          {s.label}
        </Button>
      ))}
    </>
  );
}

/**
 * Programme page (design 10.1; FR-EB-021 to 031, 046, 057): the record header, the workflow of
 * the current cycle with its requirement steps, and the tabs Cycle, Lines, Contacts, Documents,
 * BOR, Accounts, Pending Items and History.
 */
export default function ProgrammePage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'cycle',
  );
  const [dialog, setDialog] = useState<ProgrammeDialog>();
  const close = () => setDialog(undefined);
  const companyId = useCompanyId();
  const programme = useQuery({
    queryKey: ['eb', 'programme', id, companyId],
    queryFn: () => ebApi.programme(companyId, id),
    enabled: companyId > 0,
  });
  if (programme.data === undefined) {
    return programme.error ? (
      <ErrorAlert error={programme.error} onRetry={() => void programme.refetch()} />
    ) : (
      <div className="card" aria-busy="true">
        <div className="card-body">
          <div className="skeleton-line wide" />
          <div className="skeleton-line" />
          <span className="visually-hidden" aria-label="Loading" />
        </div>
      </div>
    );
  }
  const p = programme.data;
  const current = p.cycles.find((c) => c.id === p.currentCycleId);
  const market = can('EB_MARKET');
  return (
    <div className="stack">
      <PageHeader
        section={EB_SECTION}
        title={p.name}
        backTo="/eb/programmes"
        actions={
          market && (
            <>
              <Button
                variant="secondary"
                icon={<Pencil size={16} />}
                onClick={() => setDialog('profile')}
              >
                Edit Programme
              </Button>
              <Button
                variant="secondary"
                icon={<Plus size={16} />}
                onClick={() => setDialog('item')}
              >
                Add Pending Item
              </Button>
              <Button
                variant="accent"
                icon={<FilePlus2 size={16} />}
                onClick={() => setDialog('cycle')}
              >
                Open Cycle
              </Button>
            </>
          )
        }
      />
      <Header p={p} />
      {current && (
        <WorkflowPanel
          entityType="EbCycle"
          entityId={current.id}
          onChanged={() => void queryClient.invalidateQueries({ queryKey: ['eb'] })}
          renderBusinessActions={(actions) =>
            market && (
              <CycleActions
                actions={actions}
                onStep={(s) => setDialog({ step: s })}
                onFeedback={() => setDialog('feedback')}
                onRa={() => setDialog('ra')}
              />
            )
          }
        />
      )}
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabContent tab={tab} p={p} />
      {dialog !== undefined && (
        <ProgrammeActionDialogs programme={p} dialog={dialog} onClose={close} />
      )}
    </div>
  );
}

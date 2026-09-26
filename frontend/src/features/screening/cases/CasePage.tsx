import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AlarmClock,
  Briefcase,
  CalendarClock,
  FolderSearch,
  RotateCcw,
  ShieldAlert,
  Tag,
  UserCheck,
  UserCog,
  Users,
} from 'lucide-react';
import { useState } from 'react';
import type { ReactNode } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { StrTab } from '../str/StrTab';
import { casesApi } from './api';
import type { CaseDetail, CaseOutcome } from './api';
import { ReassignDialog, RiskTagDialog } from './AssignDialogs';
import {
  OutcomeDialog,
  ReopenDialog,
  ResubmitDialog,
  SubmitDialog,
  UnitHeadDialog,
  VoteDialog,
} from './CaseDialogs';
import type { CaseDialogProps } from './CaseDialogs';
import { stageLabel } from './caseLogic';
import { DecisionsTab, MatchesTab, TimelineTab } from './CaseTabs';
import { DocumentsTab } from './DocumentsTab';
import { ReviewTab } from './ReviewTab';

type TabId = 'matches' | 'review' | 'documents' | 'decisions' | 'str' | 'timeline';

const TABS: readonly { id: TabId; label: string }[] = [
  { id: 'matches', label: 'Matches' },
  { id: 'review', label: 'Review' },
  { id: 'documents', label: 'Documents' },
  { id: 'decisions', label: 'Decisions' },
  { id: 'str', label: 'STR' },
  { id: 'timeline', label: 'Timeline' },
];

type DialogId =
  'submit' | 'resubmit' | 'decide' | 'outcome' | 'vote' | 'reopen' | 'reassign' | 'risk';

const DIALOGS: Record<DialogId, (props: CaseDialogProps) => ReactNode> = {
  submit: (p) => <SubmitDialog {...p} />,
  resubmit: (p) => <ResubmitDialog {...p} />,
  decide: (p) => <UnitHeadDialog {...p} />,
  outcome: (p) => <OutcomeDialog {...p} />,
  vote: (p) => <VoteDialog {...p} />,
  reopen: (p) => <ReopenDialog {...p} />,
  reassign: (p) => <ReassignDialog {...p} />,
  risk: (p) => <RiskTagDialog {...p} />,
};

/** The business actions shown in the workflow panel, by the case action that allows them. */
const STAGE_ACTIONS: { action: CaseDetail['actions'][number]; dialog: DialogId; label: string }[] =
  [
    { action: 'SUBMIT', dialog: 'submit', label: 'Submit' },
    { action: 'RESUBMIT', dialog: 'resubmit', label: 'Resubmit' },
    { action: 'DECIDE', dialog: 'decide', label: 'Approve / Disapprove' },
    { action: 'OUTCOME', dialog: 'outcome', label: 'Record Outcome' },
    { action: 'VOTE', dialog: 'vote', label: 'Record Decision' },
  ];

function TabContent({ tab, detail }: Readonly<{ tab: TabId; detail: CaseDetail }>) {
  switch (tab) {
    case 'review':
      return <ReviewTab detail={detail} />;
    case 'documents':
      return <DocumentsTab detail={detail} />;
    case 'decisions':
      return <DecisionsTab detail={detail} />;
    case 'str':
      return <StrTab detail={detail} />;
    case 'timeline':
      return <TimelineTab caseId={detail.row.id} />;
    default:
      return <MatchesTab detail={detail} />;
  }
}

function Summary({ detail }: Readonly<{ detail: CaseDetail }>) {
  const c = detail.row;
  return (
    <RecordSummary
      title={c.clientName}
      chips={
        <>
          <ReferenceChip label="Case" value={c.caseNo} />
          <ReferenceChip label="Client" value={c.clientCode} />
          <StatusBadge status={c.stage} />
          {c.slaState !== 'NONE' && <StatusBadge status={c.slaState} />}
        </>
      }
      flags={
        <>
          <span className="tag">{humanize(c.caseType)}</span>
          {c.riskCategory && <span className="tag">{c.riskCategory}</span>}
          <span className="tag">{c.activePolicy ? 'Active Policy' : 'No Active Policy'}</span>
          {detail.strRequired && <span className="tag">STR Required</span>}
        </>
      }
      facts={[
        {
          icon: ShieldAlert,
          label: 'Opened By',
          value: `${humanize(detail.triggerCode)} ${detail.triggerReference ?? ''}`,
        },
        {
          icon: Briefcase,
          label: 'Marketing Unit / Unit Head',
          value: `${c.marketingUnit ?? '—'} / ${c.unitHead ?? '—'}`,
        },
        { icon: UserCheck, label: 'Assignee', value: c.assignee ?? 'Stage queue' },
        { icon: Users, label: 'Investigator', value: detail.investigator ?? '—' },
        { icon: CalendarClock, label: 'Created', value: formatDateTime(c.createdAt) },
        { icon: AlarmClock, label: 'Due', value: c.dueAt ? formatDateTime(c.dueAt) : '—' },
        { icon: FolderSearch, label: 'Review Template', value: humanize(detail.templateType) },
      ]}
    />
  );
}

/**
 * A screening case (SNSRP-401, 404, 501-502, 601, 701-706; FR-SS-040 to 064): the summary with
 * the SLA badge, the workflow panel with the actions of the current stage, and the tabs Matches,
 * Review, Documents, Decisions, STR and Timeline.
 */
export default function CasePage() {
  const id = Number(useParams().id);
  const [search, setSearch] = useSearchParams();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [dialog, setDialog] = useState<DialogId>();
  const tab = TABS.find((t) => t.id === search.get('tab'))?.id ?? 'matches';
  const detail = useQuery({ queryKey: ['screening', 'case', id], queryFn: () => casesApi.get(id) });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['screening'] });

  if (detail.data === undefined) {
    return detail.error ? (
      <ErrorAlert error={detail.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const d = detail.data;
  const onDone = (outcome?: CaseOutcome) => {
    setDialog(undefined);
    const next = outcome?.screeningCase.row.stage ?? d.row.stage;
    toast.success(`${d.row.caseNo}: ${stageLabel(next)}`);
    outcome?.warnings.forEach((w) => toast.error(`Warning: ${w}`));
    void refresh();
  };
  return (
    <div className="stack">
      <PageHeader
        section="Sanction Screening · Cases"
        title={d.row.caseNo}
        backTo="/screening/cases"
        description={`${humanize(d.row.caseType)} case of ${d.row.clientName} (${d.row.clientCode})`}
        actions={
          <>
            {d.actions.includes('RISK_TAG') && (
              <Button
                variant="secondary"
                icon={<Tag size={16} />}
                onClick={() => setDialog('risk')}
              >
                Update Risk Tag
              </Button>
            )}
            {d.actions.includes('REASSIGN') && (
              <Button
                variant="secondary"
                icon={<UserCog size={16} />}
                onClick={() => setDialog('reassign')}
              >
                Re-assign
              </Button>
            )}
            {d.actions.includes('REOPEN') && (
              <Button
                variant="secondary"
                icon={<RotateCcw size={16} />}
                onClick={() => setDialog('reopen')}
              >
                Re-open
              </Button>
            )}
          </>
        }
      />
      <Summary detail={d} />
      {d.breached && (
        <div className="alert warning" role="status">
          The SLA of this stage is breached; escalated to {d.escalatedTo ?? 'Compliance'}.
        </div>
      )}
      <WorkflowPanel
        entityType="ScreeningCase"
        entityId={d.row.id}
        showHistory={false}
        onChanged={() => void refresh()}
        renderBusinessActions={() =>
          STAGE_ACTIONS.filter((a) => d.actions.includes(a.action)).map((a) => (
            <Button key={a.action} size="sm" onClick={() => setDialog(a.dialog)}>
              {a.label}
            </Button>
          ))
        }
      />
      <Tabs tabs={TABS} active={tab} onChange={(t) => setSearch({ tab: t })} />
      <TabContent tab={tab} detail={d} />
      {dialog !== undefined &&
        DIALOGS[dialog]({ detail: d, onDone, onClose: () => setDialog(undefined) })}
    </div>
  );
}

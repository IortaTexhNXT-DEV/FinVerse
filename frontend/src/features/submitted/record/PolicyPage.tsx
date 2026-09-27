import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { submittedApi } from '@/api/submitted';
import type { PolicyDetail } from '@/api/submitted';
import type { WorkAction } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { InsurerName, LovLabel } from '@/components/broking/LovLabel';
import { RecordHeader } from '@/components/broking/RecordHeader';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PeriodCell } from '@/components/ui/PeriodCell';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useTabParam } from '@/components/ui/useTabParam';
import { UserName } from '@/components/ui/UserName';
import { formatAmount } from '@/utils/format';
import { FlagChips } from '../common/SubmittedBits';
import { SBM_LOV, SUBMITTED_SECTION } from '../common/submittedCodes';
import { DetailsTab, HistoryTab } from './DetailsTab';
import { RenewalTab, LettersTab } from './RenewalTab';
import { ReviewTab } from './ReviewTab';
import { RuleResultsTab } from './RuleResultsTab';
import { TorTab } from './TorTab';

const ENTITY = 'SubmittedPolicy';

const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'rules', label: 'Rule Results' },
  { id: 'review', label: 'Review & IAAF' },
  { id: 'tor', label: 'TOR' },
  { id: 'renewal', label: 'Renewal' },
  { id: 'letters', label: 'Letters' },
  { id: 'documents', label: 'Documents' },
  { id: 'history', label: 'History' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function TabBody({ tab, detail }: Readonly<{ tab: TabId; detail: PolicyDetail }>) {
  const id = detail.row.id;
  switch (tab) {
    case 'rules':
      return <RuleResultsTab policyId={id} />;
    case 'review':
      return <ReviewTab detail={detail} />;
    case 'tor':
      return <TorTab detail={detail} />;
    case 'renewal':
      return <RenewalTab detail={detail} />;
    case 'letters':
      return <LettersTab policyId={id} />;
    case 'documents':
      return (
        <Attachments
          entityType={ENTITY}
          entityId={id}
          title="Documents"
          reference={detail.row.sbmNo}
        />
      );
    case 'history':
      return <HistoryTab policyId={id} />;
    default:
      return <DetailsTab detail={detail} />;
  }
}

function BusinessActions({
  detail,
  actions,
  onDone,
}: Readonly<{ detail: PolicyDetail; actions: WorkAction[]; onDone: () => void }>) {
  const toast = useToast();
  const [open, setOpen] = useState<WorkAction | null>(null);
  const act = useMutation({
    mutationFn: ({
      action,
      reasonCode,
      comment,
    }: {
      action: string;
      reasonCode?: string;
      comment?: string;
    }) =>
      action === 'renew'
        ? submittedApi.renew(detail.row.id).then(() => undefined)
        : submittedApi.act(detail.row.id, action, reasonCode, comment).then(() => undefined),
    onSuccess: () => {
      toast.success(`${detail.row.sbmNo} updated`);
      setOpen(null);
      onDone();
    },
  });
  return (
    <>
      {actions.map((a) => (
        <Button
          key={a.action}
          variant={a.action === 'exclude' || a.action === 'close' ? 'secondary' : 'primary'}
          onClick={() => setOpen(a)}
        >
          {a.label}
        </Button>
      ))}
      {open !== null && (
        <ActionDialog
          title={open.label}
          record={`${detail.row.sbmNo} · ${detail.row.assuredName}`}
          effect={`The policy moves to ${open.toStageName ?? open.toStage}.`}
          reasonLov={open.reasonLov}
          confirmLabel={open.label}
          busy={act.isPending}
          error={act.error}
          onConfirm={(note) => act.mutate({ action: open.action, ...note })}
          onClose={() => setOpen(null)}
        />
      )}
    </>
  );
}

function TagActions({ detail, onDone }: Readonly<{ detail: PolicyDetail; onDone: () => void }>) {
  const toast = useToast();
  const [open, setOpen] = useState<'RENEWABLE' | 'NON_RENEWABLE' | null>(null);
  const tag = useMutation({
    mutationFn: ({ to, reason }: { to: string; reason?: string }) =>
      submittedApi.tag(detail.row.id, to, reason ?? null),
    onSuccess: () => {
      toast.success(`${detail.row.sbmNo} tagged`);
      setOpen(null);
      onDone();
    },
  });
  return (
    <>
      {detail.row.renewalTag !== 'RENEWABLE' && (
        <Button variant="secondary" onClick={() => setOpen('RENEWABLE')}>
          Tag Renewable
        </Button>
      )}
      {detail.row.renewalTag !== 'NON_RENEWABLE' && (
        <Button variant="secondary" onClick={() => setOpen('NON_RENEWABLE')}>
          Tag Non-Renewable
        </Button>
      )}
      {open !== null && (
        <ActionDialog
          title={open === 'RENEWABLE' ? 'Tag Renewable' : 'Tag Non-Renewable'}
          record={`${detail.row.sbmNo} · ${detail.row.assuredName}`}
          effect="A manual tag overrides the rules of the next processing runs."
          reasonLov={open === 'NON_RENEWABLE' ? SBM_LOV.nonRenewal : undefined}
          confirmLabel="Tag"
          busy={tag.isPending}
          error={tag.error}
          onConfirm={(note) => tag.mutate({ to: open, reason: note.reasonCode })}
          onClose={() => setOpen(null)}
        />
      )}
    </>
  );
}

/**
 * Submitted policy record (FR-SP-010 to 066): the summary with flags and key facts, the work case
 * with the handler's actions, and the tabs of details, rule results, review and IAAF, TOR,
 * renewal, letters, documents and history.
 */
export default function PolicyPage() {
  const { id } = useParams<{ id: string }>();
  const policyId = Number(id);
  const queryClient = useQueryClient();
  const { can } = useAuth();
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'details',
  );
  const policy = useQuery({
    queryKey: ['submitted', 'policy', policyId],
    queryFn: () => submittedApi.get(policyId),
    enabled: Number.isFinite(policyId),
  });
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['submitted'] });
  const d = policy.data;
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title={d ? d.row.assuredName : 'Submitted Policy'}
        description={d ? `Masterlist record ${d.row.sbmNo}` : undefined}
        actions={d && can('SBM_MAINTAIN') && <TagActions detail={d} onDone={refresh} />}
      />
      <ErrorAlert error={policy.error} onRetry={() => void policy.refetch()} />
      {d && (
        <>
          <RecordHeader
            chips={
              <>
                <ReferenceChip value={d.row.sbmNo} label="Masterlist No." />
                {d.row.pnNo && <ReferenceChip value={d.row.pnNo} label="PN No." />}
                {d.row.policyNo && <ReferenceChip value={d.row.policyNo} label="Policy No." />}
              </>
            }
            status={d.row.status}
            flags={<FlagChips flags={d.row.flags} />}
            facts={[
              { label: 'Segment', value: <LovLabel type={SBM_LOV.segment} code={d.row.segment} /> },
              { label: 'Insurer', value: <InsurerName code={d.row.insurerCode} /> },
              {
                label: 'Period',
                value: <PeriodCell from={d.row.inceptionDate} to={d.row.expiryDate} />,
              },
              {
                label: 'Sum Insured',
                value:
                  d.row.sumInsured === null
                    ? null
                    : `${d.row.currency} ${formatAmount(d.row.sumInsured)}`,
              },
              { label: 'Bucket', value: <LovLabel type={SBM_LOV.bucket} code={d.row.bucket} /> },
              {
                label: 'Handler',
                value: d.row.handlerUsername ? <UserName login={d.row.handlerUsername} /> : null,
              },
              {
                label: 'Account Officer',
                value: d.tracking.aoUsername ? <UserName login={d.tracking.aoUsername} /> : null,
              },
              {
                label: 'Conversion',
                value: <LovLabel type={SBM_LOV.conversion} code={d.row.conversionStatus} />,
              },
            ]}
          />
          <WorkflowPanel
            entityType={ENTITY}
            entityId={d.row.id}
            showHistory={false}
            onChanged={refresh}
            renderBusinessActions={(actions) => (
              <BusinessActions detail={d} actions={actions} onDone={refresh} />
            )}
          />
          <Tabs tabs={TABS} active={tab} onChange={setTab} />
          <TabBody tab={tab} detail={d} />
        </>
      )}
    </div>
  );
}

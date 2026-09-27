import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Clock, FileText, Gavel, UserRound, Wallet } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import type { WorkAction } from '@/api/workflow';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { workflowKey } from '@/components/broking/workflowKey';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDateTime, humanize } from '@/utils/format';
import { ReasonDialog, RecordLoading } from '../plans/Parts';
import type { Escalation, EscalationItem } from './api';
import { escalationsApi } from './api';
import { LEVEL_LABELS } from './labels';

const ENTITY = 'CollectionEscalation';
const BUSINESS_ACTIONS = new Set(['escalate_further', 'resolve', 'resubmit']);

const ITEM_COLUMNS: Column<EscalationItem>[] = [
  { key: 'inv', header: 'Invoice No.', render: (i) => <strong>{i.invoiceNo}</strong> },
  { key: 'policy', header: 'Policy No.', render: (i) => i.policyNo ?? '' },
  { key: 'aging', header: 'Days Since Booking', numeric: true, render: (i) => i.agingDays },
  {
    key: 'balance',
    header: 'Outstanding When Escalated',
    numeric: true,
    render: (i) => <Amount value={i.balance} />,
  },
];

function Summary({ e }: Readonly<{ e: Escalation }>) {
  const origin = e.kind === 'AUTO' ? 'Rule ' + (e.ruleCode ?? '') : e.raisedBy;
  return (
    <RecordSummary
      title={e.assuredName}
      chips={
        <>
          <ReferenceChip label="Escalation" value={e.escalationNo} />
          <ReferenceChip label="ARN" value={e.arn} />
          <StatusBadge status={e.status} />
        </>
      }
      flags={
        <>
          <span className="tag">{e.kind === 'AUTO' ? 'By Rule' : 'Manual'}</span>
          {e.overdue && <span className="tag">Past SLA</span>}
        </>
      }
      facts={[
        { icon: Gavel, label: 'Reason', value: humanize(e.reasonCode) },
        {
          icon: UserRound,
          label: 'Escalated To',
          value: e.targetUsername ?? LEVEL_LABELS[e.targetLevel],
        },
        {
          icon: Wallet,
          label: `Outstanding (${e.currency})`,
          value: formatAmount(e.totalBalance),
        },
        {
          icon: Clock,
          label: 'In Stage Since',
          value: `${formatDateTime(e.stageSince)} (SLA ${e.slaHours} h)`,
        },
        {
          icon: FileText,
          label: 'Raised By',
          value: `${origin}, ${formatDateTime(e.createdAt)}`,
        },
      ]}
    />
  );
}

/**
 * Escalation record (BRCLXN.049/050): the account and its escalated invoices, the workflow
 * CLX_ESCALATION with the user's actions (acknowledge, escalate further, return to the handler,
 * resubmit, resolve) and its history.
 */
export default function EscalationDetailPage() {
  const id = Number(useParams().id);
  const toast = useToast();
  const queryClient = useQueryClient();
  const [asking, setAsking] = useState<WorkAction>();
  const escalation = useQuery({
    queryKey: ['collections', 'escalation', id],
    queryFn: () => escalationsApi.escalation(id),
  });
  const act = useMutation({
    mutationFn: (v: { action: string; reasonCode?: string; comment?: string }) =>
      escalationsApi.act(id, v.action, { reasonCode: v.reasonCode, comment: v.comment }),
    onSuccess: async (e) => {
      setAsking(undefined);
      queryClient.setQueryData(['collections', 'escalation', id], e);
      await queryClient.invalidateQueries({ queryKey: workflowKey(ENTITY, id) });
      await queryClient.invalidateQueries({ queryKey: ['collections', 'escalations'] });
      toast.success(`${e.escalationNo}: ${humanize(e.status)}`);
    },
  });
  if (escalation.data === undefined) {
    return <RecordLoading error={escalation.error} />;
  }
  const e = escalation.data;
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections · Escalations"
        backTo="/collections/escalations"
        title={e.escalationNo}
        description={e.remarks ?? `Escalation of ${e.arn}`}
      />
      <Summary e={e} />
      <WorkflowPanel
        entityType={ENTITY}
        entityId={id}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['collections'] })}
        renderBusinessActions={(actions) =>
          actions
            .filter((a) => BUSINESS_ACTIONS.has(a.action))
            .map((a) => (
              <Button key={a.action} size="sm" variant="secondary" onClick={() => setAsking(a)}>
                {a.label}
              </Button>
            ))
        }
      />
      {e.resolution !== undefined && <div className="alert success">Resolved: {e.resolution}</div>}
      <Card title={`Escalated Invoices (${e.items.length})`}>
        <DataTable
          caption="Escalated invoices"
          columns={ITEM_COLUMNS}
          rows={e.items}
          rowKey={(i) => i.invoiceNo}
        />
      </Card>
      {asking?.action === 'resolve' && (
        <ReasonDialog
          title="Resolve Escalation"
          label="Resolution"
          confirmLabel="Resolve"
          busy={act.isPending}
          error={act.error}
          onClose={() => setAsking(undefined)}
          onConfirm={(comment) => act.mutate({ action: 'resolve', comment })}
        />
      )}
      {asking !== undefined && asking.action !== 'resolve' && (
        <ActionDialog
          title={asking.label}
          reasonLov={asking.reasonLov}
          confirmLabel={asking.label}
          busy={act.isPending}
          error={act.error}
          onClose={() => setAsking(undefined)}
          onConfirm={(note) => act.mutate({ action: asking.action, ...note })}
        />
      )}
    </div>
  );
}

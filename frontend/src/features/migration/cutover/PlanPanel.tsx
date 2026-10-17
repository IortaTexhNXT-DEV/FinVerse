import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, Gauge } from 'lucide-react';
import { useState } from 'react';
import { cutoverApi } from '@/api/migrationCutover';
import type {
  CutoverTask,
  GonogoCriterion,
  GonogoDecision,
  PlanDetail,
} from '@/api/migrationCutover';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';
import { ActionConfirm } from '../common/ActionConfirm';
import type { MigAction } from '../common/ActionConfirm';
import { MigStatus } from '../common/MigStatus';
import { migLabel } from '../common/migrationCodes';
import { useDownload } from '../common/useDownload';
import { criterionActions, decisionActions, taskActions } from './cutoverActions';
import { UserName } from '@/components/ui/UserName';

const DECISIONS: Column<GonogoDecision>[] = [
  {
    key: 'decision',
    header: 'Decision',
    kind: 'status',
    render: (d) => (
      <StatusBadge
        status={d.decision === 'GO' ? 'APPROVED' : 'REJECTED'}
        label={d.decision === 'GO' ? 'GO' : 'NO-GO'}
      />
    ),
  },
  {
    key: 'met',
    header: 'Criteria met',
    render: (d) => `${String(d.criteriaMet)} of ${String(d.criteriaTotal)}`,
  },
  { key: 'by', header: 'Decided by', render: (d) => <UserName login={d.decidedBy} /> },
  { key: 'at', header: 'At', kind: 'datetime', render: (d) => formatDateTime(d.decidedAt) },
  { key: 'comment', header: 'Comment', render: (d) => d.comment ?? '' },
];

function actionButtons(actions: MigAction[], onPick: (a: MigAction) => void) {
  return (
    <span className="mig-actions">
      {actions.map((a) => (
        <Button key={a.title} variant="secondary" size="sm" onClick={() => onPick(a)}>
          {a.confirmLabel}
        </Button>
      ))}
    </span>
  );
}

function times(start: string | undefined, end: string | undefined): string {
  if (start === undefined) {
    return '';
  }
  const to = end === undefined ? '' : ' to ' + formatDateTime(end);
  return formatDateTime(start) + to;
}

function tasksOf(
  planNo: string,
  manage: boolean,
  pick: (a: MigAction) => void,
): Column<CutoverTask>[] {
  return [
    { key: 'seq', header: 'No.', kind: 'center', render: (t) => t.seq },
    { key: 'task', header: 'Task', render: (t) => <CellStack main={t.task} sub={t.phase} /> },
    {
      key: 'owner',
      header: 'Owner',
      render: (t) => <CellStack main={migLabel(t.ownerRole)} sub={t.objectCode ?? ''} />,
    },
    { key: 'after', header: 'After', render: (t) => t.dependsOn ?? '' },
    { key: 'planned', header: 'Planned', render: (t) => times(t.plannedStart, t.plannedEnd) },
    { key: 'actual', header: 'Actual', render: (t) => times(t.actualStart, t.actualEnd) },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (t) => <CellStack main={<MigStatus status={t.status} />} sub={t.remarks ?? ''} />,
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (t) => (manage ? actionButtons(taskActions(planNo, t), pick) : null),
    },
  ];
}

function criteriaOf(
  planNo: string,
  manage: boolean,
  pick: (a: MigAction) => void,
): Column<GonogoCriterion>[] {
  return [
    { key: 'no', header: 'No.', kind: 'center', render: (c) => c.criterionNo },
    {
      key: 'name',
      header: 'Criterion',
      render: (c) => <CellStack main={c.name} sub={c.threshold} />,
    },
    { key: 'kind', header: 'Kind', render: (c) => (c.manual ? 'Recorded' : 'Measured') },
    {
      key: 'value',
      header: 'Measured',
      render: (c) => <CellStack main={c.measuredValue ?? ''} sub={c.note ?? ''} />,
    },
    {
      key: 'met',
      header: 'Result',
      kind: 'status',
      render: (c) =>
        c.met === undefined ? (
          <StatusBadge status="PENDING" label="Open" />
        ) : (
          <StatusBadge status={c.met ? 'APPROVED' : 'REJECTED'} label={c.met ? 'Met' : 'Not met'} />
        ),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (c) => (manage ? actionButtons(criterionActions(planNo, c), pick) : null),
    },
  ];
}

interface ActionsProps {
  planNo: string;
  detail: PlanDetail | undefined;
  pick: (a: MigAction) => void;
  onClose: () => void;
}

function PlanActions({ planNo, detail, pick, onClose }: Readonly<ActionsProps>) {
  const { can } = useAuth();
  const client = useQueryClient();
  const download = useDownload();
  const measure = useMutation({
    mutationFn: () => cutoverApi.measure(planNo),
    onSuccess: () => client.invalidateQueries({ queryKey: ['migration', 'cutover'] }),
  });
  const decide = can('MIG_GONOGO_DECIDE');
  const unmet = (detail?.criteria ?? []).filter((c) => c.met !== true).length;
  const production = detail?.plan.kind === 'PRODUCTION';
  return (
    <span className="mig-actions">
      <Button
        variant="secondary"
        size="sm"
        icon={<Download size={14} />}
        busy={download.busy}
        onClick={() => download.run(() => cutoverApi.runbook(planNo))}
      >
        Runbook
      </Button>
      {(can('MIG_CUTOVER_MANAGE') || decide) && (
        <Button
          variant="secondary"
          size="sm"
          icon={<Gauge size={14} />}
          busy={measure.isPending}
          onClick={() => measure.mutate()}
        >
          Measure Criteria
        </Button>
      )}
      {decide &&
        detail !== undefined &&
        actionButtons(decisionActions(planNo, production, unmet), pick)}
      <Button variant="ghost" size="sm" onClick={onClose}>
        Close
      </Button>
      <ErrorAlert error={measure.error ?? download.error} />
    </span>
  );
}

/** A cutover plan: runbook tasks, go / no-go criteria and decisions. */
export function PlanPanel({ planNo, onClose }: Readonly<{ planNo: string; onClose: () => void }>) {
  const { can } = useAuth();
  const [action, setAction] = useState<MigAction>();
  const detail = useQuery({
    queryKey: ['migration', 'cutover', 'plan', planNo],
    queryFn: () => cutoverApi.plan(planNo),
  });
  const manage = can('MIG_CUTOVER_MANAGE');
  const d: PlanDetail | undefined = detail.data;
  return (
    <Card
      title={d === undefined ? planNo : `${d.plan.name} (${planNo})`}
      actions={<PlanActions planNo={planNo} detail={d} pick={setAction} onClose={onClose} />}
    >
      <ErrorAlert error={detail.error} />
      <h3>Runbook</h3>
      <DataTable<CutoverTask>
        loading={detail.isLoading}
        rows={d?.tasks ?? []}
        rowKey={(t) => String(t.seq)}
        columns={tasksOf(planNo, manage, setAction)}
      />
      <h3>Go / No-Go Criteria</h3>
      <DataTable<GonogoCriterion>
        loading={detail.isLoading}
        rows={d?.criteria ?? []}
        rowKey={(c) => String(c.criterionNo)}
        columns={criteriaOf(planNo, manage, setAction)}
      />
      <h3>Decisions</h3>
      <DataTable<GonogoDecision>
        rows={d?.decisions ?? []}
        rowKey={(x) => x.decidedAt}
        emptyMessage="No decision yet"
        columns={DECISIONS}
      />
      <ActionConfirm action={action} onClose={() => setAction(undefined)} />
    </Card>
  );
}

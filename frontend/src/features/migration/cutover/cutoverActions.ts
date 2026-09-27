import { cutoverApi } from '@/api/migrationCutover';
import type {
  CutoverTask,
  DecommissionItem,
  DecommissionStatus,
  GonogoCriterion,
} from '@/api/migrationCutover';
import type { MigAction } from '../common/ActionConfirm';

/** The progress actions of a runbook task in its current status. */
export function taskActions(planNo: string, t: CutoverTask): MigAction[] {
  const record = `${String(t.seq)}. ${t.task}`;
  const actions: MigAction[] = [];
  if (t.status === 'NOT_STARTED' || t.status === 'BLOCKED') {
    actions.push({
      title: 'Start Task',
      record,
      effect: 'The actual start is recorded; the tasks it depends on must be finished.',
      confirmLabel: 'Start',
      reason: 'optional',
      done: 'Task started',
      run: (note) => cutoverApi.progress(planNo, t.seq, 'IN_PROGRESS', note),
    });
  }
  if (t.status === 'IN_PROGRESS') {
    actions.push(
      {
        title: 'Complete Task',
        record,
        effect: 'The actual end is recorded and the tasks after it may start.',
        confirmLabel: 'Done',
        reason: 'optional',
        done: 'Task completed',
        run: (note) => cutoverApi.progress(planNo, t.seq, 'DONE', note),
      },
      {
        title: 'Block Task',
        record,
        effect: 'The task is marked blocked with its reason for the cut-over call.',
        confirmLabel: 'Block',
        reason: 'required',
        destructive: true,
        done: 'Task blocked',
        run: (note) => cutoverApi.progress(planNo, t.seq, 'BLOCKED', note),
      },
    );
  }
  return actions;
}

/** Recording a criterion the system does not measure, with its evidence. */
export function criterionActions(planNo: string, c: GonogoCriterion): MigAction[] {
  if (!c.manual) {
    return [];
  }
  const record = `${String(c.criterionNo)}. ${c.name}`;
  return [
    {
      title: 'Criterion Met',
      record,
      effect: `Threshold: ${c.threshold}. Give the evidence.`,
      confirmLabel: 'Met',
      reason: 'required',
      done: 'Criterion recorded',
      run: (note) => cutoverApi.record(planNo, c.criterionNo, true, note),
    },
    {
      title: 'Criterion Not Met',
      record,
      effect: `Threshold: ${c.threshold}. Say what is missing.`,
      confirmLabel: 'Not met',
      reason: 'required',
      destructive: true,
      done: 'Criterion recorded',
      run: (note) => cutoverApi.record(planNo, c.criterionNo, false, note),
    },
  ];
}

/** The decision of the go / no-go board. */
export function decisionActions(planNo: string, production: boolean, unmet: number): MigAction[] {
  const gate = production ? ' This signs the go-live gate.' : '';
  return [
    {
      title: 'Decide GO',
      record: planNo,
      effect:
        (unmet > 0
          ? `${String(unmet)} criteria are not met; give the justification of the GO.`
          : 'Every criterion is met.') + gate,
      confirmLabel: 'GO',
      reason: unmet > 0 ? 'required' : 'optional',
      done: 'GO recorded',
      run: (comment) => cutoverApi.decide(planNo, true, comment),
    },
    {
      title: 'Decide NO-GO',
      record: planNo,
      effect: `The cut-over stops and rolls back to the snapshot.${gate}`,
      confirmLabel: 'NO-GO',
      reason: 'required',
      destructive: true,
      done: 'NO-GO recorded',
      run: (comment) => cutoverApi.decide(planNo, false, comment),
    },
  ];
}

/** Recording a decommissioning criterion with its evidence. */
export function itemActions(item: DecommissionItem): MigAction[] {
  const to = (status: DecommissionStatus, label: string, destructive = false): MigAction => ({
    title: label,
    record: `${item.systemCode}: ${item.name}`,
    effect: item.description,
    confirmLabel: label,
    reason: 'required',
    destructive,
    done: 'Checklist updated',
    run: (evidence) => cutoverApi.update(item.id, status, evidence),
  });
  if (item.status === 'SIGNED') {
    return [];
  }
  const actions = [to('SIGNED', 'Sign')];
  if (item.status === 'OPEN') {
    actions.unshift(to('MET', 'Met'));
    actions.push(to('NOT_APPLICABLE', 'Not applicable', true));
  }
  return actions;
}

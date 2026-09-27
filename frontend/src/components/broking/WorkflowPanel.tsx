import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlarmClock, UserCheck } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { workflowApi } from '@/api/workflow';
import type { ActionNote, WorkAction, WorkItem } from '@/api/workflow';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, titleCase } from '@/utils/format';
import { ActionDialog } from './ActionDialog';
import { HistoryTable } from './HistoryTable';
import { StageStepper } from './StageStepper';
import { stageSteps } from './stageSteps';
import { workflowKey } from './workflowKey';

interface WorkflowPanelProps {
  entityType: string;
  entityId: string | number;
  /**
   * Buttons for business actions (submit, approve, place, book...) rendered by the owning screen.
   * Receives the non-generic actions the user may take now.
   */
  renderBusinessActions?: (actions: WorkAction[]) => ReactNode;
  /** Called after a generic action (return, void...) so the page can refresh its record. */
  onChanged?: () => void;
  /** Show the status history below the stage. */
  showHistory?: boolean;
  /**
   * The status shown in the record header. When it changes (a business action run by the page,
   * a save, a refresh) the stepper is reloaded, so the header and the stepper never disagree.
   */
  recordStatus?: string;
}

/** Current stage, since, due date with the overdue indicator and assignee, on one row. */
function StageMeta({ item, terminal }: Readonly<{ item: WorkItem; terminal: boolean }>) {
  return (
    <dl className="workflow-meta-row">
      <div>
        <dt>Current Stage</dt>
        <dd className="workflow-stage-name">{titleCase(item.stageName)}</dd>
      </div>
      <div>
        <dt>Since</dt>
        <dd className="nowrap">{formatDateTime(item.stageEnteredAt)}</dd>
      </div>
      {!terminal && (
        <div>
          <dt>Due</dt>
          <dd className={item.overdue ? 'nowrap workflow-overdue' : 'nowrap'}>
            {item.dueAt ? (
              <>
                <AlarmClock size={14} aria-hidden="true" /> {formatDateTime(item.dueAt)}
                {item.overdue && <StatusBadge status="OVERDUE" />}
              </>
            ) : (
              <span className="muted">—</span>
            )}
          </dd>
        </div>
      )}
      <div>
        <dt>Assigned To</dt>
        <dd>
          {item.assignee ? (
            <>
              <UserCheck size={14} aria-hidden="true" /> <UserName login={item.assignee} />
            </>
          ) : (
            <span className="muted">—</span>
          )}
        </dd>
      </div>
    </dl>
  );
}

/**
 * Keeps the record header and the stepper on the same state: a change of the header status
 * reloads the stepper, and a stage change seen by the stepper (after an action taken anywhere)
 * refreshes the record through `onChanged`. Returns the last stage seen, which the panel's own
 * actions update (they call `onChanged` themselves).
 */
function useHeaderSync(
  key: ReturnType<typeof workflowKey>,
  stage: string | undefined,
  recordStatus: string | undefined,
  onChanged: (() => void) | undefined,
) {
  const queryClient = useQueryClient();
  const lastStatus = useRef(recordStatus);
  const lastStage = useRef(stage);
  useEffect(() => {
    if (recordStatus !== lastStatus.current) {
      lastStatus.current = recordStatus;
      void queryClient.invalidateQueries({ queryKey: key });
    }
  }, [recordStatus, queryClient, key]);
  useEffect(() => {
    const before = lastStage.current;
    lastStage.current = stage;
    if (before !== undefined && stage !== undefined && before !== stage) {
      onChanged?.();
    }
  }, [stage, onChanged]);
  return lastStage;
}

/**
 * The workflow header of a record (BRNB.022/115): a horizontal stepper built from the workflow's
 * defined stages (passed stages checked, the current one highlighted, returned and closed paths
 * marked), then one meta row with the current stage, since, due (overdue flagged) and assignee,
 * and the actions the current user may take on the right; the status history table below.
 * Generic actions (return, void, decline) run here with their reason; business actions are
 * supplied by the page.
 */
export function WorkflowPanel({
  entityType,
  entityId,
  renderBusinessActions,
  onChanged,
  showHistory = true,
  recordStatus,
}: Readonly<WorkflowPanelProps>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<WorkAction | null>(null);
  const key = workflowKey(entityType, entityId);
  const detail = useQuery({
    queryKey: key,
    queryFn: () => workflowApi.byRecord(entityType, entityId),
  });
  const seenStageRef = useHeaderSync(key, detail.data?.item.stageCode, recordStatus, onChanged);
  const act = useMutation({
    mutationFn: ({ action, note }: { action: WorkAction; note: ActionNote }) =>
      workflowApi.act(detail.data?.item.id ?? 0, action.action, note),
    onSuccess: async (result) => {
      setPending(null);
      seenStageRef.current = result.item.stageCode;
      queryClient.setQueryData(key, result);
      await queryClient.invalidateQueries({ queryKey: ['workflow', 'queue'] });
      toast.success(`Moved to ${result.item.stageName}`);
      onChanged?.();
    },
  });

  if (detail.isLoading) {
    return (
      <div className="workflow-panel" aria-busy="true">
        <div className="skeleton-line wide" />
        <div className="skeleton-line" />
      </div>
    );
  }
  if (!detail.data) {
    return <ErrorAlert error={detail.error} />;
  }
  const { item, actions, history, stageTerminal, stages = [] } = detail.data;
  const steps = stageSteps(stages, item.stageCode, history);
  const generic = actions.filter((a) => a.generic);
  const business = actions.filter((a) => !a.generic);
  return (
    <section className="workflow-panel" aria-label="Workflow status">
      <StageStepper steps={steps} />
      <div className="workflow-head">
        <StageMeta item={item} terminal={stageTerminal} />
        <div className="workflow-actions">
          {renderBusinessActions?.(business)}
          {generic.map((a) => (
            <Button key={a.action} variant="secondary" size="sm" onClick={() => setPending(a)}>
              {titleCase(a.label)}
            </Button>
          ))}
        </div>
      </div>
      {showHistory && <HistoryTable history={history} terminal={stageTerminal} />}
      {pending && (
        <ActionDialog
          title={titleCase(pending.label)}
          record={item.reference}
          effect={`The record moves to ${titleCase(pending.toStageName ?? pending.toStage)}.`}
          reasonLov={pending.reasonLov}
          confirmLabel={titleCase(pending.label)}
          busy={act.isPending}
          error={act.error}
          onClose={() => setPending(null)}
          onConfirm={(note) => act.mutate({ action: pending, note })}
        />
      )}
    </section>
  );
}

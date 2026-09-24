import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlarmClock, UserCheck } from 'lucide-react';
import { useState } from 'react';
import type { ReactNode } from 'react';
import { workflowApi } from '@/api/workflow';
import type { ActionNote, WorkAction } from '@/api/workflow';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { ActionDialog } from './ActionDialog';
import { StageTimeline } from './StageTimeline';
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
}

/**
 * Where a record stands (BRNB.022/115): stage, since when, due time (SLA), assignee, the
 * actions the current user may take, and the status history. Generic actions (return, void,
 * decline) run here with their reason; business actions are supplied by the page.
 */
export function WorkflowPanel({
  entityType,
  entityId,
  renderBusinessActions,
  onChanged,
  showHistory = true,
}: Readonly<WorkflowPanelProps>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<WorkAction | null>(null);
  const key = workflowKey(entityType, entityId);
  const detail = useQuery({
    queryKey: key,
    queryFn: () => workflowApi.byRecord(entityType, entityId),
  });
  const act = useMutation({
    mutationFn: ({ action, note }: { action: WorkAction; note: ActionNote }) =>
      workflowApi.act(detail.data?.item.id ?? 0, action.action, note),
    onSuccess: async (result) => {
      setPending(null);
      queryClient.setQueryData(key, result);
      await queryClient.invalidateQueries({ queryKey: ['workflow', 'queue'] });
      toast.success(`Moved to ${result.item.stageName}`);
      onChanged?.();
    },
  });

  if (detail.isLoading) {
    return <div className="workflow-panel muted">Loading status…</div>;
  }
  if (!detail.data) {
    return <ErrorAlert error={detail.error} />;
  }
  const { item, actions, history, stageTerminal } = detail.data;
  const generic = actions.filter((a) => a.generic);
  const business = actions.filter((a) => !a.generic);
  return (
    <section className="workflow-panel" aria-label="Workflow status">
      <div className="workflow-head">
        <div>
          <div className="workflow-stage">
            <StatusBadge status={item.stageCode} />
            <span className="workflow-stage-name">{item.stageName}</span>
          </div>
          <div className="workflow-meta muted">
            Since {formatDateTime(item.stageEnteredAt)}
            {item.dueAt && !stageTerminal && (
              <span className={item.overdue ? 'workflow-overdue' : ''}>
                {' '}
                · <AlarmClock size={12} aria-hidden="true" /> due {formatDateTime(item.dueAt)}
                {item.overdue && ' (overdue)'}
              </span>
            )}
            {item.assignee && (
              <span>
                {' '}
                · <UserCheck size={12} aria-hidden="true" /> {item.assignee}
              </span>
            )}
          </div>
        </div>
        <div className="row workflow-actions">
          {renderBusinessActions?.(business)}
          {generic.map((a) => (
            <Button key={a.action} variant="secondary" size="sm" onClick={() => setPending(a)}>
              {a.label}
            </Button>
          ))}
        </div>
      </div>
      {showHistory && <StageTimeline history={history} />}
      {pending && (
        <ActionDialog
          title={pending.label}
          reasonLov={pending.reasonLov}
          confirmLabel={pending.label}
          busy={act.isPending}
          error={act.error}
          onClose={() => setPending(null)}
          onConfirm={(note) => act.mutate({ action: pending, note })}
        />
      )}
    </section>
  );
}

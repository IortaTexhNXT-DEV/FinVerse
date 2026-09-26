import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlarmClock, UserCheck } from 'lucide-react';
import { useState } from 'react';
import type { ReactNode } from 'react';
import { workflowApi } from '@/api/workflow';
import type { ActionNote, WorkAction } from '@/api/workflow';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, titleCase } from '@/utils/format';
import { ActionDialog } from './ActionDialog';
import { HistoryTable } from './HistoryTable';
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
 * Where a record stands (BRNB.022/115) as a structured status panel: stage, in stage since, due
 * time (SLA), assignee, the actions the current user may take, and the status history table.
 * Generic actions (return, void, decline) run here with their reason; business actions are
 * supplied by the page.
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
  const { item, actions, history, stageTerminal } = detail.data;
  const generic = actions.filter((a) => a.generic);
  const business = actions.filter((a) => !a.generic);
  return (
    <section className="workflow-panel" aria-label="Workflow status">
      <div className="workflow-head">
        <dl className="status-strip">
          <div>
            <dt>Stage</dt>
            <dd className="workflow-stage">
              <StatusBadge status={item.stageCode} />
              <span className="workflow-stage-name">{titleCase(item.stageName)}</span>
            </dd>
          </div>
          <div>
            <dt>In Stage Since</dt>
            <dd className="nowrap">{formatDateTime(item.stageEnteredAt)}</dd>
          </div>
          {!stageTerminal && (
            <div>
              <dt>Due</dt>
              <dd className={item.overdue ? 'nowrap workflow-overdue' : 'nowrap'}>
                {item.dueAt ? (
                  <>
                    <AlarmClock size={14} aria-hidden="true" /> {formatDateTime(item.dueAt)}
                    {item.overdue && (
                      <>
                        {' '}
                        <StatusBadge status="OVERDUE" />
                      </>
                    )}
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
        <div className="row workflow-actions">
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

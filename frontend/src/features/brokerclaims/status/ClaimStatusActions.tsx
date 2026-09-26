import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RotateCcw } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { claimStatusApi } from './api';
import type { ClaimProgress } from './api';
import {
  AdjusterDialog,
  ChangeStatusDialog,
  FollowUpDialog,
  SettlementDialog,
} from './StatusDialogs';
import { statusActions } from './statusLogic';
import type { StatusAction } from './statusLogic';

type Dialog = Exclude<StatusAction, 'actionPlan'>;

const ACTION_LABELS: Record<Dialog, string> = {
  changeStatus: 'Change Status',
  settle: 'Set Settlement',
  followUp: 'Override Follow-up Date',
  adjuster: 'Assign Adjuster',
  reopen: 'Reopen',
};

interface PanelDialogProps {
  dialog?: Dialog;
  progress: ClaimProgress;
  claimId: number;
  companyId: number;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  run: (fn: () => Promise<unknown>) => void;
}

/** The dialog of the chosen status action. */
function PanelDialog({
  dialog,
  progress,
  claimId,
  companyId,
  busy,
  error,
  onClose,
  run,
}: Readonly<PanelDialogProps>) {
  const common = { busy, error, onClose };
  switch (dialog) {
    case 'changeStatus':
      return (
        <ChangeStatusDialog
          {...common}
          claimId={claimId}
          companyId={companyId}
          onSave={(code, remark) =>
            run(() => claimStatusApi.changeStatus(claimId, companyId, code, remark || undefined))
          }
        />
      );
    case 'settle':
      return (
        <SettlementDialog
          {...common}
          onSave={(input) => run(() => claimStatusApi.settle(claimId, companyId, input))}
        />
      );
    case 'followUp':
      return (
        <FollowUpDialog
          {...common}
          onSave={(date, reason) =>
            run(() => claimStatusApi.overrideFollowUp(claimId, companyId, date, reason))
          }
        />
      );
    case 'adjuster':
      return (
        <AdjusterDialog
          {...common}
          current={progress.followUp.adjusterCode}
          onSave={(code, remark) =>
            run(() => claimStatusApi.assignAdjuster(claimId, companyId, code, remark || undefined))
          }
        />
      );
    case 'reopen':
      return (
        <ActionDialog
          title={`Reopen Claim ${progress.claimNo}`}
          reasonLov="BCL_REOPEN_REASON"
          confirmLabel="Reopen Claim"
          busy={busy}
          error={error}
          onClose={onClose}
          onConfirm={(note) =>
            run(() =>
              claimStatusApi.reopen(claimId, companyId, note.reasonCode ?? '', note.comment),
            )
          }
        />
      );
    default:
      return null;
  }
}

/**
 * The status actions of a claim for the page header of the claim record (FR-CM-041-045/050):
 * Change Status, Set Settlement, Override Follow-up Date, Assign Adjuster and Reopen, as the
 * user's rights and the claim's phase allow (a closed claim offers only Reopen).
 */
export function ClaimStatusActions({
  claimId,
  companyId,
}: Readonly<{ claimId: number; companyId: number }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [dialog, setDialog] = useState<Dialog>();
  const progress = useQuery({
    queryKey: ['broker-claims', 'progress', claimId],
    queryFn: () => claimStatusApi.progress(claimId, companyId),
  });
  const action = useMutation({
    mutationFn: (run: () => Promise<unknown>) => run(),
    onSuccess: async () => {
      setDialog(undefined);
      await queryClient.invalidateQueries({ queryKey: ['broker-claims'] });
      toast.success('Claim updated');
    },
  });
  const p = progress.data;
  if (p === undefined) {
    return <ErrorAlert error={progress.error} />;
  }
  const actions = statusActions(p.status.phase, can).filter((a): a is Dialog => a !== 'actionPlan');
  return (
    <>
      {actions.map((a) => (
        <Button
          key={a}
          variant={a === 'changeStatus' || a === 'reopen' ? 'primary' : 'secondary'}
          icon={a === 'reopen' ? <RotateCcw size={16} /> : undefined}
          onClick={() => setDialog(a)}
        >
          {ACTION_LABELS[a]}
        </Button>
      ))}
      <PanelDialog
        dialog={dialog}
        progress={p}
        claimId={claimId}
        companyId={companyId}
        busy={action.isPending}
        error={action.error}
        onClose={() => setDialog(undefined)}
        run={(fn) => action.mutate(fn)}
      />
    </>
  );
}

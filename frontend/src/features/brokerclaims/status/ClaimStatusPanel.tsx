import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CalendarClock,
  Clock,
  Flag,
  Hourglass,
  NotebookPen,
  RotateCcw,
  ShieldCheck,
  UserRound,
} from 'lucide-react';
import { useState } from 'react';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useAuth } from '@/auth/authContext';
import { formatAmount, formatDate } from '@/utils/format';
import { claimStatusApi } from './api';
import type { ClaimProgress } from './api';
import {
  AdjusterDialog,
  ChangeStatusDialog,
  FollowUpDialog,
  SettlementDialog,
} from './StatusDialogs';
import { ACTION_PLAN_MAX, PHASE_LABELS, progressFlags, statusActions } from './statusLogic';
import type { StatusAction } from './statusLogic';

type Dialog = Exclude<StatusAction, 'actionPlan'>;

const ACTION_LABELS: Record<Dialog, string> = {
  changeStatus: 'Change Status',
  settle: 'Set Settlement',
  followUp: 'Override Follow-up Date',
  adjuster: 'Assign Adjuster',
  reopen: 'Reopen',
};

function settlementText(progress: ClaimProgress): string {
  const s = progress.settlement;
  if (s.typeCode === undefined) {
    return '—';
  }
  return s.amount === undefined ? s.typeLabel : `${s.typeLabel} · ${formatAmount(s.amount)}`;
}

function Facts({ progress }: Readonly<{ progress: ClaimProgress }>) {
  const facts = [
    { icon: Flag, label: 'Phase', value: PHASE_LABELS[progress.status.phase] },
    { icon: Hourglass, label: 'Age this stage', value: `${progress.ages.thisStage} day(s)` },
    { icon: Clock, label: 'Age overall', value: `${progress.ages.overall} day(s)` },
    {
      icon: CalendarClock,
      label: 'Next follow-up',
      value: formatDate(progress.followUp.nextFollowUpDate),
    },
    { icon: UserRound, label: 'Adjuster', value: progress.followUp.adjusterName ?? '—' },
    {
      icon: ShieldCheck,
      label: 'Settlement',
      value: settlementText(progress),
    },
  ];
  return (
    <div className="fact-grid">
      {facts.map(({ icon: Icon, label, value }) => (
        <div className="fact" key={label}>
          <Icon size={20} aria-hidden="true" />
          <span>
            <span className="fact-label">{label}</span>
            <span className="fact-value">{value}</span>
          </span>
        </div>
      ))}
    </div>
  );
}

function ActionPlan({
  progress,
  editable,
  onSave,
  busy,
}: Readonly<{
  progress: ClaimProgress;
  editable: boolean;
  onSave: (text: string) => void;
  busy: boolean;
}>) {
  const [text, setText] = useState(progress.followUp.nextActionPlan ?? '');
  const tooLong = text.length > ACTION_PLAN_MAX;
  return (
    <Field
      label="Next action plan"
      error={tooLong ? 'The action plan can have up to 2000 characters' : undefined}
      hint={`${text.length} / ${ACTION_PLAN_MAX} characters; every version is kept in the history.`}
    >
      {(id) => (
        <div className="stack">
          <textarea
            id={id}
            className="textarea"
            rows={3}
            value={text}
            readOnly={!editable}
            onChange={(e) => setText(e.target.value)}
          />
          {editable && (
            <div>
              <Button
                size="sm"
                icon={<NotebookPen size={14} />}
                busy={busy}
                disabled={tooLong || text === (progress.followUp.nextActionPlan ?? '')}
                onClick={() => onSave(text)}
              >
                Save Action Plan
              </Button>
            </div>
          )}
        </div>
      )}
    </Field>
  );
}

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
 * Status, follow-up and settlement of a claim with its actions (FR-CL-041-045/050/051/053): the
 * status pill, phase, ages, next follow-up, adjuster and settlement, and the buttons the user's
 * rights and the phase allow. Mounted by the claim record page (wave CL1-A) above its tabs.
 */
export function ClaimStatusPanel({
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
  if (progress.data === undefined) {
    return <ErrorAlert error={progress.error} />;
  }
  const p = progress.data;
  const actions = statusActions(p.status.phase, can);
  const busy = action.isPending;
  const run = (fn: () => Promise<unknown>) => action.mutate(fn);
  return (
    <Card
      title="Status and Follow-up"
      actions={
        <>
          {actions
            .filter((a): a is Dialog => a !== 'actionPlan')
            .map((a) => (
              <Button
                key={a}
                size="sm"
                variant={a === 'changeStatus' ? 'primary' : 'secondary'}
                icon={a === 'reopen' ? <RotateCcw size={14} /> : undefined}
                onClick={() => setDialog(a)}
              >
                {ACTION_LABELS[a]}
              </Button>
            ))}
        </>
      }
    >
      <div className="stack">
        <div className="record-summary-head">
          <StatusBadge status={p.status.label || PHASE_LABELS[p.status.phase]} />
          {progressFlags(p).map((f) => (
            <span key={f} className="tag">
              {f}
            </span>
          ))}
        </div>
        <Facts progress={p} />
        <ActionPlan
          key={p.followUp.nextActionPlan ?? ''}
          progress={p}
          editable={actions.includes('actionPlan')}
          busy={busy}
          onSave={(text) => run(() => claimStatusApi.planNextAction(claimId, companyId, text))}
        />
      </div>
      <PanelDialog
        dialog={dialog}
        progress={p}
        claimId={claimId}
        companyId={companyId}
        busy={busy}
        error={action.error}
        onClose={() => setDialog(undefined)}
        run={run}
      />
    </Card>
  );
}

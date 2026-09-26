import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CalendarClock,
  Clock,
  Flag,
  Hourglass,
  NotebookPen,
  ShieldCheck,
  UserRound,
} from 'lucide-react';
import { useState } from 'react';
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
import { ACTION_PLAN_MAX, PHASE_LABELS, progressFlags, statusActions } from './statusLogic';

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

/**
 * Status, follow-up and settlement of a claim (FR-CL-042/050/051/053): the status pill and flags,
 * phase, ages, next follow-up, adjuster and settlement, and the next action plan (editable with
 * BCL_ACTION_PLAN while the claim is open). The status actions are {@link ClaimStatusActions}.
 */
export function ClaimStatusPanel({
  claimId,
  companyId,
}: Readonly<{ claimId: number; companyId: number }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const progress = useQuery({
    queryKey: ['broker-claims', 'progress', claimId],
    queryFn: () => claimStatusApi.progress(claimId, companyId),
  });
  const save = useMutation({
    mutationFn: (text: string) => claimStatusApi.planNextAction(claimId, companyId, text),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['broker-claims'] });
      toast.success('Action plan saved');
    },
  });
  if (progress.data === undefined) {
    return <ErrorAlert error={progress.error} />;
  }
  const p = progress.data;
  return (
    <Card title="Status and Follow-up">
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
        <ErrorAlert error={save.error} />
        <ActionPlan
          key={p.followUp.nextActionPlan ?? ''}
          progress={p}
          editable={statusActions(p.status.phase, can).includes('actionPlan')}
          busy={save.isPending}
          onSave={(text) => save.mutate(text)}
        />
      </div>
    </Card>
  );
}

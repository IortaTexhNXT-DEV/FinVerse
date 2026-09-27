import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Ban, CheckCircle2, RefreshCw, Send, Undo2, Upload } from 'lucide-react';
import { useState } from 'react';
import { reservesApi } from '@/api/reserves';
import type { ValuationRun } from '@/api/reserves';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { runActions } from './reserveMath';

type ReasonAction = 'reject' | 'cancel';

const REASON_TITLES: Record<ReasonAction, string> = {
  reject: 'Reject the run back to preview',
  cancel: 'Cancel the run',
};

/** Life-cycle buttons of a valuation run (maker: preview / submit, checker: approve / post). */
export function RunActionBar({ run }: Readonly<{ run: ValuationRun }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [reasonFor, setReasonFor] = useState<ReasonAction | null>(null);
  const [reason, setReason] = useState('');
  const actions = runActions(run.status, can);

  const refresh = async (message: string) => {
    await queryClient.invalidateQueries({ queryKey: ['reserve-run', run.id] });
    await queryClient.invalidateQueries({ queryKey: ['reserve-runs'] });
    await queryClient.invalidateQueries({ queryKey: ['reserve-upr', run.id] });
    toast.success(message);
  };
  const step = useMutation({
    mutationFn: async (action: 'recalculate' | 'submit' | 'approve' | 'post') => {
      if (action === 'recalculate') {
        await reservesApi.recalculate(run.id);
      } else {
        await reservesApi[action](run.id);
      }
      return action;
    },
    onSuccess: (action) => refresh(`Run ${run.periodName}: ${action} done`),
  });
  const withReason = useMutation({
    mutationFn: (action: ReasonAction) =>
      action === 'reject' ? reservesApi.reject(run.id, reason) : reservesApi.cancel(run.id, reason),
    onSuccess: async (r) => {
      setReasonFor(null);
      setReason('');
      await refresh(`Run ${r.periodName} is now ${r.status.toLowerCase().replace('_', ' ')}`);
    },
  });
  const busy = step.isPending || withReason.isPending;

  return (
    <>
      <div className="row" style={{ gap: 'var(--space-2)', flexWrap: 'wrap' }}>
        {actions.recalculate && (
          <Button
            variant="secondary"
            icon={<RefreshCw size={16} />}
            disabled={busy}
            onClick={() => step.mutate('recalculate')}
          >
            Recalculate
          </Button>
        )}
        {actions.submit && (
          <Button
            variant="accent"
            icon={<Send size={16} />}
            disabled={busy}
            onClick={() => step.mutate('submit')}
          >
            Submit for Approval
          </Button>
        )}
        {actions.approve && (
          <Button
            variant="accent"
            icon={<CheckCircle2 size={16} />}
            disabled={busy}
            onClick={() => step.mutate('approve')}
          >
            Approve
          </Button>
        )}
        {actions.reject && (
          <Button
            variant="secondary"
            icon={<Undo2 size={16} />}
            disabled={busy}
            onClick={() => setReasonFor('reject')}
          >
            Reject
          </Button>
        )}
        {actions.post && (
          <Button
            variant="accent"
            icon={<Upload size={16} />}
            disabled={busy}
            onClick={() => step.mutate('post')}
          >
            Post Journals
          </Button>
        )}
        {actions.cancel && (
          <Button
            variant="danger"
            icon={<Ban size={16} />}
            disabled={busy}
            onClick={() => setReasonFor('cancel')}
          >
            {run.status === 'POSTED' ? 'Cancel and reverse' : 'Cancel'}
          </Button>
        )}
      </div>
      <ErrorAlert error={step.error} />
      <Modal
        title={reasonFor ? REASON_TITLES[reasonFor] : ''}
        open={reasonFor !== null}
        onClose={() => setReasonFor(null)}
        footer={
          <Button
            variant="danger"
            busy={withReason.isPending}
            disabled={reason.trim() === ''}
            onClick={() => reasonFor && withReason.mutate(reasonFor)}
          >
            Confirm
          </Button>
        }
      >
        <ErrorAlert error={withReason.error} />
        <Field label="Reason" required>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={200}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          )}
        </Field>
      </Modal>
    </>
  );
}

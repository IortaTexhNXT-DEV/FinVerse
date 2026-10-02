import { useMutation } from '@tanstack/react-query';
import { CheckCircle2, Pencil, Send, Trash2, XCircle } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import type { DecisionMode } from './DecisionDialog';
import { DecisionDialog } from './DecisionDialog';
import type { WorkflowFacts } from './workflow';
import { allowedActions } from './workflow';

export interface WorkflowHandlers {
  submit: () => Promise<unknown>;
  discard: () => Promise<unknown>;
  approve: (accountingDate: string) => Promise<unknown>;
  reject: (reason: string) => Promise<unknown>;
  edit?: () => void;
  done: (message: string) => Promise<void>;
}

/**
 * Maker actions (edit, submit, discard) and checker actions (approve with accounting date, reject
 * with reason) for a policy, endorsement or certificate, shown according to maker-checker rules.
 */
export function WorkflowActions({
  facts,
  label,
  handlers,
  compact = false,
}: Readonly<{
  facts: WorkflowFacts;
  label: string;
  handlers: WorkflowHandlers;
  compact?: boolean;
}>) {
  const { user, can } = useAuth();
  const toast = useToast();
  const [dialog, setDialog] = useState<DecisionMode>(null);
  const allowed = allowedActions(facts, user?.username, can);
  const size = compact ? 'sm' : 'md';

  const action = useMutation({
    mutationFn: ({ run }: { run: () => Promise<unknown>; message: string }) => run(),
    onSuccess: async (_result, { message }) => {
      setDialog(null);
      toast.success(`${label}: ${message}`);
      await handlers.done(message);
    },
  });
  const perform = (run: () => Promise<unknown>, message: string) => action.mutate({ run, message });
  const edit = allowed.edit ? handlers.edit : undefined;

  return (
    <>
      {edit !== undefined && (
        <Button size={size} variant="secondary" icon={<Pencil size={16} />} onClick={edit}>
          Edit
        </Button>
      )}
      {allowed.submit && (
        <Button
          size={size}
          variant="accent"
          icon={<Send size={16} />}
          busy={action.isPending}
          onClick={() => perform(handlers.submit, 'submitted for approval')}
        >
          Submit
        </Button>
      )}
      {allowed.discard && (
        <Button
          size={size}
          variant="ghost"
          icon={<Trash2 size={16} />}
          onClick={() => perform(handlers.discard, 'discarded')}
        >
          Discard
        </Button>
      )}
      {allowed.approve && (
        <Button
          size={size}
          variant="accent"
          icon={<CheckCircle2 size={16} />}
          onClick={() => setDialog('approve')}
        >
          Approve
        </Button>
      )}
      {allowed.reject && (
        <Button
          size={size}
          variant="danger"
          icon={<XCircle size={16} />}
          onClick={() => setDialog('reject')}
        >
          Reject
        </Button>
      )}
      {dialog === null && <ErrorAlert error={action.error} />}
      <DecisionDialog
        mode={dialog}
        label={label}
        busy={action.isPending}
        error={action.error}
        onClose={() => setDialog(null)}
        onApprove={(date) => perform(() => handlers.approve(date), 'approved and posted')}
        onReject={(reason) => perform(() => handlers.reject(reason), 'returned to the maker')}
      />
    </>
  );
}

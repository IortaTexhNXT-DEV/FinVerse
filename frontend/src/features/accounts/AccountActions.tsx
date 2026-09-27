import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { accountsApi } from '@/api/accounts';
import type { Account } from '@/api/accounts';
import type { ActionNote, WorkAction } from '@/api/workflow';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';
import { useAccountRefresh } from './useAccountRefresh';

/** Workflow actions of an account run by this screen (others belong to later stages). */
const HANDLED: Record<string, (id: number, comment?: string) => Promise<Account>> = {
  submit: accountsApi.submit,
  resubmit: accountsApi.resubmit,
  validate: accountsApi.validate,
  direct_booking: accountsApi.directBooking,
};

/**
 * Business actions of the account workflow offered to the user: Submit and Resubmit
 * (Marketing), Validate and Direct booking (Processing). Each asks for an optional comment.
 */
export function AccountActions({
  account,
  actions,
}: Readonly<{ account: Account; actions: WorkAction[] }>) {
  const toast = useToast();
  const refresh = useAccountRefresh(account.id);
  const [pending, setPending] = useState<WorkAction | null>(null);
  const run = useMutation({
    mutationFn: ({ action, note }: { action: WorkAction; note: ActionNote }) => {
      const call = HANDLED[action.action];
      if (call === undefined) {
        return Promise.reject(new Error(`${action.label} is not available here`));
      }
      return call(account.id, note.comment);
    },
    onSuccess: async (updated, { action }) => {
      setPending(null);
      await refresh(updated);
      toast.success(
        `${action.label}: ${updated.arn} is now ${updated.status.toLowerCase().replace(/_/g, ' ')}`,
      );
    },
  });
  const offered = actions.filter((a) => a.action in HANDLED);
  return (
    <>
      {offered.map((a) => (
        <Button key={a.action} size="sm" variant="accent" onClick={() => setPending(a)}>
          {a.label}
        </Button>
      ))}
      {pending && (
        <ActionDialog
          title={pending.label}
          confirmLabel={pending.label}
          busy={run.isPending}
          error={run.error}
          onClose={() => setPending(null)}
          onConfirm={(note) => run.mutate({ action: pending, note })}
        />
      )}
    </>
  );
}

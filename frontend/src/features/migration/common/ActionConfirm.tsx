import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { useToast } from '@/components/ui/toastContext';

/** An action of the migration console confirmed in a dialog. */
export interface MigAction {
  title: string;
  record?: string;
  effect: ReactNode;
  confirmLabel: string;
  reason?: 'required' | 'optional';
  destructive?: boolean;
  done: string;
  run: (reason: string) => Promise<unknown>;
}

/**
 * Confirms and runs an action (sign a gate, approve a load, request a rollback...), then refreshes
 * the migration screens and reports the result.
 */
export function ActionConfirm({
  action,
  onClose,
}: Readonly<{ action: MigAction | undefined; onClose: () => void }>) {
  const client = useQueryClient();
  const toast = useToast();
  const mutation = useMutation({
    mutationFn: (reason: string) => (action ? action.run(reason) : Promise.resolve()),
    onSuccess: async () => {
      toast.success(action?.done ?? 'Done');
      await client.invalidateQueries({ queryKey: ['migration'] });
      mutation.reset();
      onClose();
    },
  });
  if (action === undefined) {
    return null;
  }
  return (
    <ConfirmDialog
      title={action.title}
      record={action.record}
      effect={action.effect}
      confirmLabel={action.confirmLabel}
      reason={action.reason}
      destructive={action.destructive}
      busy={mutation.isPending}
      error={mutation.error}
      onConfirm={(reason) => mutation.mutate(reason)}
      onClose={() => {
        mutation.reset();
        onClose();
      }}
    />
  );
}

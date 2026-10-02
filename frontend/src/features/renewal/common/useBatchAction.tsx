import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { BatchOutcome } from '@/api/renewal';
import { useToast } from '@/components/ui/toastContext';
import { OutcomeDialog } from './RenewalBits';
import { outcomeSummary } from './renewalCodes';

/**
 * An action on several renewals: runs it, refreshes the renewal lists, toasts the counts and shows
 * the refused renewals with their reason.
 */
export function useBatchAction<A>(
  title: string,
  verb: string,
  run: (args: A) => Promise<BatchOutcome>,
  onDone?: () => void,
) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [outcome, setOutcome] = useState<BatchOutcome>();
  const mutation = useMutation({
    mutationFn: run,
    onSuccess: async (result) => {
      onDone?.();
      toast.success(outcomeSummary(result, verb));
      if (Object.keys(result.refused).length > 0) {
        setOutcome(result);
      }
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  const dialog =
    outcome === undefined ? null : (
      <OutcomeDialog title={title} outcome={outcome} onClose={() => setOutcome(undefined)} />
    );
  return { mutation, dialog };
}

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/components/ui/toastContext';
import { billingApi } from '../billing/api';
import { plansApi } from './api';

/**
 * The actions of an installment plan record: allocate the ledger payments now, cancel the plan and
 * generate the statement of account of a billing cycle (BRCLXN.053/058).
 */
export function usePlanActions(id: number, onCancelled: () => void) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const refreshAll = () => queryClient.invalidateQueries({ queryKey: ['collections'] });
  const refresh = useMutation({
    mutationFn: () => plansApi.refresh(id),
    onSuccess: async () => {
      await refreshAll();
      toast.success('Payments allocated from the invoice ledger');
    },
  });
  const cancel = useMutation({
    mutationFn: (reason: string) => plansApi.cancel(id, reason),
    onSuccess: async (p) => {
      onCancelled();
      await refreshAll();
      toast.success(`${p.planNo} cancelled`);
    },
  });
  const bill = useMutation({
    mutationFn: (seq: number) => billingApi.generate(id, seq),
    onSuccess: async (s) => {
      await refreshAll();
      toast.success(`${s.soaNo} generated`);
    },
  });
  return { refresh, cancel, bill };
}

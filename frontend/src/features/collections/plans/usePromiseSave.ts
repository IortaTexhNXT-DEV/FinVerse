import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/components/ui/toastContext';
import type { ItemResult } from './api';
import { plansApi } from './api';
import type { PromiseDraft } from './PlanDialogs';

/**
 * Saves a promise to pay: one invoice through the promise endpoint, several through the bulk
 * endpoint (one outcome per invoice). Refreshes the promise and plan lists.
 */
export function usePromiseSave(companyId: number, onDone: (results?: ItemResult[]) => void) {
  const toast = useToast();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (draft: PromiseDraft): Promise<ItemResult[] | undefined> => {
      const { invoiceNos, installmentId, ...terms } = draft;
      if (invoiceNos.length === 1) {
        await plansApi.recordPromise({
          companyId,
          invoiceNo: invoiceNos[0] ?? '',
          installmentId,
          ...terms,
        });
        return undefined;
      }
      return plansApi.bulkPromise({ companyId, invoiceNos, ...terms });
    },
    onSuccess: async (results) => {
      await queryClient.invalidateQueries({ queryKey: ['collections', 'promises'] });
      await queryClient.invalidateQueries({ queryKey: ['collections', 'plan'] });
      if (results === undefined) {
        toast.success('Promise to pay recorded');
      }
      onDone(results);
    },
  });
}

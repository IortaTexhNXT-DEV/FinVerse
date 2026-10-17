import { useMutation, useQueryClient } from '@tanstack/react-query';
import { renewalApi } from '@/api/renewal';
import { useToast } from '@/components/ui/toastContext';

/** Resends a letter: the same stored letter, a new transmission (FRRN.022.01, FRRN.023.02). */
export function useResend(companyId: number) {
  const toast = useToast();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (letterNo: string) => renewalApi.resendLetter(companyId, letterNo),
    onSuccess: async (r) => {
      if (r.refusal) toast.error(r.refusal);
      else toast.success(`${r.letterNo} resent`);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
}

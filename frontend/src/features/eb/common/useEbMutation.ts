import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';

/**
 * A change of an EB record: runs with the company, then closes the dialog, shows the toast and
 * refreshes the Employee Benefits queries.
 */
export function useEbMutation<V, R = unknown>(
  run: (companyId: number, value: V) => Promise<R>,
  success: string | ((result: R) => string),
  done: () => void,
) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (value: V) => run(companyId, value),
    onSuccess: async (result: R) => {
      done();
      toast.success(typeof success === 'string' ? success : success(result));
      await queryClient.invalidateQueries({ queryKey: ['eb'] });
    },
  });
}

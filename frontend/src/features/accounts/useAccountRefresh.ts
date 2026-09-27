import { useQueryClient } from '@tanstack/react-query';
import { ACCOUNT_ENTITY } from '@/api/accounts';
import type { Account } from '@/api/accounts';
import { workflowKey } from '@/components/broking/workflowKey';

/** Refreshes an account, its check, its workflow panel and the account lists. */
export function useAccountRefresh(id: number) {
  const queryClient = useQueryClient();
  return async (account?: Account) => {
    if (account) {
      queryClient.setQueryData(['account', id], account);
    }
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['account', id] }),
      queryClient.invalidateQueries({ queryKey: workflowKey(ACCOUNT_ENTITY, id) }),
      queryClient.invalidateQueries({ queryKey: ['accounts'] }),
      queryClient.invalidateQueries({ queryKey: ['workflow', 'queue'] }),
    ]);
  };
}

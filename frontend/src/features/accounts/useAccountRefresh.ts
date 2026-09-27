import { useQueryClient } from '@tanstack/react-query';
import { ACCOUNT_ENTITY } from '@/api/accounts';
import type { Account } from '@/api/accounts';
import { workflowKey } from '@/components/broking/workflowKey';

/**
 * Refreshes an account, its check, its workflow panel and the account lists, so the header status
 * and the stepper show the same state after an action. The account returned by the action (when
 * given) is shown at once; without an id the hook refreshes the account it is given.
 */
export function useAccountRefresh(id?: number) {
  const queryClient = useQueryClient();
  return async (account?: Account) => {
    const target = account?.id ?? id;
    if (target === undefined) {
      return;
    }
    if (account) {
      queryClient.setQueryData(['account', target], account);
    }
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['account', target] }),
      queryClient.invalidateQueries({ queryKey: workflowKey(ACCOUNT_ENTITY, target) }),
      queryClient.invalidateQueries({ queryKey: ['accounts'] }),
      queryClient.invalidateQueries({ queryKey: ['workflow', 'queue'] }),
    ]);
  };
}

import { useQuery } from '@tanstack/react-query';
import { receivablesApi } from '@/api/receivables';
import { useCompanyId, useDefaultBranchId } from '@/context/workspaceContext';

/**
 * Company, working branch (the shared default: header branch, home branch, head office) and bank
 * accounts of the receivables screens.
 */
export function useReceivablesLookups() {
  const companyId = useCompanyId();
  const banks = useQuery({
    queryKey: ['receivables', 'bank-accounts', companyId],
    queryFn: () => receivablesApi.bankAccounts(companyId),
    enabled: companyId > 0,
    staleTime: 300_000,
  });
  const workingBranch = useDefaultBranchId();
  return { companyId, branchId: workingBranch, bankAccounts: banks.data ?? [] };
}

/** Options for a bank account select. */
export function bankOptions(accounts: { code: string; name: string; currency: string }[]) {
  return accounts.map((a) => ({ value: a.code, label: `${a.code} - ${a.name} (${a.currency})` }));
}

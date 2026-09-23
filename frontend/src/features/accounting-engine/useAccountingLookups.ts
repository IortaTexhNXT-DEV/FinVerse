import { useQuery } from '@tanstack/react-query';
import { accountingApi } from '@/api/accounting';
import { useCompanyId } from '@/context/workspaceContext';

/** Event type catalogue and the company's accounting rules (cached). */
export function useAccountingLookups() {
  const companyId = useCompanyId();
  const eventTypes = useQuery({
    queryKey: ['event-types'],
    queryFn: accountingApi.eventTypes,
    staleTime: 300_000,
  });
  const rules = useQuery({
    queryKey: ['accounting-rules', companyId],
    queryFn: () => accountingApi.rules(companyId),
    enabled: companyId > 0,
  });
  return {
    companyId,
    eventTypes: eventTypes.data ?? [],
    rules: rules.data ?? [],
    loading: eventTypes.isLoading || rules.isLoading,
    error: eventTypes.error ?? rules.error,
  };
}

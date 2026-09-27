import { useQuery } from '@tanstack/react-query';
import { glApi } from '@/api/gl';
import { mastersApi } from '@/api/masters';
import { useCompanyId } from '@/context/workspaceContext';

/** Cached master data used by GL screens (accounts, currencies, dimensions). */
export function useGlLookups() {
  const companyId = useCompanyId();
  const accounts = useQuery({
    queryKey: ['accounts', companyId],
    queryFn: () => glApi.accounts(companyId),
    enabled: companyId > 0,
    staleTime: 60_000,
  });
  const currencies = useQuery({
    queryKey: ['currencies'],
    queryFn: mastersApi.currencies,
    staleTime: 300_000,
  });
  const costCenters = useQuery({
    queryKey: ['dimensions', companyId, 'COST_CENTER'],
    queryFn: () => mastersApi.dimensions(companyId, 'COST_CENTER'),
    enabled: companyId > 0,
    staleTime: 300_000,
  });
  const businessLines = useQuery({
    queryKey: ['dimensions', companyId, 'BUSINESS_LINE'],
    queryFn: () => mastersApi.dimensions(companyId, 'BUSINESS_LINE'),
    enabled: companyId > 0,
    staleTime: 300_000,
  });
  return {
    companyId,
    accounts: accounts.data ?? [],
    postableAccounts: (accounts.data ?? []).filter(
      (a) => a.postable && a.recordStatus === 'ACTIVE',
    ),
    currencies: currencies.data ?? [],
    costCenters: (costCenters.data ?? []).filter((d) => d.active),
    businessLines: (businessLines.data ?? []).filter((d) => d.active),
  };
}

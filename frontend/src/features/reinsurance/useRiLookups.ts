import { useQuery } from '@tanstack/react-query';
import { mastersApi } from '@/api/masters';
import { partiesApi } from '@/api/parties';
import { reinsuranceApi } from '@/api/reinsurance';
import { useCompanyId, useWorkspace } from '@/context/workspaceContext';

const LONG = 300_000;

/** Cached master data used by the reinsurance screens (treaties, reinsurers, classes). */
export function useRiLookups() {
  const companyId = useCompanyId();
  const { company } = useWorkspace();
  const enabled = companyId > 0;
  const treaties = useQuery({
    queryKey: ['ri-treaties', companyId],
    queryFn: () => reinsuranceApi.treaties(companyId),
    enabled,
    staleTime: 60_000,
  });
  const reinsurers = useQuery({
    queryKey: ['ri-parties', companyId, 'reinsurers'],
    queryFn: () => partiesApi.search(companyId, ['REINSURER']),
    enabled,
    staleTime: LONG,
  });
  const brokers = useQuery({
    queryKey: ['ri-parties', companyId, 'brokers'],
    queryFn: () => partiesApi.search(companyId, ['RI_BROKER']),
    enabled,
    staleTime: LONG,
  });
  const businessLines = useQuery({
    queryKey: ['dimensions', companyId, 'BUSINESS_LINE'],
    queryFn: () => mastersApi.dimensions(companyId, 'BUSINESS_LINE'),
    enabled,
    staleTime: LONG,
  });
  const active = (s: string) => s === 'ACTIVE';
  return {
    companyId,
    baseCurrency: company?.baseCurrency ?? 'PHP',
    treaties: treaties.data ?? [],
    treatiesLoading: treaties.isLoading,
    activeTreaties: (treaties.data ?? []).filter((t) => active(t.recordStatus)),
    reinsurers: (reinsurers.data ?? []).filter((p) => active(p.recordStatus)),
    brokers: (brokers.data ?? []).filter((p) => active(p.recordStatus)),
    businessLines: (businessLines.data ?? []).filter((d) => d.active),
  };
}

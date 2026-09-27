import { useQuery } from '@tanstack/react-query';
import { mastersApi } from '@/api/masters';
import { partiesApi } from '@/api/parties';
import { underwritingApi } from '@/api/underwriting';
import { useCompanyId, useWorkspace } from '@/context/workspaceContext';

const LONG = 300_000;

/** Cached master data used by underwriting screens (products, parties, currencies, classes). */
export function useUwLookups() {
  const companyId = useCompanyId();
  const { branches } = useWorkspace();
  const enabled = companyId > 0;
  const products = useQuery({
    queryKey: ['uw-products', companyId],
    queryFn: () => underwritingApi.products(companyId),
    enabled,
    staleTime: 60_000,
  });
  const clients = useQuery({
    queryKey: ['uw-parties', companyId, 'clients'],
    queryFn: () => partiesApi.search(companyId, ['INDIVIDUAL_CLIENT', 'CORPORATE_CLIENT']),
    enabled,
    staleTime: LONG,
  });
  const intermediaries = useQuery({
    queryKey: ['uw-parties', companyId, 'intermediaries'],
    queryFn: () => partiesApi.search(companyId, ['AGENT', 'BROKER']),
    enabled,
    staleTime: LONG,
  });
  const coinsurers = useQuery({
    queryKey: ['uw-parties', companyId, 'coinsurers'],
    queryFn: () => partiesApi.search(companyId, ['COINSURER']),
    enabled,
    staleTime: LONG,
  });
  const currencies = useQuery({
    queryKey: ['currencies'],
    queryFn: mastersApi.currencies,
    staleTime: LONG,
  });
  const businessLines = useQuery({
    queryKey: ['dimensions', companyId, 'BUSINESS_LINE'],
    queryFn: () => mastersApi.dimensions(companyId, 'BUSINESS_LINE'),
    enabled,
    staleTime: LONG,
  });
  const branchName = (id: number | undefined) =>
    branches.find((b) => b.id === id)?.code ?? (id === undefined ? '' : String(id));
  return {
    companyId,
    branches,
    branchName,
    products: products.data ?? [],
    activeProducts: (products.data ?? []).filter((p) => p.recordStatus === 'ACTIVE'),
    clients: clients.data ?? [],
    intermediaries: intermediaries.data ?? [],
    coinsurers: coinsurers.data ?? [],
    currencies: (currencies.data ?? []).filter((c) => c.active),
    businessLines: (businessLines.data ?? []).filter((d) => d.active),
  };
}

import { useQuery } from '@tanstack/react-query';
import { assetsApi } from '@/api/assets';
import { investmentsApi } from '@/api/investments';
import { partiesApi } from '@/api/parties';
import { useWorkspace } from '@/context/workspaceContext';
import { useGlLookups } from '@/features/gl/useLookups';

const STALE = 60_000;

/** Master data used by the asset and investment screens. */
export function useAssetLookups() {
  const { company, branches } = useWorkspace();
  const gl = useGlLookups();
  const companyId = gl.companyId;
  const categories = useQuery({
    queryKey: ['asset-categories', companyId],
    queryFn: () => assetsApi.categories(companyId),
    enabled: companyId > 0,
    staleTime: STALE,
  });
  const portfolios = useQuery({
    queryKey: ['investment-portfolios', companyId],
    queryFn: () => investmentsApi.portfolios(companyId),
    enabled: companyId > 0,
    staleTime: STALE,
  });
  const parties = useQuery({
    queryKey: ['parties', companyId, 'asset-lookups'],
    queryFn: () => partiesApi.search(companyId, []),
    enabled: companyId > 0,
    staleTime: STALE,
  });
  const activeParties = (parties.data ?? []).filter((p) => p.recordStatus === 'ACTIVE');
  return {
    companyId,
    baseCurrency: company?.baseCurrency ?? 'PHP',
    branches,
    branchName: (id: number | undefined) => branches.find((b) => b.id === id)?.code ?? '',
    categories: categories.data ?? [],
    activeCategories: (categories.data ?? []).filter((c) => c.recordStatus === 'ACTIVE'),
    portfolios: portfolios.data ?? [],
    activePortfolios: (portfolios.data ?? []).filter((p) => p.recordStatus === 'ACTIVE'),
    postableAccounts: gl.postableAccounts,
    bankAccounts: gl.postableAccounts.filter(
      (a) => a.categoryCode === 'BANK' || a.categoryCode === 'CASH',
    ),
    costCenters: gl.costCenters,
    suppliers: activeParties.filter((p) => p.partyType === 'SUPPLIER'),
    issuers: activeParties.filter(
      (p) => p.partyType === 'BANK' || p.partyType === 'CORPORATE_CLIENT',
    ),
  };
}

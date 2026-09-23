import { useQuery } from '@tanstack/react-query';
import { payablesApi } from '@/api/payables';
import { useCompanyId } from '@/context/workspaceContext';

/** Party types that can bill the company (vendor sub-ledger). */
export const VENDOR_TYPES = ['SUPPLIER', 'GARAGE', 'SURVEYOR'];

/** Party types the company pays (vendors, intermediaries, reinsurers, policyholders). */
export const PAYEE_TYPES = [
  ...VENDOR_TYPES,
  'AGENT',
  'BROKER',
  'REINSURER',
  'INDIVIDUAL_CLIENT',
  'CORPORATE_CLIENT',
];

/** Cached bank accounts and parties used by the payables screens. */
export function usePayablesLookups(partyTypes: string[] = VENDOR_TYPES) {
  const companyId = useCompanyId();
  const banks = useQuery({
    queryKey: ['bank-accounts', companyId],
    queryFn: () => payablesApi.bankAccounts(companyId),
    enabled: companyId > 0,
    staleTime: 60_000,
  });
  const parties = useQuery({
    queryKey: ['parties', companyId, partyTypes.join(',')],
    queryFn: () => payablesApi.parties(companyId, partyTypes),
    enabled: companyId > 0,
    staleTime: 300_000,
  });
  const allBanks = banks.data ?? [];
  return {
    companyId,
    banks: allBanks,
    activeBanks: allBanks.filter((b) => b.recordStatus === 'ACTIVE'),
    bankName: (id: number | undefined) => allBanks.find((b) => b.id === id)?.code ?? '',
    parties: parties.data ?? [],
  };
}

import { useQuery } from '@tanstack/react-query';
import { mastersApi } from '@/api/masters';
import { partiesApi } from '@/api/parties';
import type { Party, PartyType } from '@/api/parties';
import { useCompanyId } from '@/context/workspaceContext';

const LONG = 300_000;

function useParties(companyId: number, key: string, types: PartyType[]) {
  return useQuery({
    queryKey: ['claim-parties', companyId, key],
    queryFn: () => partiesApi.search(companyId, types),
    enabled: companyId > 0,
    staleTime: LONG,
  });
}

/** Label of a party in a select. */
export function partyOption(p: Party) {
  return { value: p.code, label: `${p.code} – ${p.name}` };
}

/** Cached master data used by the claims screens (parties by role, classes). */
export function useClaimLookups() {
  const companyId = useCompanyId();
  const clients = useParties(companyId, 'clients', ['INDIVIDUAL_CLIENT', 'CORPORATE_CLIENT']);
  const surveyors = useParties(companyId, 'surveyors', ['SURVEYOR']);
  const garages = useParties(companyId, 'garages', ['GARAGE']);
  const suppliers = useParties(companyId, 'suppliers', ['SUPPLIER']);
  const businessLines = useQuery({
    queryKey: ['dimensions', companyId, 'BUSINESS_LINE'],
    queryFn: () => mastersApi.dimensions(companyId, 'BUSINESS_LINE'),
    enabled: companyId > 0,
    staleTime: LONG,
  });
  const clientList = clients.data ?? [];
  const surveyorList = surveyors.data ?? [];
  const garageList = garages.data ?? [];
  return {
    companyId,
    clients: clientList,
    surveyors: surveyorList,
    garages: garageList,
    /** Parties a settlement can be paid to (payables CLAIM category). */
    payees: [...clientList, ...garageList, ...surveyorList],
    /** Parties money can be recovered from (salvage buyers, third parties). */
    payers: [...(suppliers.data ?? []), ...clientList],
    businessLines: (businessLines.data ?? []).filter((d) => d.active),
  };
}

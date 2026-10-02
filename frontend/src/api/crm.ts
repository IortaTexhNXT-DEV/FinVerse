import { api, toQuery } from './client';

export type ClientType = 'INDIVIDUAL' | 'CORPORATE';
export type ClientStatus = 'PROSPECT' | 'CONFIRMED' | 'INACTIVE';
export type KycStatus = 'NOT_STARTED' | 'PENDING' | 'VERIFIED' | 'EXPIRED';

export interface ClientSummary {
  id: number;
  code: string;
  prospectCode: string;
  clientCode?: string;
  displayName: string;
  clientType: ClientType;
  status: ClientStatus;
  kycStatus: KycStatus;
  email?: string;
  mobile?: string;
  marketSegment?: string;
  partyCode?: string;
}

/** Client lookups used across broking screens (the crm module adds the full client API). */
export const crmApi = {
  lookup: (companyId: number, q: string) =>
    api.get<ClientSummary[]>(`/crm/clients/lookup${toQuery({ companyId, q })}`),
  summary: (id: number) => api.get<ClientSummary>(`/crm/clients/${id}/summary`),
};

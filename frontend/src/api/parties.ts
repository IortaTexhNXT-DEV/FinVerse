import { api, toQuery } from './client';
import type { RecordStatus } from './types';

export const PARTY_TYPES = [
  'INDIVIDUAL_CLIENT',
  'CORPORATE_CLIENT',
  'AGENT',
  'BROKER',
  'REINSURER',
  'RI_BROKER',
  'COINSURER',
  'INSURER',
  'SUPPLIER',
  'GARAGE',
  'SURVEYOR',
  'BANK',
] as const;

export type PartyType = (typeof PARTY_TYPES)[number];

/** Business partner (policyholder, intermediary, reinsurer, supplier...). */
export interface Party {
  id: number;
  companyId: number;
  code: string;
  name: string;
  partyType: PartyType;
  taxId?: string;
  address?: string;
  email?: string;
  phone?: string;
  defaultCurrency: string;
  creditDays: number;
  commissionRate?: number;
  withholdingTaxRate?: number;
  licenceNo?: string;
  bankName?: string;
  bankAccountNo?: string;
  branchId?: number;
  recordStatus: RecordStatus;
  createdBy: string;
  /** Creator or last maintainer; unchanged by authorization. */
  maker?: string;
  authorizedBy?: string;
}

export type PartyInput = Omit<
  Party,
  'id' | 'recordStatus' | 'createdBy' | 'maker' | 'authorizedBy'
>;

export const partiesApi = {
  search: (companyId: number, types: PartyType[], q?: string) => {
    const typeQuery = types.map((t) => `&types=${t}`).join('');
    return api.get<Party[]>(`/parties${toQuery({ companyId, q })}${typeQuery}`);
  },
  get: (id: number) => api.get<Party>(`/parties/${id}`),
  create: (body: PartyInput) => api.post<Party>('/parties', body),
  update: (id: number, body: PartyInput) => api.put<Party>(`/parties/${id}`, body),
  authorize: (id: number) => api.post<Party>(`/parties/${id}/authorize`),
};

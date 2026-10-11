import { api, toQuery } from './client';

/** Recipient set-up of the Insurance Advices of a mortgagee bank. */
export interface AdviceRecipient {
  id: number;
  mortgageeBank: string;
  marketSegment: string | null;
  to: string[];
  cc: string[];
  autoSend: boolean;
  effectiveFrom: string;
  effectiveTo: string | null;
  recordStatus: string;
  maker: string | null;
  authorizedBy: string | null;
  authorizedAt: string | null;
}

/** Values of a recipient set-up. */
export interface AdviceRecipientData {
  mortgageeBank: string;
  marketSegment?: string;
  to: string[];
  cc: string[];
  autoSend: boolean;
  effectiveFrom: string;
  effectiveTo?: string;
}

const BASE = '/issuance/advice-recipients';

/** Insurance Advice recipients per mortgagee bank, maker-checker. */
export const adviceRecipientsApi = {
  list: (companyId: number) => api.get<AdviceRecipient[]>(`${BASE}${toQuery({ companyId })}`),
  save: (companyId: number, id: number | null, data: AdviceRecipientData) =>
    id === null
      ? api.post<AdviceRecipient>(`${BASE}${toQuery({ companyId })}`, data)
      : api.put<AdviceRecipient>(`${BASE}/${id}${toQuery({ companyId })}`, data),
  action: (companyId: number, id: number, action: 'authorize' | 'deactivate') =>
    api.post<AdviceRecipient>(`${BASE}/${id}/${action}${toQuery({ companyId })}`),
};

import { api, toQuery } from './client';

export type ItemDirection = 'DEBIT' | 'CREDIT';
export type OpenItemStatus = 'OPEN' | 'PARTIALLY_SETTLED' | 'SETTLED';

/** Receivable (debit) or payable (credit) document of a party. */
export interface OpenItem {
  id: number;
  partyCode: string;
  direction: ItemDirection;
  documentType: string;
  documentNo: string;
  documentDate: string;
  dueDate: string;
  currency: string;
  amount: number;
  settledAmount: number;
  outstanding: number;
  status: OpenItemStatus;
  sourceModule: string;
  journalBatchNo?: string;
  narration?: string;
}

export interface AgeingRow {
  partyCode: string;
  amounts: number[];
  total: number;
}

export interface Ageing {
  asOf: string;
  buckets: string[];
  rows: AgeingRow[];
}

export const subledgerApi = {
  partyItems: (companyId: number, partyCode: string) =>
    api.get<OpenItem[]>(`/subledger/items${toQuery({ companyId, partyCode })}`),
  ageing: (companyId: number, asOf: string, partyCode?: string) =>
    api.get<Ageing>(`/subledger/ageing${toQuery({ companyId, asOf, partyCode })}`),
  allocate: (id: number, date: string) =>
    api.post<OpenItem>(`/subledger/items/${id}/allocate${toQuery({ date })}`),
};

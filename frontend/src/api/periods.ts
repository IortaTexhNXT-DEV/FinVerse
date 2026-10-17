import { api, toQuery } from './client';

export type PeriodStatus = 'FUTURE' | 'OPEN' | 'CLOSING' | 'CLOSED' | 'REOPENED';

export interface FiscalYear {
  id: number;
  companyId: number;
  yearCode: number;
  startDate: string;
  endDate: string;
  status: 'OPEN' | 'CLOSED';
  closedBy?: string;
  closedAt?: string;
}

export interface Period {
  id: number;
  fiscalYearId: number;
  periodNo: number;
  name: string;
  startDate: string;
  endDate: string;
  status: PeriodStatus;
  statusChangedBy?: string;
  statusChangedAt?: string;
  statusReason?: string;
}

export const periodApi = {
  years: (companyId: number) => api.get<FiscalYear[]>(`/periods/years${toQuery({ companyId })}`),
  createYear: (companyId: number, yearCode: number) =>
    api.post<FiscalYear>('/periods/years', { companyId, yearCode }),
  periods: (yearId: number) => api.get<Period[]>(`/periods/years/${yearId}/periods`),
  open: (id: number) => api.post<Period>(`/periods/${id}/open`),
  startClosing: (id: number) => api.post<Period>(`/periods/${id}/start-closing`),
  close: (id: number) => api.post<Period>(`/periods/${id}/close`),
  reopen: (id: number, reason: string) => api.post<Period>(`/periods/${id}/reopen`, { reason }),
};

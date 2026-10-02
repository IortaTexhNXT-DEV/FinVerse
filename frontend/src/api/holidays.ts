import { api, toQuery } from './client';

export interface Holiday {
  id: number;
  companyId: number;
  branchId?: number;
  holidayDate: string;
  description: string;
}

export interface HolidayInput {
  companyId: number;
  branchId?: number;
  holidayDate: string;
  description: string;
}

export const holidaysApi = {
  list: (companyId: number, year: number) =>
    api.get<Holiday[]>(`/organization/holidays${toQuery({ companyId, year })}`),
  add: (body: HolidayInput) => api.post<Holiday>('/organization/holidays', body),
};

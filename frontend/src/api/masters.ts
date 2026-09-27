import { api, toQuery } from './client';

export interface Currency {
  code: string;
  name: string;
  symbol?: string;
  decimalPlaces: number;
  active: boolean;
}

export type RateType = 'SPOT' | 'CLOSING' | 'AVERAGE' | 'BUDGET' | 'BOOK';

export interface ExchangeRate {
  id: number;
  currencyCode: string;
  rateType: RateType;
  effectiveDate: string;
  rate: number;
  createdBy: string;
}

export type DimensionType = 'COST_CENTER' | 'PROFIT_CENTER' | 'DEPARTMENT' | 'BUSINESS_LINE';

export interface DimensionValue {
  id: number;
  companyId: number;
  type: DimensionType;
  code: string;
  name: string;
  active: boolean;
}

export const mastersApi = {
  currencies: () => api.get<Currency[]>('/currencies'),
  rates: (from: string, to: string) =>
    api.get<ExchangeRate[]>(`/currencies/rates${toQuery({ from, to })}`),
  saveRate: (body: Omit<ExchangeRate, 'id' | 'createdBy'>) =>
    api.post<ExchangeRate>('/currencies/rates', body),
  dimensions: (companyId: number, type: DimensionType) =>
    api.get<DimensionValue[]>(`/dimensions${toQuery({ companyId, type })}`),
  createDimension: (body: Omit<DimensionValue, 'id' | 'active'>) =>
    api.post<DimensionValue>('/dimensions', body),
  setDimensionActive: (id: number, active: boolean) =>
    api.post<DimensionValue>(`/dimensions/${id}/${active ? 'activate' : 'deactivate'}`),
};

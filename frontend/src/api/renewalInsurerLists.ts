import { api, toQuery } from './client';
import type { Approval } from './renewalSetupTypes';

/** A risk code an insurer renews (insurer renewable list of Renewal Setup). */
export interface RenewableRiskView {
  id: number;
  insurerCode: string;
  riskCode: string;
  remarks: string | null;
  effectiveFrom: string;
  effectiveTo: string | null;
  approval: Approval;
}

/** Values of a row of an insurer renewable list. */
export interface RenewableRiskData {
  insurerCode: string;
  riskCode: string;
  remarks?: string;
  effectiveFrom: string;
  effectiveTo?: string;
}

const BASE = '/renewal/setup/insurer-renewable';

/** Insurer renewable lists of Renewal Setup (FR-RN-020, 112). */
export const insurerListsApi = {
  list: (companyId: number) => api.get<RenewableRiskView[]>(`${BASE}${toQuery({ companyId })}`),
  save: (companyId: number, id: number | null, data: RenewableRiskData) =>
    id === null
      ? api.post<RenewableRiskView>(`${BASE}${toQuery({ companyId })}`, data)
      : api.put<RenewableRiskView>(`${BASE}/${id}${toQuery({ companyId })}`, data),
  action: (companyId: number, id: number, action: 'AUTHORIZE' | 'DEACTIVATE') =>
    api.post<RenewableRiskView>(`${BASE}/${id}/${action}${toQuery({ companyId })}`),
};

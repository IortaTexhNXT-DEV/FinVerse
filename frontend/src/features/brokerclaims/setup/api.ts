import { api } from '@/api/client';
import type { RecordStatus } from '@/api/types';

/** Claims Setup (BRCLM.010/012/014; FR-CM-040/041/043): /api/v1/broker-claims/setup. */

export const STATUS_LIST = 'BCL_CLAIM_STATUS';
export const SETTLEMENT_LIST = 'BCL_SETTLEMENT_TYPE';

export interface ValueAttributes {
  code: string;
  label: string;
  valueStatus: RecordStatus;
  attributes: Record<string, string>;
  pending: Record<string, string | null>;
  pendingBy?: string;
}

export interface StatusAttributesInput {
  phase: string;
  waitingOn: string;
  followUpDays: string;
  awaitingPremiumRemittance: boolean;
}

export interface SettlementAttributesInput {
  outcome: string;
  closesClaim: boolean;
  requiresSettlementAmount: boolean;
}

export interface MatrixRow {
  id: number;
  statusCode: string;
  statusLabel: string;
  roleCode: string;
  unitCode?: string;
  status: RecordStatus;
  maker?: string;
  authorizedBy?: string;
}

export interface ClaimsRole {
  code: string;
  name: string;
}

export interface Handler {
  id: number;
  username: string;
  unitCode: string;
  team?: string;
  active: boolean;
}

export interface ClaimsList {
  code: string;
  name: string;
  description?: string;
  maintainable: boolean;
}

const SETUP = '/broker-claims/setup';

export const claimsSetupApi = {
  attributes: (list: string) => api.get<ValueAttributes[]>(`${SETUP}/attributes/${list}`),
  proposeStatus: (code: string, input: StatusAttributesInput) =>
    api.put<ValueAttributes>(`${SETUP}/statuses/${encodeURIComponent(code)}/attributes`, input),
  proposeSettlement: (code: string, input: SettlementAttributesInput) =>
    api.put<ValueAttributes>(
      `${SETUP}/settlement-types/${encodeURIComponent(code)}/attributes`,
      input,
    ),
  authorizeAttributes: (list: string, code: string) =>
    api.post<ValueAttributes>(`${SETUP}/attributes/${list}/${encodeURIComponent(code)}/authorize`),
  rejectAttributes: (list: string, code: string) =>
    api.post<ValueAttributes>(`${SETUP}/attributes/${list}/${encodeURIComponent(code)}/reject`),
  matrix: () => api.get<MatrixRow[]>(`${SETUP}/matrix`),
  roles: () => api.get<ClaimsRole[]>(`${SETUP}/matrix/roles`),
  addRow: (statusCode: string, roleCode: string, unitCode: string) =>
    api.post<MatrixRow>(`${SETUP}/matrix`, { statusCode, roleCode, unitCode }),
  authorizeRow: (id: number) => api.post<MatrixRow>(`${SETUP}/matrix/${id}/authorize`),
  deactivateRow: (id: number) => api.post<MatrixRow>(`${SETUP}/matrix/${id}/deactivate`),
  handlers: () => api.get<Handler[]>(`${SETUP}/handlers`),
  users: () => api.get<string[]>(`${SETUP}/users`),
  saveHandler: (input: Omit<Handler, 'id'>) => api.put<Handler>(`${SETUP}/handlers`, input),
  lists: () => api.get<ClaimsList[]>(`${SETUP}/lists`),
};

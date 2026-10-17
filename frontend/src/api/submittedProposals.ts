import { api, toQuery } from './client';
import type { PageResponse } from './types';

/** What is proposed for a record: insurer, nominated rate and premium. */
export interface ProposedLine {
  policyId: number;
  sbmNo: string;
  assuredName: string;
  segment: string;
  vehicleType: string | null;
  sumInsured: number | null;
  defaultInsurer: string | null;
  insurerCode: string | null;
  nominatedRate: number | null;
  premium: number | null;
  problem: string | null;
}

/** A record to generate, with the insurer and rate to apply. */
export interface ProposalLine {
  policyId: number;
  insurerCode?: string;
  rate?: number;
  reason?: string;
}

export type ProposalStatus = 'FOR_REVIEW' | 'RELEASED' | 'RETURNED' | 'SUPERSEDED';

/** A renewal proposal. */
export interface ProposalView {
  id: number;
  policyId: number;
  sbmNo: string;
  assuredName: string;
  versionNo: number;
  defaultInsurer: string | null;
  insurerCode: string;
  nominatedRate: number | null;
  appliedRate: number;
  rateReason: string | null;
  sumInsured: number | null;
  premium: number | null;
  status: ProposalStatus;
  returnReason: string | null;
  maker: string;
  releasedBy: string | null;
}

/** A batch of proposals. */
export interface ProposalBatch {
  id: number;
  batchNo: string;
  createdBy: string;
  createdAt: string;
  proposals: ProposalView[];
}

/** A nominated package rate. */
export interface NominatedRate {
  id: number;
  segment: string;
  vehicleType: string | null;
  insurerCode: string;
  rate: number;
  effectiveFrom: string;
  effectiveTo: string | null;
  recordStatus: string;
  maker: string | null;
}

/** Values of a nominated rate. */
export interface NominatedRateRow {
  segment: string;
  vehicleType?: string;
  insurerCode: string;
  rate: number;
  effectiveFrom: string;
  effectiveTo?: string;
}

const B = '/submitted/proposals';

/** Renewal proposals and nominated rates of Submitted Policies. */
export const proposalsApi = {
  preview: (companyId: number, policyIds: number[]) =>
    api.post<ProposedLine[]>(`${B}/preview`, { companyId, policyIds }),
  generate: (companyId: number, lines: ProposalLine[]) =>
    api.post<ProposalBatch>(B, { companyId, lines }),
  batches: (companyId: number, page: number) =>
    api.get<PageResponse<ProposalBatch>>(`${B}/batches${toQuery({ companyId, page, size: 20 })}`),
  assignInsurer: (proposalIds: number[], insurerCode: string) =>
    api.post<ProposalView[]>(`${B}/assign-insurer`, { proposalIds, insurerCode }),
  release: (id: number) => api.post<ProposalView>(`${B}/${String(id)}/release`),
  returnProposal: (id: number, reason: string) =>
    api.post<ProposalView>(`${B}/${String(id)}/return`, { reason }),
  document: (id: number) => api.getFile(`${B}/${String(id)}/document`),
  rates: (companyId: number) => api.get<NominatedRate[]>(`${B}/rates${toQuery({ companyId })}`),
  saveRate: (companyId: number, id: number | null, row: NominatedRateRow) =>
    id === null
      ? api.post<NominatedRate>(`${B}/rates${toQuery({ companyId })}`, row)
      : api.put<NominatedRate>(`${B}/rates/${String(id)}${toQuery({ companyId })}`, row),
  rateAction: (companyId: number, id: number, action: 'authorize' | 'deactivate') =>
    api.post<NominatedRate>(`${B}/rates/${String(id)}/${action}${toQuery({ companyId })}`),
};

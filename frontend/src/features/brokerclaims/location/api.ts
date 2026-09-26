import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';

/**
 * Claim locations and insurer location references API (BRCLM.037/042), under
 * /api/v1/broker-claims.
 */

export interface LocationRef {
  id: number;
  arn: string;
  itemNo: number;
  locationKey?: string;
  insurerCode: string;
  reference: string;
  effectiveFrom: string;
  effectiveTo?: string;
  createdBy: string;
  createdAt: string;
}

export interface ClaimLocation {
  itemNo: number;
  address?: string;
  city?: string;
  province?: string;
  locationKey?: string;
  description?: string;
  references: LocationRef[];
}

export interface LocationPick {
  itemNo: number;
  description?: string;
}

export interface NewLocationRef {
  companyId: number;
  arn: string;
  itemNo: number;
  insurerCode: string;
  reference: string;
  effectiveFrom?: string;
}

const base = (claimId: number) => `/broker-claims/${claimId}/locations`;
const REFS = '/broker-claims/location-refs';

export const locationApi = {
  ofClaim: (companyId: number, claimId: number) =>
    api.get<ClaimLocation[]>(`${base(claimId)}${toQuery({ companyId })}`),
  link: (companyId: number, claimId: number, locations: LocationPick[]) =>
    api.post<ClaimLocation[]>(`${base(claimId)}${toQuery({ companyId })}`, { locations }),
  describe: (companyId: number, claimId: number, itemNo: number, description: string) =>
    api.put<ClaimLocation[]>(`${base(claimId)}/${itemNo}${toQuery({ companyId })}`, {
      description,
    }),
  remove: (companyId: number, claimId: number, itemNo: number) =>
    api.delete(`${base(claimId)}/${itemNo}${toQuery({ companyId })}`),
  refs: (companyId: number, q: string, page: number) =>
    api.get<PageResponse<LocationRef>>(`${REFS}${toQuery({ companyId, q, page, size: 20 })}`),
  refsOfCover: (companyId: number, arn: string) =>
    api.get<LocationRef[]>(`${REFS}/by-cover/${encodeURIComponent(arn)}${toQuery({ companyId })}`),
  maintain: (input: NewLocationRef) => api.post<LocationRef>(REFS, input),
};

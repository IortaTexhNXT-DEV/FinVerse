import { api, toQuery } from '@/api/client';
import type { LocationRef } from '../location/api';

/**
 * Cover Lookup API (BRCLM.001/002/003/039/042): read-only covers of the company, and the cover card
 * of Record Claim, under /api/v1/broker-claims/covers.
 */

export type SearchBy = 'ARN' | 'POLICY_NO' | 'ASSURED';

export type PremiumStatus = 'PAID' | 'UNPAID' | 'PARTIALLY_PAID' | 'DIRECT_PAYMENT' | 'NO_INVOICE';

export interface CoverHit {
  arn: string;
  accountId: number;
  clientCode: string;
  assuredName: string;
  productCode: string;
  lineCode?: string;
  insurerCode?: string;
  periodFrom?: string;
  periodTo?: string;
  status: string;
  currency?: string;
  policyNumbers: string[];
}

export interface PolicyYear {
  year: number;
  policyNo?: string;
  from?: string;
  to?: string;
}

export interface CoverItem {
  itemNo: number;
  kind: string;
  label: string;
  city?: string;
  province?: string;
  locationKey?: string;
  sumInsured?: number;
}

export interface Endorsement {
  endorsementNo: string;
  type: string;
  effectiveDate: string;
  policyYear: number;
  invoiceNo?: string;
  description?: string;
}

export interface CoverInvoice {
  invoiceNo: string;
  kind: string;
  policyYear: number;
  endorsementNo?: string;
  currency: string;
  grossPremium: number;
  balance: number;
  paymentStatus: string;
  remittanceStatus: string;
  directPayment: boolean;
  cancelled: boolean;
}

export interface UnpaidInvoice {
  invoiceNo: string;
  kind: string;
  paymentStatus: string;
  balance: number;
  currency: string;
}

export interface PremiumCheck {
  status: PremiumStatus;
  blocking: boolean;
  unpaid: UnpaidInvoice[];
}

export interface CoverClaim {
  id: number;
  claimNo: string;
  policyYear: number;
  lossDate: string;
  reportedDate: string;
  statusCode?: string;
  phase: string;
  premiumStatus?: PremiumStatus;
}

export interface Share {
  insurerCode: string;
  sharePct: number;
  lead: boolean;
}

export interface CoverDetail {
  header: CoverHit;
  termYears: number;
  sumInsured?: number;
  salesTeam?: string;
  accountOfficer?: string;
  paymentArrangement?: string;
  years: PolicyYear[];
  items: CoverItem[];
  endorsements: Endorsement[];
  invoices: CoverInvoice[];
  claims: CoverClaim[];
  locationRefs: LocationRef[];
}

export interface ClaimDraft {
  header: CoverHit;
  years: PolicyYear[];
  policyYear: number;
  policyNo?: string;
  versionNo: number;
  versionLabel: string;
  periodFrom?: string;
  periodTo?: string;
  sumInsured?: number;
  currency: string;
  salesTeam?: string;
  accountOfficer?: string;
  invoicingBranchId?: number;
  premium: PremiumCheck;
  locations: CoverItem[];
  insurers: Share[];
  lossInsidePeriod: boolean;
}

const COVERS = '/broker-claims/covers';

export const coverApi = {
  search: (companyId: number, by: SearchBy, q: string) =>
    api.get<CoverHit[]>(`${COVERS}${toQuery({ companyId, by, q })}`),
  cover: (companyId: number, arn: string) =>
    api.get<CoverDetail>(`${COVERS}/${encodeURIComponent(arn)}${toQuery({ companyId })}`),
  draft: (companyId: number, arn: string, policyYear: number, lossDate?: string) =>
    api.get<ClaimDraft>(
      `${COVERS}/${encodeURIComponent(arn)}/claim-draft${toQuery({ companyId, policyYear, lossDate })}`,
    ),
};

import { api, toQuery } from '@/api/client';
import type { CoverClaim, PremiumCheck, PremiumStatus } from '../cover/api';
import type { NewInsurerLine } from '../insurer/api';
import type { LocationPick } from '../location/api';

/**
 * Claim record API (BRCLM.001/003/004/006/016/036/039): record a claim, read it, change its loss
 * data, correct the reported date, override the claimant, refresh the cover data, use the latest
 * cover version, re-check the premium and generate the authorization code.
 */

/** Entity type of a claim for attachments, workflow and e-mails. */
export const CLAIM_ENTITY = 'BrokerClaim';

export type ClaimSource = 'BDOI_NOTICE' | 'INSURER_REPORTED' | 'MIGRATED';

export interface LossInput {
  lossDate: string;
  reportedDate: string;
  lossNature: string;
  claimType: string;
  lossDescription: string;
  lossPlace?: string;
  catastropheCode?: string;
  catastropheEvent?: string;
  claimAmount?: number;
  deductible?: number;
  initialReserve?: number;
}

export interface RecordClaimInput {
  companyId: number;
  arn: string;
  policyYear: number;
  source: ClaimSource;
  initialStatus?: string;
  loss: LossInput;
  locations: LocationPick[];
  insurers: NewInsurerLine[];
  confirmOutsidePeriod: boolean;
  confirmReuse: boolean;
}

export interface ClaimCover {
  arn: string;
  accountId?: number;
  policyYear: number;
  policyNo?: string;
  versionNo?: number;
  versionLabel: string;
  versionAt?: string;
  latestVersionNo: number;
  productCode?: string;
  lineCode?: string;
  clientCode?: string;
  assuredName?: string;
  leadInsurerCode?: string;
  periodFrom?: string;
  periodTo?: string;
  sumInsured?: number;
  salesTeam?: string;
  salesDepartment?: string;
  accountOfficer?: string;
  invoicingBranchId?: number;
  currency: string;
}

export interface ClaimPremium {
  recordedStatus?: PremiumStatus;
  checkedAt?: string;
  live: PremiumCheck;
  authorizationCode?: string;
  authorizedBy?: string;
  authorizedAt?: string;
  evidenceAttachmentId?: number;
  dpPolicy: string;
  canAuthorize: boolean;
  unremittedInvoices: string[];
}

export interface ClaimLoss {
  lossDate: string;
  reportedDate: string;
  lossNature?: string;
  lossNatureLabel?: string;
  claimType?: string;
  claimTypeLabel?: string;
  lossDescription?: string;
  lossPlace?: string;
  catastropheCode?: string;
  catastropheLabel?: string;
  catastropheEvent?: string;
  claimAmount?: number;
  deductible?: number;
  initialReserve?: number;
  claimantName?: string;
  claimantOverridden: boolean;
  claimantReason?: string;
}

export interface ClaimProgress {
  statusCode?: string;
  statusLabel?: string;
  statusSince?: string;
  phase: string;
  closureKind?: string;
  settlementTypeCode?: string;
  settlementTypeLabel?: string;
  settlementAmount?: number;
  dateSettled?: string;
  closedOn?: string;
  adjusterCode?: string;
  adjusterLabel?: string;
  nextFollowUpDate?: string;
  followUpOverridden: boolean;
  nextActionPlan?: string;
}

export interface ClaimFlags {
  unpaidPremium: boolean;
  awaitingPremiumRemittance: boolean;
  newerCoverVersion: boolean;
  multiLocation: boolean;
  multiInsurer: boolean;
  catastrophe: boolean;
  claimantOverridden: boolean;
}

export interface Claim {
  id: number;
  claimNo: string;
  source: ClaimSource;
  handler: string;
  unitCode?: string;
  unitLabel?: string;
  branchId?: number;
  cover: ClaimCover;
  premium: ClaimPremium;
  loss: ClaimLoss;
  progress: ClaimProgress;
  flags: ClaimFlags;
  locationCount: number;
  insurerCount: number;
  totalReserve: number;
  createdBy: string;
  createdAt: string;
}

const CLAIMS = '/broker-claims';
const one = (companyId: number, id: number, action = '') =>
  `${CLAIMS}/${id}${action}${toQuery({ companyId })}`;

export const claimApi = {
  record: (input: RecordClaimInput) => api.post<Claim>(CLAIMS, input),
  get: (companyId: number, id: number) => api.get<Claim>(one(companyId, id)),
  search: (companyId: number, q: string) =>
    api.get<CoverClaim[]>(`${CLAIMS}/search${toQuery({ companyId, q })}`),
  amendLoss: (companyId: number, id: number, loss: LossInput) =>
    api.put<Claim>(one(companyId, id, '/loss'), loss),
  correctReportedDate: (
    companyId: number,
    id: number,
    body: { reportedDate: string; reason: string; remark?: string },
  ) => api.post<Claim>(one(companyId, id, '/reported-date'), body),
  overrideClaimant: (companyId: number, id: number, claimantName: string, reason: string) =>
    api.post<Claim>(one(companyId, id, '/claimant'), { claimantName, reason }),
  refreshCover: (companyId: number, id: number) =>
    api.post<string[]>(one(companyId, id, '/refresh-cover')),
  useLatestVersion: (companyId: number, id: number) =>
    api.post<Claim>(one(companyId, id, '/latest-version')),
  premiumCheck: (companyId: number, id: number) =>
    api.post<PremiumCheck>(one(companyId, id, '/premium-check')),
  authorize: (companyId: number, id: number, evidenceAttachmentId?: number) =>
    api.post<Claim>(one(companyId, id, '/authorize'), { evidenceAttachmentId }),
};

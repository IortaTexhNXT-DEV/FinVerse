import type { PeriodBasis, RiskItemKind } from './catalog';
import { api, toQuery } from './client';
import type { PageResponse } from './types';

/** New Business accounts (account module): drafts, submission, tags and look-ups. */

export type AccountStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'RETURNED_TO_MARKETING'
  | 'AWAITING_PAYMENT'
  | 'READY_FOR_PLACEMENT'
  | 'PLACED'
  | 'RETURNED_BY_INSURER'
  | 'PLACEMENT_CANCELLED'
  | 'POLICY_ISSUED'
  | 'BOOKED'
  | 'CANCELLED'
  | 'VOIDED';

export const ACCOUNT_STATUSES: AccountStatus[] = [
  'DRAFT',
  'SUBMITTED',
  'RETURNED_TO_MARKETING',
  'AWAITING_PAYMENT',
  'READY_FOR_PLACEMENT',
  'PLACED',
  'RETURNED_BY_INSURER',
  'PLACEMENT_CANCELLED',
  'POLICY_ISSUED',
  'BOOKED',
  'CANCELLED',
  'VOIDED',
];

export type PaymentArrangement = 'VIA_BDOI' | 'DIRECT_TO_INSURER';
export type PaymentStatus = 'UNPAID' | 'PAID' | 'CLIENT_CONFIRMED' | 'DIRECT';
export type HoldCoverStatus = 'REQUESTED' | 'CONFIRMED' | 'DECLINED' | 'EXPIRED';

export interface Vehicle {
  plateNo?: string;
  conductionSticker?: string;
  engineNo?: string;
  chassisNo?: string;
  make?: string;
  model?: string;
  yearModel?: number;
  bodyType?: string;
  colour?: string;
  seatingCapacity?: number;
}

export interface InsuredItem {
  description: string;
  sumInsured: number;
}

export interface Location {
  address?: string;
  city?: string;
  province?: string;
  occupancy?: string;
  constructionClass?: string;
  insuredItems?: InsuredItem[];
}

export interface Person {
  name?: string;
  birthDate?: string;
  relationship?: string;
}

export interface ItemInput {
  description?: string;
  sumInsured?: number;
  rate?: number;
  biLimit?: number;
  pdLimit?: number;
  vehicle?: Vehicle;
  location?: Location;
  person?: Person;
}

export interface AccountItem extends ItemInput {
  id: number;
  itemNo: number;
  kind: RiskItemKind;
  label: string;
  premium?: number;
}

export interface AccountInput {
  companyId: number;
  clientId: number;
  productCode: string;
  marketSegment?: string;
  sourceChannel?: string;
  insurerCode?: string;
  insurerBranch?: string;
  periodFrom?: string;
  periodTo?: string;
  multiYear: boolean;
  termYears: number;
  currency?: string;
  paymentArrangement?: PaymentArrangement;
  mortgageeBank?: string;
  loanApplicationNo?: string;
  pnNumbers?: string[];
  contactName?: string;
  contactEmail?: string;
  contactMobile?: string;
  contactAddress?: string;
  items: ItemInput[];
  ratingBasis?: PeriodBasis;
  commissionRate?: number;
  ffyStart?: string;
}

export interface AccountPremium {
  ratingBasis?: PeriodBasis;
  netPremium?: number | null;
  dst?: number | null;
  premiumTax?: number | null;
  vat?: number | null;
  fst?: number | null;
  lgt?: number | null;
  totalCharges?: number | null;
  grossPremium?: number | null;
  commissionRate?: number | null;
  commission?: number | null;
  vatOnCommission?: number | null;
  minimumApplied: boolean;
}

export interface FreeFirstYear {
  active: boolean;
  start?: string;
  end?: string;
  cancelledAt?: string;
  cancelledBy?: string;
  cancelReason?: string;
}

export interface AccountContact {
  name?: string;
  email?: string;
  mobile?: string;
  address?: string;
}

export interface SalesStamp {
  region?: string;
  department?: string;
  team?: string;
  accountOfficer?: string;
  costCenter?: string;
}

export interface TsuClearance {
  required: boolean;
  rule?: string;
  clearedBy?: string;
  clearedAt?: string;
}

export interface Lifecycle {
  paymentStatus?: PaymentStatus;
  paymentSource?: string;
  paymentConfirmedAt?: string;
  placementSlipRef?: string;
  placedAt?: string;
  insurerRef?: string;
  holdCoverStatus?: HoldCoverStatus;
  holdCoverRef?: string;
  holdCoverDate?: string;
  policyIssueDate?: string;
  epolicyReceived: boolean;
  bookingRef?: string;
  bookedAt?: string;
  incentiveFlag: boolean;
  cancelledAt?: string;
  cancellationReason?: string;
}

export interface Account {
  id: number;
  companyId: number;
  arn: string;
  quotationRef?: string;
  proposalRef?: string;
  clientId: number;
  clientCode?: string;
  clientName: string;
  productCode: string;
  lineCode: string;
  coverTypeCode?: string;
  marketSegment?: string;
  sourceChannel?: string;
  insurerCode?: string;
  insurerBranch?: string;
  periodFrom?: string;
  periodTo?: string;
  multiYear: boolean;
  termYears: number;
  currency: string;
  totalSumInsured?: number;
  premium: AccountPremium;
  paymentArrangement?: PaymentArrangement;
  directPayment: boolean;
  directPaymentTaggedBy?: string;
  mortgageeBank?: string;
  loanApplicationNo?: string;
  pnNumbers: string[];
  freeFirstYear: FreeFirstYear;
  contact: AccountContact;
  sales: SalesStamp;
  status: AccountStatus;
  tsu: TsuClearance;
  directBooking: boolean;
  lifecycle: Lifecycle;
  policyNumbers: string[];
  items: AccountItem[];
  createdBy: string;
  createdAt: string;
  /** New Business or Renewal (BRNB.097, BRID-022.01; shared work item BT0). */
  businessType: BusinessType;
  /** What a renewal renews: expiring ARN, SBM number or legacy reference. */
  renewalOfRef?: string;
  /** How the account was created. */
  origin: AccountOrigin;
}

/** Business type of an account (shared work item BT0). */
export type BusinessType = 'NEW_BUSINESS' | 'RENEWAL';

/** How an account was created (shared work item BT0). */
export type AccountOrigin =
  'QUOTATION' | 'PROPOSAL' | 'DIRECT' | 'SUBMITTED_POLICY' | 'EMPLOYEE_BENEFITS' | 'RENEWAL';

export interface AccountSummary {
  id: number;
  arn: string;
  clientCode?: string;
  clientName: string;
  productCode: string;
  lineCode: string;
  insurerCode?: string;
  status: AccountStatus;
  periodFrom?: string;
  periodTo?: string;
  totalSumInsured?: number;
  grossPremium?: number;
  currency: string;
  ffy: boolean;
  ffyStart?: string;
  ffyEnd?: string;
  paymentArrangement?: PaymentArrangement;
  directPayment: boolean;
  accountOfficer?: string;
  createdAt: string;
  businessType: BusinessType;
}

export interface DuplicateFinding {
  itemNo: number;
  field: string;
  value: string;
  existingArn: string;
  existingProduct: string;
}

export interface AccountCheck {
  fieldErrors: Record<string, string>;
  missingDocuments: string[];
  duplicates: DuplicateFinding[];
  premiumRated: boolean;
  tsuRequired: boolean;
  tsuRule?: string;
  tsuReason?: string;
  tsuCleared: boolean;
  readyToSubmit: boolean;
}

export interface AccountCriteria {
  text?: string;
  pn?: string;
  vehicle?: string;
  location?: string;
  product?: string;
  line?: string;
  insurer?: string;
  ffy?: boolean;
  directPayment?: boolean;
  officer?: string;
  mine?: boolean;
  includeVoided?: boolean;
  status?: AccountStatus[];
  periodFrom?: string;
  periodTo?: string;
  businessType?: string;
}

/** Workflow entity type of accounts (NB_ACCOUNT work cases, attachments, e-mails). */
export const ACCOUNT_ENTITY = 'Account';

const base = '/accounts';

export const accountsApi = {
  search: (companyId: number, criteria: AccountCriteria, page = 0, size = 20) =>
    api.get<PageResponse<AccountSummary>>(
      `${base}${toQuery({
        companyId,
        ...criteria,
        status: criteria.status?.length ? criteria.status.join(',') : undefined,
        page,
        size,
      })}`,
    ),
  get: (id: number) => api.get<Account>(`${base}/${id}`),
  byArn: (arn: string) => api.get<Account>(`${base}/by-arn/${encodeURIComponent(arn)}`),
  check: (id: number) => api.get<AccountCheck>(`${base}/${id}/check`),
  create: (input: AccountInput) => api.post<Account>(base, input),
  update: (id: number, input: AccountInput) => api.put<Account>(`${base}/${id}`, input),
  submit: (id: number, comment?: string) => api.post<Account>(`${base}/${id}/submit`, { comment }),
  resubmit: (id: number, comment?: string) =>
    api.post<Account>(`${base}/${id}/resubmit`, { comment }),
  validate: (id: number, comment?: string) =>
    api.post<Account>(`${base}/${id}/validate`, { comment }),
  directBooking: (id: number, comment?: string) =>
    api.post<Account>(`${base}/${id}/direct-booking`, { comment }),
  clearTsu: (id: number, comment?: string) =>
    api.post<Account>(`${base}/${id}/tsu-clearance`, { comment }),
  tagFfy: (id: number, start: string) => api.put<Account>(`${base}/${id}/ffy`, { start }),
  cancelFfy: (id: number, reasonCode: string, comment?: string) =>
    api.post<Account>(`${base}/${id}/ffy/cancel`, { reasonCode, comment }),
  setPaymentArrangement: (id: number, arrangement: PaymentArrangement) =>
    api.put<Account>(`${base}/${id}/payment-arrangement`, { arrangement }),
};

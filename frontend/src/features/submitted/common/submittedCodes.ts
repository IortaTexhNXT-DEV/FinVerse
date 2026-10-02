import type { MasterlistTab } from '@/api/submitted';

/** Breadcrumb of every Submitted Policies screen (group Client & Policy). */
export const SUBMITTED_SECTION = 'Client & Policy · Submitted Policies';

/** Lists of values of Submitted Policies. */
export const SBM_LOV = {
  segment: 'SBM_SEGMENT',
  bucket: 'SBM_BUCKET',
  reason: 'SBM_REASON',
  nonRenewal: 'SBM_NON_RENEWAL_REASON',
  conversion: 'SBM_CONVERSION_STATUS',
  loanStatus: 'SBM_LOAN_STATUS',
  letterType: 'SBM_LETTER_TYPE',
  decline: 'SBM_DECLINE_REASON',
  finding: 'SBM_IAAF_FINDING',
  returnReason: 'SBM_RETURN_REASON',
} as const;

/** Masterlist tabs (design section 12). */
export const MASTERLIST_TABS: readonly { id: MasterlistTab; label: string }[] = [
  { id: 'ALL', label: 'All' },
  { id: 'FOR_VALIDATION', label: 'For Validation' },
  { id: 'CLASSIFIED', label: 'Classified' },
  { id: 'FOR_RENEWAL', label: 'For Renewal' },
  { id: 'MANUAL_DISPOSITION', label: 'Manual Disposition' },
  { id: 'NON_RENEWAL', label: 'Non-Renewal' },
  { id: 'FALLOUT', label: 'Fallout' },
];

/** Names of the record flags. */
export const FLAG_LABELS: Record<string, string> = {
  RENEWABLE: 'Renewable',
  NON_RENEWABLE: 'Non-Renewable',
  FFY: 'FFY',
  NO_TOUCH: 'No Touch',
  EMPLOYEE: 'Employee',
  MIGRATED: 'Migrated',
  INSURER_APPROVAL: 'Insurer Approval',
  FALLOUT: 'Fallout',
};

/** Names of the sources of the register. */
export const SOURCE_LABELS: Record<string, string> = {
  LFS_INSURANCE: 'LFS insurance report',
  HLS_INSURANCE: 'HLS daily insurance report',
  CIU: 'CIU report',
  SPI: 'SPI consolidated list',
  LOAN_BOOKING: 'Loan Booking Report',
  LAMD: 'LAMD loan report',
  IBG_LEASING_DOC: 'IBG / Leasing documents',
  IA_MASTERLIST: 'IA masterlist',
  MANUAL: 'Manual entry',
  MIGRATION: 'Excel masterlist migration',
};

/** Names of the processing steps. */
export const STEP_LABELS: Record<string, string> = {
  SANITATION: 'Sanitation',
  MATCHING: 'Matching',
  CLASSIFICATION: 'Classification',
  DISPOSITION: 'Disposition',
  LIMITS: 'Limits',
};

/** The link of a masterlist record. */
export const policyLink = (id: number) => `/submitted/policies/${String(id)}`;

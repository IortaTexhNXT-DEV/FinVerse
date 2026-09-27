import { humanize } from '@/utils/format';

/** Lists of values of Employee Benefits (V1030). */
export const EB_LOV = {
  benefitLine: 'EB_BENEFIT_LINE',
  team: 'EB_TEAM',
  processType: 'EB_PROCESS_TYPE',
  lostReason: 'EB_LOST_REASON',
  itemType: 'EB_TRACKED_ITEM_TYPE',
  documentType: 'DOCUMENT_TYPE',
} as const;

/** Labels of the fixed EB codes (not lists of values). */
const LABELS: Record<string, string> = {
  NEW_BUSINESS: 'New Business',
  RENEWAL: 'Renewal',
  EMPLOYER: 'Employer',
  VOLUNTARY: 'Voluntary',
  HR_HEAD: 'HR Head',
  HR_OFFICER: 'HR Officer',
  FINANCE: 'Finance',
  AO: 'Account Officer',
  EMAIL: 'E-mail',
  PHONE: 'Telephone',
  MEETING: 'Meeting',
  LETTER: 'Letter',
  PROCESSING: 'Processing',
  CLIENT: 'Client',
  INSURER: 'Insurer',
  SYSTEM: 'BIBS',
  BDOI: 'BDOI',
  RENEWAL_ADVICE: 'Renewal advice',
  PLACEMENT_REQUEST: 'Placement request',
};

/** The label of a fixed EB code. */
export function ebLabel(code: string | null | undefined): string {
  if (code === null || code === undefined || code === '') {
    return '';
  }
  return LABELS[code] ?? humanize(code);
}

/** Document types uploaded on the Documents tab (the BOR has its own tab). */
export const EB_UPLOAD_TYPES: readonly string[] = [
  'EB_CLIENT_FEEDBACK',
  'EB_TOR',
  'EB_MASTERLIST',
  'EB_MASTERLIST_UNNAMED',
  'EB_UTILIZATION',
  'EB_INDICATIVE_PROPOSAL',
  'EB_PROPOSAL',
  'EB_COMPARATIVE',
  'EB_FRANCHISE_FORM',
  'EB_CLIENT_CONFIRMATION',
  'EB_POLICY_FORM',
  'EB_DIRECT_BILLING',
  'EB_SOA',
  'EB_MEMBER_CHANGE',
  'EB_ISACOM_APPROVAL',
  'RENEWAL_ADVICE',
];

/** Where an uploaded document came from. */
export const EB_SOURCES: readonly { code: string; label: string }[] = [
  { code: 'CLIENT', label: 'Client' },
  { code: 'INSURER', label: 'Insurer' },
  { code: 'AO', label: 'Account Officer' },
  { code: 'PROCESSING', label: 'Processing' },
];

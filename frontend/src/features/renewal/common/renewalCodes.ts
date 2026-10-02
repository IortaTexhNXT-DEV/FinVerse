import type { RenewalTab } from '@/api/renewal';

/** Breadcrumb of every Renewal screen (group Client & Policy). */
export const RENEWAL_SECTION = 'Client & Policy · Renewal';

/** Lists of values of Renewal. */
export const RNW_LOV = {
  nonRenewalReason: 'RNW_NONRENEWAL_REASON',
  returnReason: 'RNW_RETURN_REASON',
  transferReason: 'RNW_TRANSFER_REASON',
  overrideReason: 'RNW_OVERRIDE_REASON',
  followupChannel: 'RNW_FOLLOWUP_CHANNEL',
  followupOutcome: 'RNW_FOLLOWUP_OUTCOME',
} as const;

/** Tab definition of a renewal list. */
export interface RenewalTabDef {
  id: RenewalTab;
  label: string;
}

/** Expiry List (BDOI UX design "Renewal"). */
export const EXPIRY_TABS: RenewalTabDef[] = [
  { id: 'UNASSIGNED', label: 'Unassigned Disposition' },
  { id: 'EXTRACTED', label: 'Extracted' },
  { id: 'FOR_RENEWAL', label: 'For Renewal' },
  { id: 'FOR_QUOTATION', label: 'For Quotation' },
  { id: 'FOR_PROPOSAL', label: 'For Proposal' },
  { id: 'NOT_FOR_RENEWAL', label: 'Not for Renewal' },
  { id: 'LOST_BUSINESS', label: 'Lost Business' },
  { id: 'EXCEPTIONS', label: 'Exceptions' },
  { id: 'TRANSFER_PENDING', label: 'Transfer Pending' },
  { id: 'ALL', label: 'All' },
];

/** Processing Worklist. */
export const PROCESSING_TABS: RenewalTabDef[] = [
  { id: 'FOR_PROCESSING', label: 'For Processing' },
  { id: 'IN_PROCESSING', label: 'In Processing' },
  { id: 'WITH_INSURER', label: 'With Insurer' },
  { id: 'INSURER_RESPONDED', label: 'Insurer Responded' },
  { id: 'RETURNED', label: 'Returned' },
];

/** Letters. */
export const LETTER_TABS: RenewalTabDef[] = [
  { id: 'RA_READY', label: 'RA Ready' },
  { id: 'RA_GENERATED', label: 'RA Generated' },
  { id: 'RA_SENT', label: 'RA Sent' },
  { id: 'LETTER_PENDING', label: 'NAL / NFR' },
  { id: 'NRNS', label: 'NRNS' },
];

/** The tab of a list from the URL, the first tab when unknown. */
export function tabOf(tabs: readonly RenewalTabDef[], value: string | null): RenewalTab {
  return tabs.find((t) => t.id === value)?.id ?? tabs[0]?.id ?? 'ALL';
}

/** Tone of the Classification pill: Clean green, Review yellow, Exception red. */
export function bucketTone(bucket: string | null): 'success' | 'warning' | 'danger' | 'neutral' {
  switch (bucket) {
    case 'CLEAN':
      return 'success';
    case 'REVIEW':
      return 'warning';
    case 'EXCEPTION':
      return 'danger';
    default:
      return 'neutral';
  }
}

const DISPOSITIONS: Record<string, string> = {
  FOR_RENEWAL: 'For Renewal',
  NOT_FOR_RENEWAL: 'Not for Renewal',
  FOR_QUOTATION: 'For Quotation',
  FOR_PROPOSAL: 'For Proposal',
  LOST_BUSINESS: 'Lost Business',
};

/** The name of a disposition. */
export function dispositionLabel(code: string | null | undefined): string {
  return code ? (DISPOSITIONS[code] ?? code) : '';
}

/** Disposition options. */
export const DISPOSITION_OPTIONS = Object.entries(DISPOSITIONS).map(([code, label]) => ({
  code,
  label,
}));

/** Summary of a batch action for the toast. */
export function outcomeSummary(
  outcome: { done: string[]; refused: Record<string, string> },
  verb: string,
): string {
  const refused = Object.keys(outcome.refused).length;
  const done = `${String(outcome.done.length)} ${verb}`;
  return refused === 0 ? done : `${done}, ${String(refused)} refused`;
}

/** The list and tab that show the renewals of a stage. */
export function stageLink(code: string): string {
  if (PROCESSING_TABS.some((t) => t.id === code)) {
    return `/renewal/processing?tab=${code}`;
  }
  if (LETTER_TABS.some((t) => t.id === code)) {
    return `/renewal/letters?tab=${code}`;
  }
  return EXPIRY_TABS.some((t) => t.id === code)
    ? `/renewal/expiry?tab=${code}`
    : '/renewal/expiry?tab=ALL';
}

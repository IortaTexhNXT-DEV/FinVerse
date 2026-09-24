import type { QuotationListItem, QuotationSearch, QuotationStatus } from '@/api/quotations';

/** Status tabs of the quotation work list (BDOI Quotation / Proposal design). */
export const QUOTATION_TABS = [
  { id: 'drafts', label: 'Drafts' },
  { id: 'review', label: 'For Review' },
  { id: 'sent', label: 'Sent to Client' },
  { id: 'accepted', label: 'Accepted' },
  { id: 'closed', label: 'Not Proceeded' },
] as const;

export type QuotationTab = (typeof QUOTATION_TABS)[number]['id'];

/** Statuses listed under each tab; approved quotations wait with the ones for review. */
export const TAB_STATUSES: Record<QuotationTab, QuotationStatus[]> = {
  drafts: ['DRAFT'],
  review: ['FOR_REVIEW', 'APPROVED'],
  sent: ['SENT_TO_CLIENT'],
  accepted: ['ACCEPTED', 'CONVERTED'],
  closed: ['NOT_PROCEEDED', 'VOIDED'],
};

export type QuickFilter = 'myDrafts' | 'forReview' | 'sent' | 'accepted' | 'expiring';

interface QuickDef {
  label: string;
  tab: QuotationTab;
  mine?: boolean;
  expiring?: boolean;
}

/** Quick filters: each selects a tab, some narrow it further. */
export const QUICK_FILTERS: Record<QuickFilter, QuickDef> = {
  myDrafts: { label: 'My drafts', tab: 'drafts', mine: true },
  forReview: { label: 'For review', tab: 'review' },
  sent: { label: 'Sent', tab: 'sent' },
  accepted: { label: 'Accepted', tab: 'accepted' },
  expiring: { label: 'Expiring', tab: 'sent', expiring: true },
};

/** The list criteria of a tab, a quick filter and the search box. */
export function criteriaOf(
  tab: QuotationTab,
  quick: QuickFilter | undefined,
  text: string,
): QuotationSearch {
  const q = quick === undefined ? undefined : QUICK_FILTERS[quick];
  const expiring = q?.expiring === true;
  return {
    text: text.trim() === '' ? undefined : text.trim(),
    status: expiring ? ['APPROVED', 'SENT_TO_CLIENT'] : TAB_STATUSES[tab],
    mine: q?.mine === true ? true : undefined,
    expiring: expiring ? true : undefined,
  };
}

/** Rows that may be sent in a batch (approved only). */
export function sendable(rows: QuotationListItem[], selected: ReadonlySet<number>) {
  return rows.filter((r) => selected.has(r.id) && r.status === 'APPROVED');
}

/** Days until a validity date (negative when past). */
export function daysLeft(validUntil: string, today: string): number {
  const ms = Date.parse(`${validUntil}T00:00:00Z`) - Date.parse(`${today}T00:00:00Z`);
  return Math.round(ms / 86_400_000);
}

/** Toggles one id in a selection. */
export function toggle(selected: ReadonlySet<number>, id: number): Set<number> {
  const next = new Set(selected);
  if (next.has(id)) {
    next.delete(id);
  } else {
    next.add(id);
  }
  return next;
}

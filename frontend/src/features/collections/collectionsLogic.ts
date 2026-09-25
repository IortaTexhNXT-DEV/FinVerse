import type { AgingCell, DispositionRule, ItemStatus } from './api';

/** Pure rules of the Collections screens (BRCLXN.001-029), tested with Vitest. */

export type WorklistTab = ItemStatus | 'ALL';

export const WORKLIST_TABS: readonly { id: WorklistTab; label: string }[] = [
  { id: 'OPEN', label: 'Open Accounts' },
  { id: 'CREDIT', label: 'Credit Balances' },
  { id: 'COMPLETED', label: 'Completed Collections' },
  { id: 'EXCLUDED_CANCELLED', label: 'Excluded' },
  { id: 'ALL', label: 'All' },
];

/** Filters of the worklist panel (segment, unit, UH, handler, AO, bracket ...). */
export interface WorklistFilters {
  segment?: string;
  salesUnit?: string;
  unitHead?: string;
  handler?: string;
  ao?: string;
  bracket?: string;
  category?: string;
  disposition?: string;
  amountFrom?: string;
  amountTo?: string;
  promise?: string;
  client?: string;
  mine?: boolean;
  unassigned?: boolean;
  escalated?: boolean;
}

export const FILTER_FIELDS: readonly { key: keyof WorklistFilters; label: string }[] = [
  { key: 'segment', label: 'Market Segment' },
  { key: 'salesUnit', label: 'Sales Unit' },
  { key: 'unitHead', label: 'Unit Head' },
  { key: 'handler', label: 'Handler' },
  { key: 'ao', label: 'Account Officer' },
  { key: 'bracket', label: 'Aging Bracket' },
  { key: 'category', label: 'Category (A/B/C)' },
  { key: 'disposition', label: 'Disposition' },
  { key: 'amountFrom', label: 'Outstanding From' },
  { key: 'amountTo', label: 'Outstanding To' },
  { key: 'promise', label: 'Promise Status' },
  { key: 'client', label: 'Client Code' },
];

/** The worklist query string of a tab, search text and filters. */
export function worklistQuery(tab: WorklistTab, text: string, filters: WorklistFilters): string {
  const params = new URLSearchParams();
  if (tab === 'ALL') {
    ['OPEN', 'CREDIT', 'COMPLETED', 'EXCLUDED_CANCELLED'].forEach((s) =>
      params.append('status', s),
    );
  } else {
    params.append('status', tab);
  }
  if (text.trim() !== '') {
    params.set('q', text.trim());
  }
  Object.entries(filters).forEach(([key, value]) => {
    if (typeof value === 'boolean') {
      if (value) {
        params.set(key, 'true');
      }
    } else if (typeof value === 'string' && value.trim() !== '') {
      params.set(key, value.trim());
    }
  });
  return params.toString();
}

/** Filters read from the URL of a home tile (mine, unassigned, disposition). */
export function filtersFromSearch(search: URLSearchParams): WorklistFilters {
  const out: WorklistFilters = {};
  if (search.get('mine') === 'true') {
    out.mine = true;
  }
  if (search.get('unassigned') === 'true') {
    out.unassigned = true;
  }
  const disposition = search.get('disposition');
  if (disposition !== null && disposition !== '') {
    out.disposition = disposition;
  }
  return out;
}

/** The tab of a home tile link, OPEN by default. */
export function tabFromSearch(search: URLSearchParams): WorklistTab {
  const tab = search.get('tab');
  return WORKLIST_TABS.some((t) => t.id === tab) ? (tab as WorklistTab) : 'OPEN';
}

/** Hand-off detail fields a disposition needs, by its Operations action. */
export interface DetailField {
  key: string;
  label: string;
  type: 'text' | 'date' | 'number';
  required: boolean;
  hint?: string;
}

const PICKUP_FIELDS: DetailField[] = [
  { key: 'pickupDate', label: 'Pick-up Date', type: 'date', required: true },
  { key: 'pickupAddress', label: 'Pick-up Address', type: 'text', required: true },
  { key: 'contactPerson', label: 'Contact Person', type: 'text', required: false },
  { key: 'amount', label: 'Check Amount', type: 'number', required: true },
  { key: 'checkNo', label: 'Check No.', type: 'text', required: false },
  { key: 'checkBank', label: 'Check Bank', type: 'text', required: false },
];

const CWT_FIELDS: DetailField[] = [
  {
    key: 'path',
    label: 'Path (CASH or CERTIFICATE)',
    type: 'text',
    required: true,
    hint: 'CERTIFICATE once the BIR 2307 is received; CASH when the client pays the 2% in cash',
  },
  { key: 'certificateNo', label: 'Certificate No.', type: 'text', required: false },
  { key: 'periodFrom', label: 'Certificate Period From', type: 'date', required: false },
  { key: 'periodTo', label: 'Certificate Period To', type: 'date', required: false },
  { key: 'amount', label: '2% Amount', type: 'number', required: false },
];

export function detailFields(opsAction: DispositionRule['opsAction'] | undefined): DetailField[] {
  if (opsAction === 'CHECK_PICKUP') {
    return PICKUP_FIELDS;
  }
  return opsAction === 'CWT2307_REVERSAL' ? CWT_FIELDS : [];
}

/** What a disposition hands to Operations, for the dialog's information panel. */
export function handoffText(rule: DispositionRule | undefined): string | undefined {
  switch (rule?.opsAction) {
    case 'CHECK_PICKUP':
      return 'Sends a check pick-up request to the Cashiering pick-up queue.';
    case 'CWT2307_REVERSAL':
      return 'Sends the BIR 2307 tag to Cashiering once the tag is an Operations action.';
    case 'DP_REVERSAL':
      return 'Places the account on the Commission direct payment list for PR reversal.';
    case 'CANCEL_REQUEST':
      return 'Raise the cancellation endorsement in Adjustment; no feed is sent.';
    default:
      return undefined;
  }
}

function pickupErrors(details: Record<string, string>, today: string): Record<string, string> {
  const date = details.pickupDate ?? '';
  return date !== '' && date < today
    ? { pickupDate: 'The pick-up date cannot be in the past' }
    : {};
}

function cwtErrors(details: Record<string, string>): Record<string, string> {
  const path = (details.path ?? '').trim().toUpperCase();
  if (path !== '' && path !== 'CASH' && path !== 'CERTIFICATE') {
    return { path: 'Enter CASH or CERTIFICATE' };
  }
  if (path === 'CERTIFICATE' && (details.certificateNo ?? '').trim() === '') {
    return { certificateNo: 'A certificate tag needs the certificate number' };
  }
  return {};
}

/** Field errors of a disposition before it is sent. */
export function dispositionErrors(
  code: string,
  rule: DispositionRule | undefined,
  details: Record<string, string>,
  today: string,
): Record<string, string> {
  if (code === '') {
    return { code: 'Choose a disposition' };
  }
  const errors: Record<string, string> = {};
  detailFields(rule?.opsAction)
    .filter((f) => f.required && (details[f.key] ?? '').trim() === '')
    .forEach((f) => {
      errors[f.key] = `${f.label} is required`;
    });
  if (rule?.opsAction === 'CHECK_PICKUP') {
    Object.assign(errors, pickupErrors(details, today));
  }
  if (rule?.opsAction === 'CWT2307_REVERSAL') {
    Object.assign(errors, cwtErrors(details));
  }
  return errors;
}

/** Only the filled detail values, trimmed (path upper-cased). */
export function cleanDetails(
  rule: DispositionRule | undefined,
  details: Record<string, string>,
): Record<string, string> {
  const out: Record<string, string> = {};
  detailFields(rule?.opsAction).forEach((f) => {
    const v = (details[f.key] ?? '').trim();
    if (v !== '') {
      out[f.key] = f.key === 'path' ? v.toUpperCase() : v;
    }
  });
  return out;
}

/** Lower bound of an aging bracket label (0-30, 121+) for ordering. */
export function bracketStart(label: string | undefined): number {
  const match = /^(\d+)/.exec(label ?? '');
  return match === null ? Number.MAX_SAFE_INTEGER : Number(match[1]);
}

/** One bar of the aging chart. */
export interface AgingBar {
  bracket: string;
  items: number;
  amount: number;
  share: number;
}

/** Segments present in the aging cells, sorted, with blank segments as "Unclassified". */
export function agingSegments(cells: AgingCell[]): string[] {
  return [...new Set(cells.map((c) => c.segment ?? 'Unclassified'))].sort((a, b) =>
    a.localeCompare(b),
  );
}

/** Open accounts and amounts per bracket for one segment (or all), in bracket order. */
export function agingBars(cells: AgingCell[], segment: string): AgingBar[] {
  const byBracket = new Map<string, { items: number; amount: number }>();
  cells
    .filter((c) => segment === '' || (c.segment ?? 'Unclassified') === segment)
    .forEach((c) => {
      const key = c.bracket ?? '—';
      const current = byBracket.get(key) ?? { items: 0, amount: 0 };
      byBracket.set(key, {
        items: current.items + c.items,
        amount: current.amount + c.netOutstanding,
      });
    });
  const max = Math.max(0, ...[...byBracket.values()].map((v) => v.amount));
  return [...byBracket.entries()]
    .sort(([a], [b]) => bracketStart(a) - bracketStart(b))
    .map(([bracket, v]) => ({
      bracket,
      items: v.items,
      amount: v.amount,
      share: max > 0 ? v.amount / max : 0,
    }));
}

/** Report titles of the Collections files. */
export const REPORT_TITLES: Record<string, string> = {
  'CLX-OUTSTANDING-PR': 'Outstanding PR List',
  'CLX-FULL-PRODUCTION': 'Full Production Report',
  'CLX-DP-FOR-REVERSAL': 'DP PR for Reversal',
  'CLX-PR2307-FOR-REVERSAL': 'PR 2307 for Reversal',
};

/** The title of a file's report, the code when unknown. */
export function reportTitle(code: string): string {
  return REPORT_TITLES[code] ?? code;
}

export type AccountTabId =
  'summary' | 'payments' | 'timeline' | 'policy' | 'dispositions' | 'history';

/** Tabs of the collection account (COLLECTIONS_DESIGN 11). */
export const ACCOUNT_TABS: readonly { id: AccountTabId; label: string }[] = [
  { id: 'summary', label: 'Summary' },
  { id: 'payments', label: 'Payments' },
  { id: 'timeline', label: 'Timeline' },
  { id: 'policy', label: 'Policy & Co-insurance' },
  { id: 'dispositions', label: 'Dispositions & Efforts' },
  { id: 'history', label: 'History' },
];

/** Validity of an assignment: from, and to for a temporary one. */
export function validity(
  from: string,
  to: string | undefined,
  format: (d: string) => string,
): string {
  return to === undefined ? format(from) : format(from) + ' – ' + format(to);
}

/** Text of a timeline entry's source kind. */
export const TIMELINE_KINDS: Record<string, string> = {
  LEDGER: 'Ledger',
  ASSIGNMENT: 'Assignment',
  DISPOSITION: 'Disposition',
  EFFORT: 'Effort',
  HANDOFF: 'Hand-off',
  INBOX: 'From Operations',
};

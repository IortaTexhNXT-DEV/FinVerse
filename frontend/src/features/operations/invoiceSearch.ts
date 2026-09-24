import type { InvoiceFlag, InvoiceSearch, PaymentStatus, RemittanceStatus } from '@/api/operations';

/** Quick filters of the invoice search (status tabs). */
export type SearchTab = 'ALL' | 'OUTSTANDING' | 'PAID_UNREMITTED' | 'ON_HOLD' | 'LOCKED' | 'DP';

export const SEARCH_TABS: readonly { id: SearchTab; label: string }[] = [
  { id: 'ALL', label: 'All Invoices' },
  { id: 'OUTSTANDING', label: 'Outstanding' },
  { id: 'PAID_UNREMITTED', label: 'Paid, Not Extracted' },
  { id: 'ON_HOLD', label: 'On Hold' },
  { id: 'LOCKED', label: 'Locked' },
  { id: 'DP', label: 'Direct Payment' },
];

const TAB_FILTERS: Record<SearchTab, InvoiceSearch> = {
  ALL: {},
  OUTSTANDING: { payment: 'UNPAID' },
  PAID_UNREMITTED: { payment: 'PAID', remittance: 'UNPROCESSED' },
  ON_HOLD: { flag: 'HOLD' },
  LOCKED: { locked: true },
  DP: { dp: true },
};

/** The criteria of a tab. */
export function tabFilter(tab: SearchTab): InvoiceSearch {
  return TAB_FILTERS[tab];
}

/**
 * The criteria of a URL (links from the Operations home tiles), e.g. {@code
 * ?payment=PAID&remittance=UNPROCESSED}.
 */
export function searchFromParams(params: URLSearchParams): InvoiceSearch {
  const value = (key: string) => params.get(key) ?? undefined;
  const bool = (key: string) => (params.has(key) ? params.get(key) === 'true' : undefined);
  return {
    q: value('q'),
    payment: value('payment') as PaymentStatus | undefined,
    remittance: value('remittance') as RemittanceStatus | undefined,
    flag: value('flag') as InvoiceFlag | undefined,
    locked: bool('locked'),
    dp: bool('dp'),
    insurer: value('insurer'),
    from: value('from'),
    to: value('to'),
  };
}

/** The tab matching a URL's criteria, ALL when none matches exactly. */
export function tabOf(search: InvoiceSearch): SearchTab {
  const keys = (s: InvoiceSearch) =>
    JSON.stringify([s.payment, s.remittance, s.flag, s.locked, s.dp]);
  return SEARCH_TABS.find((t) => keys(TAB_FILTERS[t.id]) === keys(search))?.id ?? 'ALL';
}

/** A trimmed text, or undefined when blank (optional criteria). */
export function orUndefined(value: string): string | undefined {
  const v = value.trim();
  return v === '' ? undefined : v;
}

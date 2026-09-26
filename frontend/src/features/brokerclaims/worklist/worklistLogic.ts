import type { WorklistFlag, WorklistQuery, WorklistTab } from '../home/api';

/** Tabs of the Claims worklist (FR-CL-055); "All" is reached from the home tiles. */
export const WORKLIST_TABS: readonly { id: WorklistTab; label: string }[] = [
  { id: 'MINE', label: 'My Claims' },
  { id: 'OPEN', label: 'Open' },
  { id: 'TEMP_CLOSED', label: 'Temporarily Closed' },
  { id: 'CLOSED', label: 'Closed' },
  { id: 'FOLLOW_UPS_DUE', label: 'Follow-ups Due' },
  { id: 'ALL', label: 'All' },
];

/** Labels of the home tile filters. */
export const FLAG_LABELS: Record<WorklistFlag, string> = {
  OVERDUE: 'Follow-up overdue',
  UNPAID_PREMIUM: 'Unpaid premium',
  AWAITING_REMITTANCE: 'Awaiting premium remittance',
};

const TAB_IDS = new Set<string>(WORKLIST_TABS.map((t) => t.id));

/** The worklist query of a URL (?tab=&flag=&status=), My Claims by default. */
export function queryFromSearch(search: URLSearchParams): WorklistQuery {
  const tab = search.get('tab') ?? '';
  const flag = search.get('flag') ?? '';
  const status = search.get('status') ?? '';
  return {
    tab: TAB_IDS.has(tab) ? (tab as WorklistTab) : 'MINE',
    flag: flag in FLAG_LABELS ? (flag as WorklistFlag) : undefined,
    status: status === '' ? undefined : status,
  };
}

/** The text of the active tile filter, if any. */
export function filterText(query: WorklistQuery): string | undefined {
  const parts: string[] = [];
  if (query.flag !== undefined) {
    parts.push(FLAG_LABELS[query.flag]);
  }
  if (query.status !== undefined) {
    parts.push(`Status ${query.status}`);
  }
  return parts.length === 0 ? undefined : parts.join(' · ');
}

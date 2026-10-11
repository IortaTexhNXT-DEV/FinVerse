import type { ProgrammeTab } from '@/api/eb';
import { countOf } from '@/utils/format';

/** Status tabs of the Programmes work list (design 10.1). */
export const PROGRAMME_TABS: readonly { id: ProgrammeTab; label: string }[] = [
  { id: 'RENEWAL_DUE', label: 'Renewal Due' },
  { id: 'IN_PROGRESS', label: 'In Progress' },
  { id: 'WITH_CLIENT', label: 'With Client' },
  { id: 'IN_PLACEMENT', label: 'In Placement' },
  { id: 'PLACED', label: 'Placed' },
  { id: 'LOST', label: 'Lost' },
  { id: 'ALL', label: 'All' },
];

const TAB_IDS = new Set<string>(PROGRAMME_TABS.map((t) => t.id));

/** The tab named in the URL, Renewal Due by default. */
export function tabOf(value: string | null): ProgrammeTab {
  return value !== null && TAB_IDS.has(value) ? (value as ProgrammeTab) : 'RENEWAL_DUE';
}

/** Cycle stages offered as a filter, in workflow order. */
export const CYCLE_STAGES: readonly string[] = [
  'OPEN',
  'RA_SENT',
  'REQUIREMENTS',
  'INCUMBENT_TERMS',
  'FRANCHISE',
  'PROPOSALS',
  'COMPARATIVE',
  'FOR_SIGNOFF',
  'THRESHOLD_APPROVAL',
  'READY_TO_PRESENT',
  'WITH_CLIENT',
  'REVISION',
  'CONFIRMED',
  'IN_PLACEMENT',
];

/** One line summary of a Send RA run for the toast. */
export function sendRaSummary(results: readonly { sent: boolean }[]): string {
  const sent = results.filter((r) => r.sent).length;
  const refused = results.length - sent;
  return refused === 0
    ? `Renewal advice sent for ${countOf(sent, 'programme')}`
    : `Renewal advice sent for ${countOf(sent, 'programme')}; ${String(refused)} not sent`;
}

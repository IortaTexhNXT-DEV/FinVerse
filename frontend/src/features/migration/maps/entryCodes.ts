import type { EntryAction, MapEntryInput } from '@/api/migration';

/** Actions, labels and sources of a code map entry. */
export const ACTIONS: EntryAction[] = ['MAP', 'CREATE', 'DEFAULT', 'REJECT'];
export const ACTION_LABEL: Record<EntryAction, string> = {
  MAP: 'Map to a BIBS value',
  CREATE: 'Create in BIBS',
  DEFAULT: 'Use the default value',
  REJECT: 'Reject the record',
};
export const SOURCES = ['EBIX', 'QPS', 'ISYS', 'EXCEL', 'CMS'];
export const EMPTY: MapEntryInput = { sourceSystem: 'QPS', legacyCode: '', action: 'MAP' };

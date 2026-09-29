import type { EntryAction, MapEntryInput } from '@/api/migration';
import { BRAND } from '@/branding';

/** Actions, labels and sources of a code map entry. */
export const ACTIONS: EntryAction[] = ['MAP', 'CREATE', 'DEFAULT', 'REJECT'];
export const ACTION_LABEL: Record<EntryAction, string> = {
  MAP: `Map to a ${BRAND.product} value`,
  CREATE: `Create in ${BRAND.product}`,
  DEFAULT: 'Use the default value',
  REJECT: 'Reject the record',
};
export const SOURCES = ['EBIX', 'QPS', 'ISYS', 'EXCEL', 'CMS'];
export const EMPTY: MapEntryInput = { sourceSystem: 'QPS', legacyCode: '', action: 'MAP' };

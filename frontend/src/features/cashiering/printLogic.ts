import type { ReceiptCriteria } from './cashieringApi';

/** The tabs and the searches of Batch Print (FRS.CSH.02.04.02). */

export type PrintTab = 'manual' | 'queue' | 'reprint';

export const PRINT_TABS: readonly { id: PrintTab; label: string }[] = [
  { id: 'manual', label: 'Print' },
  { id: 'queue', label: 'Print Queue' },
  { id: 'reprint', label: 'Re-print' },
];

export interface Filters {
  kind: 'AR' | 'OR';
  branchId: string;
  insurer: string;
  from: string;
  to: string;
  number: string;
}

export const EMPTY_FILTERS: Filters = {
  kind: 'AR',
  branchId: '',
  insurer: '',
  from: '',
  to: '',
  number: '',
};

/** The search of a tab (FRS.CSH.02.04.02): AR by branch and date, OR by insurer and date. */
export function criteriaOf(tab: PrintTab, f: Filters, companyId: number): ReceiptCriteria {
  const base: ReceiptCriteria = { companyId, status: 'ISSUED', kind: f.kind };
  if (tab === 'queue') {
    return { ...base, kind: '', printed: false, systemOnly: true };
  }
  if (tab === 'reprint') {
    return { ...base, printed: true, numberPart: f.number || undefined };
  }
  return {
    ...base,
    branchId: f.kind === 'AR' && f.branchId ? Number(f.branchId) : undefined,
    insurer: f.kind === 'OR' ? f.insurer : undefined,
    from: f.from,
    to: f.to,
  };
}

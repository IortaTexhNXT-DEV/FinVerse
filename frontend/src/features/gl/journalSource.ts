import type { Journal } from '@/api/gl';
import { humanize } from '@/utils/format';

const LABELS: Record<string, string> = {
  JOURNAL_UPLOAD: 'Journal upload',
  RECURRING: 'Recurring journal',
};

/**
 * Where a journal came from: "Manual entry" for a keyed voucher, else the originating function
 * with its reference (e.g. "Journal upload" · UPL-2026-000003).
 */
export function journalSource(j: Pick<Journal, 'sourceModule' | 'sourceReference'>): {
  label: string;
  reference?: string;
} {
  if (j.sourceModule === undefined || j.sourceModule === '') {
    return { label: 'Manual entry' };
  }
  return {
    label: LABELS[j.sourceModule] ?? humanize(j.sourceModule),
    reference: j.sourceReference,
  };
}

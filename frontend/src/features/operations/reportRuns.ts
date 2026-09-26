import type { ReportRun } from '@/api/operations';

/** Status pill of an archived run: VIEWED, EXPORTED_<format> or GENERATED_<format>. */
export function runStatus(run: Pick<ReportRun, 'action' | 'format'>): string {
  if (run.action === 'VIEW') {
    return 'VIEWED';
  }
  const prefix = run.action === 'EXPORT' ? 'EXPORTED' : 'GENERATED';
  return `${prefix}_${run.format ?? ''}`;
}

/**
 * Whether the file of a run can be downloaded now: exports at once, scheduled files from their
 * availability time (BRCLXN.024-029, 045); on-screen runs have no file.
 */
export function fileAvailable(
  run: Pick<ReportRun, 'action' | 'availableFrom'>,
  now: Date = new Date(),
): boolean {
  if (run.action === 'VIEW') {
    return false;
  }
  return run.availableFrom === undefined || new Date(run.availableFrom) <= now;
}

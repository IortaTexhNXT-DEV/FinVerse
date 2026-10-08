import type {
  ConfigUploadAction,
  ConfigUploadJob,
  ConfigUploadRow,
  ConfigUploadRowStatus,
  ConfigUploadStatus,
} from '@/api/configUploads';
import type { Tone } from '@/components/ui/statusTones';

/** Labels and rules of the uploads of the configuration screens (kept apart from the views for tests). */

const STATUS: Record<ConfigUploadStatus, { label: string; tone: Tone }> = {
  VALIDATED: { label: 'Checked, not submitted', tone: 'info' },
  SUBMITTED: { label: 'Waiting for Approval', tone: 'warning' },
  REJECTED: { label: 'Rejected', tone: 'danger' },
  COMPLETED: { label: 'Applied', tone: 'success' },
  CANCELLED: { label: 'Discarded', tone: 'neutral' },
};

/** Label and tone of the status of an upload. */
export function uploadStatus(status: ConfigUploadStatus): { label: string; tone: Tone } {
  return STATUS[status];
}

const ROW_STATUS: Record<ConfigUploadRowStatus, { label: string; tone: Tone }> = {
  VALID: { label: 'Valid', tone: 'success' },
  INVALID: { label: 'Not valid', tone: 'danger' },
  COMMITTED: { label: 'Applied', tone: 'success' },
  FAILED: { label: 'Not applied', tone: 'danger' },
};

/** Label and tone of the status of a row. */
export function rowStatus(status: ConfigUploadRowStatus): { label: string; tone: Tone } {
  return ROW_STATUS[status];
}

const ACTIONS: Record<ConfigUploadAction, string> = {
  ADD: 'Adds a record',
  UPDATE: 'Updates the record',
};

/** What applying a row does, in words. */
export function rowAction(action: ConfigUploadAction | null | undefined): string {
  return action === null || action === undefined ? '—' : ACTIONS[action];
}

/** Rows that add and rows that update among the valid rows of a page. */
export function previewCounts(rows: readonly ConfigUploadRow[]): { add: number; update: number } {
  const valid = rows.filter((r) => r.status === 'VALID' || r.status === 'COMMITTED');
  return {
    add: valid.filter((r) => r.action === 'ADD').length,
    update: valid.filter((r) => r.action === 'UPDATE').length,
  };
}

/** The first values of a row, as a short text for a table cell. */
export function rowSummary(row: ConfigUploadRow, max = 3): string {
  return Object.values(row.values)
    .filter((v) => v.trim() !== '')
    .slice(0, max)
    .join(' · ');
}

/** What the user may do on an upload. */
export type UploadAction = 'submit' | 'discard' | 'approve' | 'reject';

function same(a: string | null | undefined, b: string): boolean {
  return (a ?? '').toLowerCase() === b.toLowerCase();
}

/**
 * The actions open to a user: the uploader submits or discards a checked upload with valid rows;
 * another user with the approval permission of the upload type approves or rejects it. Nobody
 * approves an upload they made.
 */
export function uploadActions(
  job: ConfigUploadJob,
  who: { username: string; mayUpload: boolean; mayApprove: boolean },
): UploadAction[] {
  const uploader = same(job.createdBy, who.username) || same(job.submittedBy, who.username);
  const actions: UploadAction[] = [];
  if (who.mayUpload && uploader && job.status === 'VALIDATED') {
    if (job.validRows > 0) {
      actions.push('submit');
    }
    actions.push('discard');
  }
  if (who.mayApprove && !uploader && job.status === 'SUBMITTED') {
    actions.push('approve', 'reject');
  }
  return actions;
}

/** The link of an upload on the approval screen. */
export function uploadLink(id: number): string {
  return `/admin/config-uploads?job=${id}`;
}

/** A view of the rows of an upload. */
export type RowFilter = 'ALL' | ConfigUploadRowStatus;

/** The views of the rows of an upload, with their counts. */
export function rowTabs(job: ConfigUploadJob): { id: RowFilter; label: string; count: number }[] {
  const applied = job.status === 'COMPLETED';
  const tabs: { id: RowFilter; label: string; count: number }[] = [
    { id: 'ALL', label: 'All rows', count: job.totalRows },
    { id: 'INVALID', label: 'Not valid', count: job.invalidRows },
    applied
      ? { id: 'COMMITTED', label: 'Applied', count: job.committedRows }
      : { id: 'VALID', label: 'Valid', count: job.validRows },
  ];
  if (applied && job.failedRows > 0) {
    tabs.push({ id: 'FAILED', label: 'Not applied', count: job.failedRows });
  }
  return tabs;
}

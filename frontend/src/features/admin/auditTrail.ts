import type { ComboOption } from '@/components/ui/comboOptions';

/** Action types of the audit trail filter, in BDOI's words (FRUM.008.01). */
export const AUDIT_ACTIONS: readonly ComboOption[] = [
  { value: 'CREATE', label: 'Create' },
  { value: 'UPDATE', label: 'Update' },
  { value: 'DEACTIVATE', label: 'Delete / Deactivate' },
  { value: 'SUBMIT', label: 'Submit' },
  { value: 'AUTHORIZE', label: 'Approve' },
  { value: 'REJECT', label: 'Reject' },
  { value: 'LOGIN', label: 'Login' },
  { value: 'LOGIN_FAILED', label: 'Failed Login' },
  { value: 'LOGOUT', label: 'Logout' },
  { value: 'INACTIVITY', label: 'Inactivity' },
  { value: 'TIMEOUT', label: 'Timeout' },
  { value: 'RUN', label: 'Generate' },
  { value: 'EXPORT', label: 'Export' },
  { value: 'POST', label: 'Post' },
  { value: 'REVERSE', label: 'Reverse' },
  { value: 'OPEN', label: 'Open' },
  { value: 'CLOSE', label: 'Close' },
  { value: 'REOPEN', label: 'Reopen' },
];

/** Filters of the Audit Trail screen. */
export interface AuditFilters {
  from: string;
  to: string;
  username: string;
  entityType: string;
  entityId: string;
  action: string;
}

/** The message of a date range whose end is before its start, or null. */
export function dateRangeError(from: string, to: string): string | null {
  return from !== '' && to !== '' && to < from
    ? 'The end date must be on or after the start date'
    : null;
}

/** The report parameters of the export of the filters on screen (report CTL-AUDIT). */
export function exportParams(filters: AuditFilters): Record<string, string> {
  const params: Record<string, string> = { fromDate: filters.from, toDate: filters.to };
  const optional: [string, string][] = [
    ['username', filters.username],
    ['entityType', filters.entityType],
    ['reference', filters.entityId],
    ['action', filters.action],
  ];
  optional.forEach(([key, value]) => {
    if (value.trim() !== '') {
      params[key] = value.trim();
    }
  });
  return params;
}

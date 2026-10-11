import type { Approval } from '@/api/renewal';

/** Severities of a check (FR-RN-020). */
export const SEVERITIES: { code: string; label: string }[] = [
  { code: 'FAIL_EXCEPTION', label: 'Fail – Exception' },
  { code: 'FAIL_REVIEW', label: 'Fail – Review' },
  { code: 'WARN', label: 'Warning' },
  { code: 'INFO', label: 'Information' },
  { code: 'SYSTEM', label: 'System (sets the disposition)' },
];

/** The name of a severity. */
export function severityLabel(code: string | null): string {
  return SEVERITIES.find((s) => s.code === code)?.label ?? code ?? 'Any';
}

/** Whether a record waits for the checker. */
export function pending(approval: Approval): boolean {
  return approval.recordStatus === 'PENDING_AUTHORIZATION';
}

/** A number input value, null when empty. */
export function numberOrNull(value: string): number | null {
  return value.trim() === '' ? null : Number(value);
}

/** A text input value, null when empty. */
export function textOrNull(value: string): string | null {
  return value.trim() === '' ? null : value.trim();
}

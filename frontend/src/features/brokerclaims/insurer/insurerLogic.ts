import { ApiError } from '@/api/client';
import type { InsurerLine } from './api';

/** Pure rules of the insurer side of a claim (FR-CM-021/022/024/032). */

/** Whether the last save was refused because the number is on another claim (FR-CM-021). */
export function isReuse(error: unknown): boolean {
  return error instanceof ApiError && error.code === 'BCL_INSURER_CLAIM_NO_REUSED';
}

/** Total insurer reserve of a claim: the sum of its lines (FR-CM-032 R1). */
export function totalReserve(lines: InsurerLine[]): number {
  return lines.reduce((sum, l) => sum + (l.reserveAmount ?? 0), 0);
}

export interface UpdateErrors {
  date?: string;
  source?: string;
  remarks?: string;
}

/** Errors of an insurer update (FR-CM-022). */
export function updateErrors(
  date: string,
  source: string,
  remarks: string,
  now: string,
): UpdateErrors {
  return {
    date: date === '' || date > now ? 'Enter an update date that is not in the future' : undefined,
    source: source === '' ? 'Select the source of the update' : undefined,
    remarks: remarks.trim() === '' ? 'Enter the remarks' : undefined,
  };
}

/** The recipients typed for one insurer, split on commas, semicolons and spaces. */
export function addresses(text: string): string[] {
  return text
    .split(/[,;\s]+/)
    .map((a) => a.trim())
    .filter((a) => a !== '');
}

/** Whether an address looks like an e-mail address: one @, a local part and a dotted domain. */
export function isEmail(address: string): boolean {
  const parts = address.split('@');
  if (parts.length !== 2) {
    return false;
  }
  const [local = '', domain = ''] = parts;
  const labels = domain.split('.');
  return local !== '' && labels.length > 1 && labels.every((l) => l !== '');
}

/** Errors of the advice recipients; empty when the advice can be sent (FR-CM-024). */
export function adviceErrors(selected: string[], to: Record<string, string>): string[] {
  if (selected.length === 0) {
    return ['Select at least one insurer'];
  }
  const errors: string[] = [];
  selected.forEach((code) => {
    const list = addresses(to[code] ?? '');
    if (list.length === 0) {
      errors.push(`Enter the recipient address for ${code}`);
    }
    list.filter((a) => !isEmail(a)).forEach((a) => errors.push(`Invalid e-mail address: ${a}`));
  });
  return errors;
}

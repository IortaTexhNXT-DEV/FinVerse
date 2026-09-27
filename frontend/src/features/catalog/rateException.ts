import type { RateException, RateExceptionDetail } from '@/api/productCatalog';
import { formatDate } from '@/utils/format';

/** Permission of the approvers of rate exceptions (FR-PM-051). */
export const DECIDE_PERMISSION = 'PRODUCT_AUTHORIZE';

/** Route of an exception record (My Approvals and the requester's notice open it). */
export function rateExceptionPath(reference: string): string {
  return `/catalog/rate-exceptions/${reference}`;
}

/** The status of an exception as its record shows it: pending, approved or rejected. */
export function exceptionStatus(e: Pick<RateException, 'recordStatus'>): string {
  switch (e.recordStatus) {
    case 'ACTIVE':
      return 'APPROVED';
    case 'INACTIVE':
      return 'REJECTED';
    default:
      return 'PENDING_AUTHORIZATION';
  }
}

/**
 * Whether a user may approve or reject an exception: it is pending, the user holds the approval
 * permission and did not request it (four eyes; the server refuses the requester as well).
 */
export function mayDecide(
  e: Pick<RateException, 'recordStatus' | 'requestedBy'>,
  username: string | undefined,
  can: (permission: string) => boolean,
): boolean {
  return (
    e.recordStatus === 'PENDING_AUTHORIZATION' &&
    username !== undefined &&
    username.toLowerCase() !== e.requestedBy.toLowerCase() &&
    can(DECIDE_PERMISSION)
  );
}

/** What the exception approves: "Rate 1.10% until 27-Oct-2026" or "Version 1 until ...". */
export function exceptionSubject(
  e: Pick<RateException, 'requestedRate' | 'requestedVersionNo' | 'validUntil'>,
): string {
  return `${requestedValue(e)} until ${formatDate(e.validUntil)}`;
}

/** What is requested: "Rate 1.10%" or "Version 1". */
export function requestedValue(
  e: Pick<RateException, 'requestedRate' | 'requestedVersionNo'>,
): string {
  if (e.requestedVersionNo !== undefined) {
    return `Version ${e.requestedVersionNo}`;
  }
  return `Rate ${e.requestedRate === undefined ? '–' : formatRate(e.requestedRate)}%`;
}

/** The scheme in force: "Version 2, rate 1.75%", or why there is no single rate. */
export function schemeInForce(
  d: Pick<RateExceptionDetail, 'currentVersionNo' | 'schemeRate'>,
): string {
  if (d.currentVersionNo === undefined) {
    return 'No version in force';
  }
  const rate =
    d.schemeRate === undefined ? 'rate per insurer' : `rate ${formatRate(d.schemeRate)}%`;
  return `Version ${d.currentVersionNo}, ${rate}`;
}

/** A rate in percent with at least two decimals and no trailing zeros beyond them (1.1 → 1.10). */
export function formatRate(rate: number): string {
  const text = String(Number(rate.toFixed(6)));
  const [whole, decimals = ''] = text.split('.');
  return `${whole ?? '0'}.${decimals.padEnd(2, '0')}`;
}

/** The requested rate less the scheme rate, signed, in percentage points. */
export function rateDifference(requested?: number, scheme?: number): string {
  if (requested === undefined || scheme === undefined) {
    return '';
  }
  const diff = Number((requested - scheme).toFixed(6));
  if (diff === 0) {
    return '0.00';
  }
  return `${diff > 0 ? '+' : '−'}${formatRate(Math.abs(diff))}`;
}

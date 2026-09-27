import type { AccessReason } from '@/api/legacyInquiry';

/** The reasons offered before the legacy archive is opened. */
export const REASONS = [
  { value: 'AUDIT', label: 'Internal or external audit' },
  { value: 'COMPLIANCE_REVIEW', label: 'Compliance review' },
  { value: 'REGULATOR', label: 'Regulator request' },
  { value: 'CLIENT_REQUEST', label: 'Client request or complaint' },
  { value: 'CLAIM', label: 'Claim on a legacy policy' },
  { value: 'OTHER', label: 'Other (give details)' },
];

const KEY = 'legacy-inquiry-reason';

/**
 * The reason given in this browser session, kept so it is asked once per session.
 *
 * @returns the reason, undefined when none
 */
export function sessionReason(): AccessReason | undefined {
  try {
    const raw = sessionStorage.getItem(KEY);
    return raw === null ? undefined : (JSON.parse(raw) as AccessReason);
  } catch {
    return undefined;
  }
}

/**
 * Keeps the reason for the rest of the session.
 *
 * @param reason reason
 */
export function keepReason(reason: AccessReason): void {
  try {
    sessionStorage.setItem(KEY, JSON.stringify(reason));
  } catch {
    // The reason is then asked again on the next visit.
  }
}

/**
 * The readable label of a reason code.
 *
 * @param code code
 * @returns label
 */
export function reasonLabel(code: string | undefined): string {
  return REASONS.find((r) => r.value === code)?.label ?? code ?? '';
}

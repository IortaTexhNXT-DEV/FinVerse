import type { SearchKeyType } from '@/api/csf';
import type { Tone } from '@/components/ui/statusTones';

/** Sidebar section of the Customer Servicing Facility (UX-3). */
export const CSF_SECTION = 'Customer Service Facility';

/** Lists of values of the CSF (V1040). */
export const CSF_LOV = {
  status: 'CSF_STATUS',
  documentType: 'CSF_DOCUMENT_TYPE',
  check: 'CSF_VERIFY_CHECK',
  reason: 'CSF_CHANGE_REASON',
  channel: 'CSF_CHANNEL',
  referral: 'CSF_REFERRAL_FIELD',
  platformDocumentType: 'DOCUMENT_TYPE',
} as const;

/** Key types of the Customer Search with their labels and placeholders. */
export const SEARCH_KEYS: readonly { key: SearchKeyType; label: string; placeholder: string }[] = [
  { key: 'NAME', label: 'Name', placeholder: 'Client name' },
  { key: 'CLIENT_ID', label: 'Client ID', placeholder: 'Client code or ID number' },
  { key: 'ACCOUNT_NO', label: 'Account No.', placeholder: 'ARN or policy number' },
  { key: 'PN_NO', label: 'PN No.', placeholder: 'Promissory note number' },
  { key: 'APPLICATION_NO', label: 'Application No.', placeholder: 'Loan application number' },
];

/** Label of a key type. */
export function searchKeyLabel(key: SearchKeyType): string {
  return SEARCH_KEYS.find((k) => k.key === key)?.label ?? key;
}

/** Why a search cannot run: a blank value, or a name shorter than 3 characters. */
export function searchRefusal(key: SearchKeyType, text: string): string | undefined {
  if (text === '') {
    return 'Enter the value to search';
  }
  return key === 'NAME' && text.length < 3 ? 'Enter at least 3 characters' : undefined;
}

const STATUS_TONES: Record<string, Tone> = {
  PENDING: 'warning',
  AWAITING: 'info',
  BOOKED: 'warning',
  OPEN: 'success',
  CLOSED: 'neutral',
};

/** Pill tone of a CSF status: open green, awaiting blue, pending and booked amber, closed grey. */
export function csfStatusTone(status: string | null | undefined): Tone {
  return status ? (STATUS_TONES[status] ?? 'neutral') : 'neutral';
}

/** Contact fields of the Update Contact dialog, in display order. */
export const CONTACT_FIELDS = [
  { key: 'EMAIL', label: 'E-mail', prop: 'email', hint: 'name@example.com' },
  { key: 'MOBILE', label: 'Mobile', prop: 'mobile', hint: '09xxxxxxxxx or +639xxxxxxxxx' },
  { key: 'PHONE', label: 'Phone', prop: 'phone', hint: 'Digits and separators' },
  { key: 'ADDRESS_LINE', label: 'Address Line', prop: 'addressLine', hint: undefined },
  { key: 'CITY', label: 'City', prop: 'city', hint: undefined },
  { key: 'PROVINCE', label: 'Province', prop: 'province', hint: undefined },
  { key: 'POSTAL_CODE', label: 'Postal Code', prop: 'postalCode', hint: undefined },
] as const;

export type ContactProp = (typeof CONTACT_FIELDS)[number]['prop'];

/** Label of a changed field: a contact field, or the referral list label resolved by the caller. */
export function contactFieldLabel(field: string): string | undefined {
  return CONTACT_FIELDS.find((f) => f.key === field)?.label;
}

/** Plain e-mail check before the server applies the client master rules. */
export function isEmail(text: string): boolean {
  const at = text.indexOf('@');
  const dot = text.lastIndexOf('.');
  return !/\s/.test(text) && at > 0 && dot > at + 1 && dot < text.length - 1;
}

/** Philippine mobile number (client master rule). */
export function isMobile(text: string): boolean {
  return /^(09\d{9}|\+639\d{9})$/.test(text.trim());
}

/** The error of a contact value entered in the dialog, undefined when acceptable. */
export function contactError(key: string, value: string): string | undefined {
  const text = value.trim();
  if (text === '') {
    return undefined;
  }
  if (key === 'EMAIL' && !isEmail(text)) {
    return 'Enter a valid e-mail address';
  }
  if (key === 'MOBILE' && !isMobile(text)) {
    return 'Enter a valid mobile number';
  }
  if (key === 'PHONE' && !/^[0-9+() ./-]{3,30}$/.test(text)) {
    return 'Enter the phone number with digits and separators';
  }
  return undefined;
}

/** Minutes left of a verification, never below zero. */
export function minutesLeft(validUntil: string, now: Date = new Date()): number {
  return Math.max(0, Math.ceil((new Date(validUntil).getTime() - now.getTime()) / 60_000));
}

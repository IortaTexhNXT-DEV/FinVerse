import type { ContactDetails, UserSessionEntry } from '@/api/auth';
import { isEmail } from '@/features/crm/clientForm';

const MOBILE = /^\+?\d[\d ()-]{6,24}$/;

/** Field problems of the contact details before saving (the server checks the same; UQ17). */
export function contactProblems(
  details: ContactDetails,
): Partial<Record<keyof ContactDetails, string>> {
  const problems: Partial<Record<keyof ContactDetails, string>> = {};
  const email = details.email.trim();
  if (email === '') {
    problems.email = 'Enter your e-mail address';
  } else if (!isEmail(email)) {
    problems.email = `${email} is not a valid e-mail address`;
  }
  const mobile = details.mobileNo.trim();
  if (mobile !== '' && !MOBILE.test(mobile)) {
    problems.mobileNo = 'Enter the mobile number with digits only, for example +63 917 123 4567';
  }
  return problems;
}

/** The status shown for a session: OPEN, or why it ended (UAM-NFR-35). */
export function sessionStatus(s: UserSessionEntry): string {
  if (s.open) {
    return 'OPEN';
  }
  return s.endReason ?? 'EXPIRED';
}

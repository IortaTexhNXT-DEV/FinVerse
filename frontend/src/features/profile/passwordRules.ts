/** Mirrors the server password policy so users see problems before submitting. */
export function passwordProblems(password: string, confirmation: string): string[] {
  const problems: string[] = [];
  if (password.length < 10) {
    problems.push('At least 10 characters');
  }
  if (!/[A-Z]/.test(password)) {
    problems.push('An upper-case letter');
  }
  if (!/[a-z]/.test(password)) {
    problems.push('A lower-case letter');
  }
  if (!/\d/.test(password)) {
    problems.push('A digit');
  }
  if (!/[^A-Za-z0-9]/.test(password)) {
    problems.push('A symbol');
  }
  if (password !== confirmation) {
    problems.push('Both new passwords must match');
  }
  return problems;
}

interface PasswordRules {
  historyCount: number;
  minAgeDays: number;
  maxAgeDays: number;
}

function days(count: number): string {
  return count === 1 ? 'a day' : `${String(count)} days`;
}

/** The server rules in words (UAM-NFR-36), shown under the password fields. */
export function policyHint(rules: PasswordRules): string {
  const parts: string[] = [];
  if (rules.historyCount > 0) {
    parts.push(`it must differ from your last ${String(rules.historyCount)} passwords`);
  }
  if (rules.minAgeDays > 0) {
    parts.push(`it can be changed once within ${days(rules.minAgeDays)}`);
  }
  if (rules.maxAgeDays > 0) {
    parts.push(`it expires after ${String(rules.maxAgeDays)} days`);
  }
  if (parts.length === 0) {
    return '';
  }
  const text = parts.join('; ');
  return `${text.charAt(0).toUpperCase()}${text.slice(1)}.`;
}

/** Why the password must be changed before working (FR-UA-005). */
export function changeReasonText(reason: 'RESET' | 'EXPIRED', maxAgeDays?: number): string {
  if (reason === 'RESET') {
    return 'Your password was set by an administrator. Choose your own password to continue.';
  }
  const age =
    maxAgeDays === undefined || maxAgeDays === 0 ? '' : ` than ${String(maxAgeDays)} days`;
  return `Your password is older${age} and has expired. Choose a new password to continue.`;
}

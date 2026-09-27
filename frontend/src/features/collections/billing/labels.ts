import { formatAmount, formatDate } from '@/utils/format';
import type { Statement, StatementStatus } from './api';

/** Tabs and texts of the statements of account screens (BRCLXN.058/060). */

export type StatementTab = 'GENERATED' | 'SENT' | 'CANCELLED';

export const STATEMENT_TABS: readonly {
  id: StatementTab;
  label: string;
  statuses: StatementStatus[];
}[] = [
  { id: 'GENERATED', label: 'To Send', statuses: ['GENERATED'] },
  { id: 'SENT', label: 'Sent', statuses: ['SENT'] },
  { id: 'CANCELLED', label: 'Cancelled', statuses: ['CANCELLED'] },
];

/** The statuses behind a tab. */
export function statusesOfTab(tab: StatementTab): StatementStatus[] {
  return STATEMENT_TABS.find((t) => t.id === tab)?.statuses ?? [];
}

/** The billing cycle of a statement as text. */
export function cycleText(s: Pick<Statement, 'cycleFrom' | 'cycleTo'>): string {
  return `${formatDate(s.cycleFrom)} – ${formatDate(s.cycleTo)}`;
}

/** Subject and body proposed for e-mailing a statement (wording to confirm, CQ18). */
export function statementEmail(s: Statement): { subject: string; body: string } {
  return {
    subject: `Statement of Account ${s.soaNo} – ${s.assuredName}`,
    body:
      `Dear ${s.assuredName},\n\n` +
      `Please find attached your statement of account ${s.soaNo} for account ${s.arn}, ` +
      `billing cycle ${cycleText(s)}. The amount due is ${s.currency} ${formatAmount(s.balance)}, ` +
      `payable on or before ${formatDate(s.dueDate)}.\n\n` +
      'The attachment is protected; the password follows in a separate e-mail. ' +
      'Please disregard this statement if payment has been made.',
  };
}

/** Error of the billing run period (dates as yyyy-mm-dd), or undefined. */
export function periodError(from: string, to: string): string | undefined {
  if (from === '' || to === '') {
    return 'Enter both dates';
  }
  return to < from ? 'The period ends before it starts' : undefined;
}

import type { HelpScreen } from '@/features/help/helpContent';

/**
 * Help entries of the Collections escalation screens (BRCLXN.049/050), in sidebar order; added to
 * `COLLECTIONS_HELP.screens` by the Collections owner.
 */
export const ESCALATION_HELP: HelpScreen[] = [
  {
    name: 'Escalations',
    path: '/collections/escalations',
    summary:
      'Accounts escalated to the team lead or to the unit / section head: automatically by the escalation rules (aging, no commitment, broken promises, overdue installments, amount) or by a user, for one or several invoices at once.',
    workflow: [
      'Escalate Accounts: enter the invoices, the level (or a designated user), the reason and remarks; the invoices of one account are escalated together.',
      'The receiving level acknowledges the escalation, escalates it further with a reason, returns it to the handler with an instruction, or resolves it with the resolution.',
      'A returned escalation is resubmitted by the handler once the instruction is done.',
    ],
    controls: [
      'Rules escalate an account once per rule and month; the job CLX_ESCALATION runs every night after the promise check.',
      'An escalation whose invoices are collected is closed automatically.',
      'Each stage has the SLA of its rule; past it the alert CLX_ESCALATION_OVERDUE is raised.',
      'The target and the account officers are notified (CLX_ESCALATED). Escalating needs CLX_ESCALATE; acting on escalations needs CLX_ESCALATION_HANDLE.',
    ],
  },
  {
    name: 'Escalation Rules',
    path: '/collections/escalation-rules',
    summary:
      'What escalates an account automatically: the basis and threshold, the segment, the receiving level or user, the reason and the SLA in hours. Preview shows the accounts a rule escalates today.',
    workflow: [
      'New Rule or Change: the rule waits for authorization.',
      'Another user with MASTER_AUTHORIZE authorizes it; from then on the job and the broken-promise check use it.',
      'Deactivate a rule instead of deleting it.',
    ],
    controls: [
      'Maker-checker: the maker cannot authorize the rule; pending rules appear in My Approvals.',
      'The defaults of the process (45 days, 60th day from inception, broken promise to the team lead) are to be confirmed by BDOI (CQ14).',
    ],
  },
];

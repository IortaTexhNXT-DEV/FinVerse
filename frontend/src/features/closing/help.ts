import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Planning & Closing screens (listed in the help centre). */
export const PLANNING_HELP: HelpSection = {
  id: 'planning',
  module: 'Planning & Closing',
  intro:
    'Budgets and budget monitoring, inter-company transactions, group consolidation, FX revaluation and the period-end and year-end close.',
  screens: [
    {
      name: 'Budgets',
      path: '/planning/budgets',
      summary:
        'Budget versions by fiscal year: one original version and any number of revisions. Each line is an income or expense account, optionally per cost centre, with twelve monthly amounts.',
      workflow: [
        'Create the version (a revision starts from the latest approved version) and fill the grid: spread evenly or by seasonality, import a CSV or copy prior-year actuals ± %.',
        'Submit it; a different user approves it or rejects it back to the maker.',
        'Approving a revision supersedes the previous approved version.',
      ],
      controls: [
        'The submitter can never approve (maker-checker); submitted versions appear in My Approvals.',
        'Needs BUDGET_MANAGE. Only the latest approved version is used for monitoring.',
      ],
    },
    {
      name: 'Budget vs Actual',
      path: '/planning/budget-vs-actual',
      summary:
        'Budget, actual and variance for the month and the year to date, with utilization (YTD actual / annual budget) and the expense accounts that reached the alert threshold.',
      workflow: [
        'Choose the as-of date and the alert threshold (90 % by default).',
        'Open report GL-BVA for the printable version; GL-BUTIL shows budget utilization.',
      ],
      controls: ['Actuals come from the posted ledger and exclude year-end closing journals.'],
    },
    {
      name: 'Inter-company',
      path: '/planning/intercompany',
      summary:
        'Due-to / due-from relationships between group companies, inter-company charges and settlements, and their reconciliation.',
      workflow: [
        'Define the relationship of a company pair with each company’s due-from and due-to accounts.',
        'Post a charge or settlement: mirror journals in both companies with one IC-… reference and value date.',
        'Reconcile due-from against the counterparty’s due-to per currency (report GL-ICREC).',
      ],
      controls: [
        'Both journals post in one transaction, or neither does. Only active relationships transact.',
        'Base amounts use the SPOT rate of the value date. Needs CONSOLIDATION_RUN.',
      ],
    },
    {
      name: 'Consolidation',
      path: '/planning/consolidation',
      summary:
        'Group consolidation as of a date: translation of each member, elimination of inter-company balances and of the investment against the subsidiary’s equity.',
      workflow: [
        'Set up the group: parent, consolidation currency, CTA, NCI and goodwill accounts, subsidiaries with ownership %.',
        'Run it: balance sheet at the CLOSING rate, income and expenses at the AVERAGE rate, the difference to the translation reserve (CTA).',
        'Review the consolidated trial balance and eliminations, then finalize the run; reports GL-CON-TB, GL-CON-BS, GL-CON-PL, GL-CON-ELIM.',
      ],
      controls: [
        'Company ledgers are never changed; every elimination is balanced and the consolidated trial balance is checked.',
        'A re-run replaces the previous draft; a final run blocks another run for the same date.',
      ],
    },
    {
      name: 'FX Revaluation',
      path: '/planning/fx-revaluation',
      summary:
        'Month-end restatement of foreign currency balances of the accounts flagged for revaluation at the CLOSING rate; the difference goes to unrealized FX gain or loss.',
      workflow: [
        'Choose the period and preview the balances per branch, account and currency.',
        'Post: one REVALUATION journal for the period, optionally reversed on the first day of the next period.',
      ],
      controls: [
        'Each period is revalued once; posting again returns the existing run.',
        'A missing CLOSING rate blocks posting. Needs PERIOD_END_RUN.',
      ],
    },
    {
      name: 'Period-End & Year-End',
      path: '/planning/closing',
      summary:
        'Closing checklists with automatic pass / fail results, the monthly soft close and close, and the year-end close to retained earnings.',
      workflow: [
        'Month end: review the checklist (period status, pending journals, unreconciled items, FX revaluation, trial balance and module checks such as actuarial reserves), start the soft close, revalue, then close.',
        'Year end: preview, then close. One CLOSING journal per branch on the last day of the year zeroes income and expenses against retained earnings; the fiscal year is closed and the next one opened.',
      ],
      controls: [
        'The year-end close requires every period closed or closing, no pending journals, a balanced trial balance, the last period revalued and a retained earnings account.',
        'Unreconciled items: the book entries and bank statement lines of every bank account not yet matched up to the period end (the items of the BRS and the un-reconciled entries reports). They show as a warning to review and do not block the close, because deposits in transit and unpresented cheques are normal at a period end.',
        'A closed fiscal year cannot be reopened.',
        'Soft close and close need PERIOD_MANAGE; the year-end close needs YEAR_END_CLOSE.',
      ],
    },
  ],
};

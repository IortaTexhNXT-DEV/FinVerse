import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the General Ledger screens (listed in the help centre). */
export const GL_HELP: HelpSection = {
  id: 'gl',
  module: 'General Ledger',
  intro: 'Journals, account inquiry, chart of accounts and financial periods.',
  screens: [
    {
      name: 'Journals',
      path: '/gl/journals',
      summary:
        'Register of manual, adjustment and accrual vouchers and of the system journals posted by the business modules, with their status. Open a journal to review, approve, reverse it or attach documents.',
      workflow: [
        'A different user with authorization rights approves a submitted journal; approval posts it to the ledger immediately.',
        'Posted journals are corrected by reversal, never by editing.',
      ],
      controls: [
        'Maker-checker and per-user authorization limits.',
        'Attach supporting documents on the journal screen (PDF, images, Excel, CSV, Word; max 10 MB).',
      ],
    },
    {
      name: 'New Journal',
      path: '/gl/journals/new',
      summary:
        'Enter a manual, adjustment or accrual voucher with any number of debit and credit lines, each with optional cost centre, line of business, reference and narration.',
      workflow: [
        'Save as draft (it may be incomplete or unbalanced), then submit it when balanced.',
        'The journal waits for approval in My Approvals.',
      ],
      controls: [
        'Value date must fall in an open period and within the back / forward-dated window.',
        'Each line is checked against the account’s posting controls (currency, manual posting, required dimensions); cost centre and line of business must be valid codes.',
      ],
    },
    {
      name: 'Recurring Journals',
      path: '/gl/recurring',
      summary:
        'Templates for standing entries and accruals, generated monthly, quarterly or annually on a chosen day (31 = month end).',
      workflow: [
        'Create a balanced template with a start (and optional end) date.',
        'The RECURRING_JOURNALS job generates due occurrences every night; "Run now" generates them on demand.',
        'Generated journals are drafts, or submitted for approval when auto-submit is on.',
      ],
      controls: [
        'Each occurrence is generated once only, however often the job runs.',
        'Auto-reverse creates the reversing draft on the first day of the next period.',
      ],
    },
    {
      name: 'Journal Upload',
      path: '/gl/upload',
      summary:
        'Upload many vouchers from CSV or Excel: one row per line, grouped by voucher key. Download the template for the column layout.',
      workflow: [
        'Validate first: every row and voucher is checked and nothing is created.',
        'Import: each valid voucher becomes a draft journal; invalid vouchers are listed and skipped.',
      ],
      controls: ['Each voucher is created atomically; the upload is recorded in the audit trail.'],
    },
    {
      name: 'Account Inquiry',
      path: '/gl/inquiry',
      summary: 'Statement of any account with opening balance, movements and running balance.',
    },
    {
      name: 'Party Statement',
      path: '/gl/party-statement',
      summary:
        'The single party statement: every receivable and payable document of a policyholder, intermediary, reinsurer or supplier from the sub-ledger, with settled and outstanding amounts, days overdue and ageing.',
      workflow: [
        'Find the party by code or name and choose the as-of date.',
        'For the matched / unmatched statement of a period, run report FIN-ARAP-SOA-MATCH.',
      ],
    },
    {
      name: 'Chart of Accounts',
      path: '/gl/accounts',
      summary: 'GL heads, sub and micro accounts with posting controls, currencies and dimensions.',
      controls: ['New and changed accounts must be authorized before they can be posted to.'],
    },
    {
      name: 'Financial Periods',
      path: '/gl/periods',
      summary:
        'Fiscal years and accounting periods with open, closing, closed and reopened states.',
    },
  ],
};

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
        'The GL Team Lead assigns entries to a poster (FRBS 2.5.1); the poster filters on “Assigned to me”.',
        'Select several pending journals and use Post Selected (FRBS 2.5.6): each is checked and posted on its own and refusals are listed.',
        '“Return to Maker” sends a journal back with remarks; the maker corrects and resubmits it.',
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
        'Save as draft (it may be incomplete or unbalanced), then submit it when balanced; a confirmation shows the totals first (FRBS 2.5.10).',
        'Type an account code or its short code; an accrual can carry a “Reverse on” date and is reversed automatically on that date by the JOURNAL_AUTO_REVERSAL job (FRBS 2.8.1).',
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
      summary:
        'GL heads, sub and micro accounts with posting controls, currencies, dimensions, a unique short code and the negative balance control (FRBS 2.3.x, 2.5.4). Search by code, name or short code.',
      workflow: [
        'A new child account left without a code takes the next number of its parent’s numbering scheme.',
        'Negative balance: Allow, Warn (the maker and checker see a warning) or Block (the journal is refused) when a manual journal leaves the account on the wrong side.',
      ],
      controls: ['New and changed accounts must be authorized before they can be posted to.'],
    },
    {
      name: 'Chart Upload',
      path: '/gl/accounts/upload',
      summary:
        'Load the chart of accounts from a file (FRBS 2.3.1) and maintain the numbering schemes that generate child account numbers (FRBS 2.3.2). Sample file: docs/samples/coa_upload_sample.xlsx.',
      workflow: [
        'Download the template, list every parent before its children, upload and review each row.',
        'Create the valid accounts: each is pending authorization, exactly as when keyed on the chart screen.',
        'Numbering schemes: parent code, separator and digits; a child with a blank code gets the next free number.',
      ],
      controls: [
        'Needs COA_UPLOAD (GL Team Lead). A parent must exist or be an earlier row of the file; codes and short codes must be new.',
      ],
    },
    {
      name: 'Financial Periods',
      path: '/gl/periods',
      summary:
        'Fiscal years and accounting periods with open, closing, closed and reopened states.',
      controls: [
        'Every status change asks for confirmation and states its consequence; reopening a closed period needs a reason (at most 200 characters), recorded in the audit trail.',
      ],
    },
  ],
};

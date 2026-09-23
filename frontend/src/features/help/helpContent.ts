import { CLAIMS_HELP } from '@/features/claims/help';
import { REINSURANCE_HELP } from '@/features/reinsurance/help';
import { RESERVES_HELP } from '@/features/reserves/help';
import { TAX_HELP } from '@/features/tax/help';

/**
 * In-app help. One entry per module; each screen lists its purpose, the usual workflow and the
 * controls (maker-checker, audit, validations) that apply. Keep the text short and factual and
 * update it together with the screen. Modules added later append their own section here.
 */

export interface HelpScreen {
  name: string;
  /** Route of the screen (used for the "Open" link). */
  path?: string;
  summary: string;
  workflow?: string[];
  controls?: string[];
}

export interface HelpSection {
  id: string;
  module: string;
  intro: string;
  screens: HelpScreen[];
}

export const HELP_SECTIONS: HelpSection[] = [
  {
    id: 'overview',
    module: 'Overview',
    intro: 'Your starting point: what needs your attention today.',
    screens: [
      {
        name: 'My Approvals',
        path: '/approvals',
        summary:
          'Everything waiting for your authorization across modules: journals, master data and business documents. The badge in the header shows the count.',
        workflow: [
          'Open an item to review it on its own screen.',
          'Authorize, reject or return it there; the item leaves your inbox.',
        ],
        controls: [
          'You never see your own submissions (maker-checker).',
          'Journals above your authorization limit are not offered to you.',
          'Items waiting longer than the threshold raise a PENDING_APPROVAL_AGEING alert.',
        ],
      },
      {
        name: 'Dashboard',
        path: '/',
        summary:
          'Executive view of the selected company and branch: ledger KPIs, gross written premium against the prior year, claims paid and outstanding, collections and receivables ageing, payables due in 7 and 30 days, cash and bank position, expense budget against actual, open alerts and your pending approvals.',
        workflow: [
          'Choose the company and branch in the header; every widget follows the selection (the budget is company-wide).',
          'Click the pending approvals or open alerts figure to open the inbox or the alert list.',
        ],
        controls: [
          'Figures are read from the posted ledger and the sub-ledger in base currency; unposted journals are excluded.',
          'Each widget loads on its own and shows a message when it has no data.',
        ],
      },
      {
        name: 'Alerts',
        path: '/alerts',
        summary:
          'Exceptions raised by the control rules (large or back-dated journals, weekend postings, negative cash, unbalanced trial balance, suspense balances, approval ageing, failed jobs).',
        workflow: [
          'Acknowledge an alert when you take it over.',
          'Resolve it with a comment once the cause is fixed. A condition that persists is raised again by the next daily check.',
        ],
        controls: ['Every acknowledgement and resolution is audited.'],
      },
    ],
  },
  {
    id: 'gl',
    module: 'General Ledger',
    intro: 'Journals, account inquiry, chart of accounts and financial periods.',
    screens: [
      {
        name: 'Journals and New Journal',
        path: '/gl/journals',
        summary:
          'Manual, adjustment and accrual vouchers with any number of debit and credit lines.',
        workflow: [
          'Save as draft (may be unbalanced), then submit when balanced.',
          'A different user with authorization rights approves; approval posts to the ledger immediately.',
          'Posted journals are corrected by reversal, never by editing.',
        ],
        controls: [
          'Maker-checker and per-user authorization limits.',
          'Value date must fall in an open period and within the back/forward-dated window.',
          'Attach supporting documents on the journal screen (PDF, images, Excel, CSV, Word; max 10 MB).',
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
        controls: [
          'Each voucher is created atomically; the upload is recorded in the audit trail.',
        ],
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
          'GL heads, sub and micro accounts with posting controls, currencies and dimensions.',
        controls: ['New and changed accounts must be authorized before they can be posted to.'],
      },
      {
        name: 'Financial Periods',
        path: '/gl/periods',
        summary:
          'Fiscal years and accounting periods with open, closing, closed and reopened states.',
      },
    ],
  },
  CLAIMS_HELP,
  {
    id: 'accounting-engine',
    module: 'Accounting Engine',
    intro: 'How business events (policies, claims, receipts…) become journals automatically.',
    screens: [
      {
        name: 'Event Types and Accounting Rules',
        path: '/accounting/rules',
        summary:
          'Each business event type is mapped to debit/credit accounts by an accounting rule (per business line and currency, by priority and effective dates).',
        controls: [
          'New and changed rules must be authorized by another user; they appear in the approval inbox.',
        ],
      },
      {
        name: 'Rule Simulator and Event Register',
        path: '/accounting/simulator',
        summary:
          'Preview the journal a sample event would produce, and inspect every processed or failed event.',
      },
    ],
  },
  TAX_HELP,
  {
    id: 'reports',
    module: 'Reports',
    intro: 'Financial statements, registers and control reports with PDF, Excel and CSV export.',
    screens: [
      {
        name: 'Report catalogue',
        path: '/reports',
        summary:
          'Choose a report, fill in the parameters and run it on screen or export it. The Exception Report (CTL-EXCEPTIONS) lists raised alerts and their handling.',
      },
    ],
  },
  {
    id: 'setup',
    module: 'Setup',
    intro: 'Organisation and reference data.',
    screens: [
      {
        name: 'Companies and Branches',
        path: '/setup/branches',
        summary: 'Legal entities and offices with their weekly holidays.',
        controls: ['Maker-checker: changes appear in the authorizers’ approval inbox.'],
      },
      {
        name: 'Business Partners',
        path: '/setup/parties',
        summary:
          'Policyholders, agents, brokers, reinsurers, suppliers and banks used by the sub-ledgers.',
        controls: ['Maker-checker: new and changed partners must be authorized before use.'],
      },
      {
        name: 'Holiday Calendar',
        path: '/setup/holidays',
        summary:
          'Company-wide or branch holidays. Used for working-day checks such as the WEEKEND_POSTING exception.',
      },
      {
        name: 'Currencies, Rates and Dimensions',
        path: '/setup/currencies',
        summary: 'Exchange rates by type and date; cost centres and lines of business.',
      },
    ],
  },
  REINSURANCE_HELP,
  {
    id: 'admin',
    module: 'Administration',
    intro: 'Security, configuration and monitoring (administrators).',
    screens: [
      {
        name: 'Users, Roles and Audit Trail',
        path: '/admin/users',
        summary:
          'User accounts, role permissions, authorization limits and the tamper-evident audit trail.',
      },
      {
        name: 'System Parameters',
        path: '/admin/parameters',
        summary:
          'Business parameters such as session timeout, ageing buckets, report footer and suspense accounts, plus a read-only view of the runtime configuration.',
        controls: ['Values are validated by type and every change is audited.'],
      },
      {
        name: 'Exception Codes',
        path: '/admin/exception-codes',
        summary: 'Severity, threshold amount/days and activation of each monitored exception.',
      },
      {
        name: 'Scheduled Jobs',
        path: '/admin/jobs',
        summary: 'Background jobs with schedule, last and next run, run history and "Run now".',
        controls: ['A failed run raises a JOB_FAILURE alert.'],
      },
      {
        name: 'Application Info',
        path: '/admin/info',
        summary: 'Version, build, database migration level and health of the installation.',
      },
    ],
  },
  {
    id: 'account',
    module: 'Your account',
    intro: 'Personal settings and security.',
    screens: [
      {
        name: 'My Profile',
        path: '/profile',
        summary: 'Your details, roles, permissions and last sign-in; change your password here.',
        controls: [
          'You are signed out automatically after the configured period of inactivity; a warning appears one minute before.',
        ],
      },
    ],
  },
  RESERVES_HELP,
];

/** Sections whose module, screen names or text contain the search term. */
export function searchHelp(term: string, sections: HelpSection[] = HELP_SECTIONS): HelpSection[] {
  const needle = term.trim().toLowerCase();
  if (needle === '') {
    return sections;
  }
  const matches = (s: HelpScreen) =>
    [s.name, s.summary, ...(s.workflow ?? []), ...(s.controls ?? [])]
      .join(' ')
      .toLowerCase()
      .includes(needle);
  return sections
    .map((section) =>
      section.module.toLowerCase().includes(needle)
        ? section
        : { ...section, screens: section.screens.filter(matches) },
    )
    .filter((section) => section.screens.length > 0);
}

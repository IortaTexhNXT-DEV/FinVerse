import { ACCOUNTING_ENGINE_HELP } from '@/features/accounting-engine/help';
import { ACSL_HELP } from '@/features/acsl/help';
import { ACCOUNTS_HELP } from '@/features/accounts/help';
import { ADJUSTMENT_HELP } from '@/features/adjustment/help';
import { ADMIN_HELP } from '@/features/admin/help';
import { ASSETS_HELP } from '@/features/assets/help';
import { BOOKING_HELP } from '@/features/booking/help';
import { BROKING_SETUP_HELP } from '@/features/brokingsetup/help';
import { BULK_HELP } from '@/features/bulk/help';
import { CASHIERING_HELP } from '@/features/cashiering/help';
import { CATALOG_HELP } from '@/features/catalog/help';
import { CLAIMS_HELP } from '@/features/claims/help';
import { COLLECTIONS_HELP } from '@/features/collections/help';
import { COMMISSION_HELP } from '@/features/commission/help';
import { CRM_HELP } from '@/features/crm/help';
import { DISBURSEMENT_HELP } from '@/features/disbursement/help';
import { FRBS_HELP } from '@/features/frbs/help';
import { PROPOSALS_HELP } from '@/features/proposals/help';
import { QUOTATIONS_HELP } from '@/features/quotations/help';
import { PLANNING_HELP } from '@/features/closing/help';
import { GL_HELP } from '@/features/gl/help';
import { ISSUANCE_HELP } from '@/features/issuance/help';
import { NB_DASHBOARD_HELP, NB_REPORTS_HELP } from '@/features/nbreports/help';
import { OPERATIONS_HELP } from '@/features/operations/help';
import { PAYABLES_HELP } from '@/features/payables/help';
import { PAY_REQUESTS_HELP } from '@/features/payrequest/help';
import { PLACEMENT_HELP } from '@/features/placement/help';
import { PRODRECON_HELP } from '@/features/prodrecon/help';
import { withPackageRequestHelp } from '@/features/productmaint/help';
import { RECEIVABLES_HELP } from '@/features/receivables/help';
import { REINSURANCE_HELP } from '@/features/reinsurance/help';
import { REMITTANCE_HELP } from '@/features/remittance/help';
import { RESERVES_HELP } from '@/features/reserves/help';
import { SETUP_HELP } from '@/features/setup/help';
import { TAX_HELP } from '@/features/tax/help';
import { UNDERWRITING_HELP } from '@/features/underwriting/help';
import { WORKSPACE_HELP } from '@/features/workspace/help';

/**
 * In-app help. One section per sidebar module, in sidebar order; each screen lists its purpose,
 * the usual workflow and the controls (maker-checker, audit, validations) that apply. Keep the
 * text short and factual and update it together with the screen. A module keeps its section in
 * `features/<module>/help.ts` and lists it here; every non-hidden screen route needs exactly one
 * entry with its `path` (enforced by helpContent.test.ts).
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
          'Journals, payables documents and claim reserves or settlements above your authorization limit are not offered to you.',
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
  NB_DASHBOARD_HELP,
  WORKSPACE_HELP,
  CRM_HELP,
  QUOTATIONS_HELP,
  ACCOUNTS_HELP,
  PROPOSALS_HELP,
  PLACEMENT_HELP,
  ISSUANCE_HELP,
  BOOKING_HELP,
  PRODRECON_HELP,
  ADJUSTMENT_HELP,
  withPackageRequestHelp(CATALOG_HELP),
  BULK_HELP,
  OPERATIONS_HELP,
  COLLECTIONS_HELP,
  CASHIERING_HELP,
  REMITTANCE_HELP,
  COMMISSION_HELP,
  DISBURSEMENT_HELP,
  PAY_REQUESTS_HELP,
  ACSL_HELP,
  FRBS_HELP,
  GL_HELP,
  RECEIVABLES_HELP,
  PAYABLES_HELP,
  ASSETS_HELP,
  PLANNING_HELP,
  TAX_HELP,
  ACCOUNTING_ENGINE_HELP,
  UNDERWRITING_HELP,
  CLAIMS_HELP,
  REINSURANCE_HELP,
  RESERVES_HELP,
  NB_REPORTS_HELP,
  {
    id: 'reports',
    module: 'Reports',
    intro:
      'Financial statements, registers and control reports with PDF, Excel, ODS, CSV and XML export.',
    screens: [
      {
        name: 'Report Centre',
        path: '/reports',
        summary:
          'Choose a report, fill in the parameters and run it on screen, print it or export it. The Exception Report (CTL-EXCEPTIONS) lists raised alerts and their handling.',
        workflow: [
          'Save the parameters you use often as a named variant; shared variants are offered to everyone who may run the report.',
          'Print opens the PDF with the report, user, run time and filters printed in its header.',
        ],
        controls: [
          'Each report has its own permission (e.g. POLICY_VIEW for underwriting reports, CLAIM_VIEW for claims, REPORT_FINANCIAL for financial statements); the catalogue lists only the reports you may run.',
        ],
      },
    ],
  },
  BROKING_SETUP_HELP,
  SETUP_HELP,
  ADMIN_HELP,
  {
    id: 'account',
    module: 'Help and your account',
    intro: 'This help centre, your personal settings and security.',
    screens: [
      {
        name: 'Help Center',
        path: '/help',
        summary:
          'What each screen is for, how its workflow runs and which controls apply, grouped by module in sidebar order.',
        workflow: [
          'Search by screen, module or keyword (for example a status, a report code or an alert code).',
          'Use Open next to a screen to go straight to it; a screen you have no permission for shows an access message instead.',
        ],
      },
      {
        name: 'My Profile',
        path: '/profile',
        summary: 'Your details, roles, permissions and last sign-in; change your password here.',
        controls: [
          'You are signed out automatically after the configured period of inactivity (SESSION_TIMEOUT_MINUTES); a warning appears after SESSION_IDLE_WARNING_MINUTES (15) of inactivity.',
          'The session also ends at a fixed time after sign-in; a warning appears SESSION_EXPIRY_WARNING_MINUTES (30) before.',
          'You can work in several tabs: a new tab uses the session of the open tabs, activity in any tab keeps all of them signed in, and signing out in one tab signs out all of them.',
        ],
      },
    ],
  },
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

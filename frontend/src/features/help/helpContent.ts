import { ACCOUNTING_ENGINE_HELP } from '@/features/accounting-engine/help';
import { ACSL_HELP } from '@/features/acsl/help';
import { ACCOUNTS_HELP } from '@/features/accounts/help';
import { ADJUSTMENT_HELP } from '@/features/adjustment/help';
import { ADMIN_HELP } from '@/features/admin/help';
import { ASSETS_HELP } from '@/features/assets/help';
import { BOOKING_HELP } from '@/features/booking/help';
import { CONFIG_PROMOTION_HELP } from '@/features/configpromo/help';
import { CSF_HELP } from '@/features/csf/help';
import { RENEWAL_HELP } from '@/features/renewal/help';
import { SUBMITTED_HELP } from '@/features/submitted/help';
import { BROKER_CLAIMS_HELP } from '@/features/brokerclaims/help';
import { BROKING_SETUP_HELP } from '@/features/brokingsetup/help';
import { BULK_HELP } from '@/features/bulk/help';
import { CASHIERING_HELP } from '@/features/cashiering/help';
import { CATALOG_HELP } from '@/features/catalog/help';
import { COLLECTIONS_HELP } from '@/features/collections/help';
import { COMMISSION_HELP } from '@/features/commission/help';
import { CRM_HELP } from '@/features/crm/help';
import { DISBURSEMENT_HELP } from '@/features/disbursement/help';
import { EB_HELP } from '@/features/eb/help';
import { FRBS_HELP } from '@/features/frbs/help';
import { PROPOSALS_HELP } from '@/features/proposals/help';
import { QUOTATIONS_HELP } from '@/features/quotations/help';
import { PLANNING_HELP } from '@/features/closing/help';
import { GL_HELP } from '@/features/gl/help';
import { ISSUANCE_HELP } from '@/features/issuance/help';
import { LEGACY_INQUIRY_HELP } from '@/features/legacy-inquiry/help';
import { MIGRATION_HELP } from '@/features/migration/help';
import { USER_ACCESS_HELP } from '@/features/nbadmin/help';
import { NB_DASHBOARD_HELP, NB_REPORTS_HELP } from '@/features/nbreports/help';
import { OPERATIONS_HELP } from '@/features/operations/help';
import { PAYABLES_HELP } from '@/features/payables/help';
import { PAY_REQUESTS_HELP } from '@/features/payrequest/help';
import { PLACEMENT_HELP } from '@/features/placement/help';
import { PRODRECON_HELP } from '@/features/prodrecon/help';
import { withPackageRequestHelp } from '@/features/productmaint/help';
import { RECEIVABLES_HELP } from '@/features/receivables/help';
import { REMITTANCE_HELP } from '@/features/remittance/help';
import { SCREENING_HELP, SCREENING_SETUP_HELP } from '@/features/screening/help';
import { SETUP_HELP } from '@/features/setup/help';
import { TAX_HELP } from '@/features/tax/help';
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
          'Journals and payables documents above your authorization limit are not offered to you.',
          'Items waiting longer than the threshold raise a PENDING_APPROVAL_AGEING alert.',
        ],
      },
      {
        name: 'Dashboard',
        path: '/',
        summary:
          'Executive view of the selected company and branch: ledger KPIs, collections and receivables ageing, payables due in 7 and 30 days, cash and bank position, expense budget against actual, open alerts and your pending approvals.',
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
  SCREENING_HELP,
  QUOTATIONS_HELP,
  ACCOUNTS_HELP,
  PROPOSALS_HELP,
  EB_HELP,
  PLACEMENT_HELP,
  ISSUANCE_HELP,
  BOOKING_HELP,
  RENEWAL_HELP,
  SUBMITTED_HELP,
  PRODRECON_HELP,
  ADJUSTMENT_HELP,
  CSF_HELP,
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
  BROKER_CLAIMS_HELP,
  MIGRATION_HELP,
  LEGACY_INQUIRY_HELP,
  NB_REPORTS_HELP,
  {
    id: 'reports',
    module: 'Reports',
    intro:
      'Financial statements, registers and control reports with Excel, PDF, ODS, CSV and XML export; documents and schedules also in Word.',
    screens: [
      {
        name: 'Report Centre',
        path: '/reports',
        summary:
          'Choose a report, fill in the parameters and run it on screen, print it or export it. The Exception Report (CTL-EXCEPTIONS) lists raised alerts and their handling.',
        workflow: [
          'Save the parameters you use often as a named variant; shared variants are offered to everyone who may run the report.',
          'Print opens the PDF with the report, user, run time and filters printed in its header.',
          'Every report downloads as Excel and PDF. A report that is a document or schedule (board schedules, statements, vouchers) also downloads as Word, with the same layout: the client logo, brand-coloured table headings repeated on every page, "Confidential" footer and page x of y. Paper, orientation and fit to width apply to PDF and Word.',
          'Any generated document downloaded as PDF (slips, letters, statements, forms) can also be downloaded as Word: after the download, choose Download Word in the prompt at the bottom of the screen.',
        ],
        controls: [
          'Each report has its own permission (e.g. TAX_VIEW for tax reports, BCL_REPORT_VIEW for claims handling, REPORT_FINANCIAL for financial statements); the catalogue lists only the reports you may run.',
        ],
      },
    ],
  },
  BROKING_SETUP_HELP,
  SCREENING_SETUP_HELP,
  SETUP_HELP,
  USER_ACCESS_HELP,
  ADMIN_HELP,
  CONFIG_PROMOTION_HELP,
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
        summary:
          'Your details, roles and permissions; your e-mail address and mobile number; your second factor (authenticator app); your password with its rules; and your recent sign-in sessions.',
        workflow: [
          'Contact details: change your e-mail address or mobile number and click Save Contact Details (recorded in the access change log).',
          'Second factor: Set Up App shows a QR code for your authenticator app; enter the code it shows to confirm, then keep the ten recovery codes safe. Replace App moves the second factor to a new phone; New Recovery Codes replaces the codes.',
          'Change password: enter the current password and the new one twice; the rules are shown under the fields.',
          'Recent sessions: your sign-ins with the last activity and how each session ended (log-out, inactivity, end of session, ended by the administrator or account locked).',
        ],
        controls: [
          'A new password has at least 10 characters with upper and lower case letters, a digit and a symbol; it must differ from your last PASSWORD_HISTORY_COUNT (8) passwords, cannot be changed again within PASSWORD_MIN_AGE_DAYS (1) and expires after PASSWORD_MAX_AGE_DAYS (90). You are told 7 days before it expires.',
          'After an administrator reset, on first use or once it has expired, you must change the password before the home page opens. Forgot password? on the login page e-mails a link that works once, within 30 minutes.',
          'You are signed out automatically after the configured period of inactivity; a warning appears after SESSION_IDLE_WARNING_MINUTES (15) of inactivity.',
          'The session also ends at a fixed time after sign-in; a warning appears SESSION_EXPIRY_WARNING_MINUTES (30) before. While you work, the access to the system is renewed in the background every few minutes (ACCESS_TOKEN_MINUTES).',
          'When the second factor is required (MFA_POLICY) you enter the code of your authenticator app after the password; a recovery code works once when the phone is not at hand. A lost phone is reset by two administrators.',
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

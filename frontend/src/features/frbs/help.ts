import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the accounting report pack, account schedules and service fee (FRBS 2.10, 3.2). */
export const FRBS_HELP: HelpSection = {
  id: 'frbs',
  module: 'Accounting Reports',
  intro:
    'The BDOI report pack groups the Comptrollership reports of Appendix A (end of day, GARD, subsidiaries, schedules and ageing, Mancom, service fee, government). Schedules are configuration run by one engine; the service fee pays the referrers their share of fully paid commission.',
  screens: [
    {
      name: 'Report Pack',
      path: '/frbs',
      summary:
        'Every report of the pack by Appendix A group, opened in its runner or exported at once to Excel or PDF for the month to date.',
      controls: [
        'Viewing needs FRBS_REPORT_VIEW and exporting FRBS_REPORT_EXPORT; every run and export is kept in the report archive.',
        'Government reports need TAX_VIEW (granted to the FRBS roles). Several reports at once: Report Centre, Report Batch (ZIP or one merged PDF).',
        'Board-deck schedules flagged "Word requested" (GARD, subsidiaries, ManCom) are exported to Word as well as Excel and PDF, with the BDO Insure header, logo and "Confidential" footer.',
      ],
    },
    {
      name: 'Account Schedules',
      path: '/frbs/schedules',
      summary:
        'Run a GARD, subsidiaries or ageing schedule as of a date, export it, and keep the commentary of the variance analyses; maintain the definitions (accounts, rows, figures, ageing, comparative).',
      controls: [
        'A schedule reads the posted ledger of its accounts (code prefixes or report groups) grouped by account, party, document, cost centre, branch or line of business, in base currency or in its own currency.',
        'Ageing is first in first out: the balance is made of the most recent increases. Up to 8 buckets, e.g. 30,90,180,365,730.',
        'Comments are kept per row and month (FRBS_REPORT_EXPORT); a blank comment removes it. Definitions are maintained with MASTER_MAINTAIN; drafts stay "to confirm" until BDOI confirms the layout (AQ05).',
      ],
    },
    {
      name: 'Service Fee Runs',
      path: '/frbs/service-fee',
      summary:
        'Service-fee runs by stage. A run takes the invoices fully paid in a period, one line per segment, unit and currency; open a run to submit, approve and tag its lines.',
      controls: [
        "The fee is the rate of the segment on the commission net of the insurer's withholding tax. An invoice is paid in one run only; recomputing or cancelling a computed run frees its invoices.",
        'The approver (SERVICE_FEE_APPROVE) is never the preparer. Approval accrues each line (FRBS_SERVICE_FEE_ACCRUE: service fee expense with its cost centre / service fee payable) and sends it to Disbursement as a SERVICE_FEE payment request.',
        "A line is released when Disbursement pays it or when tagged with the credit date, and liquidated with the unit's liquidation report (SERVICE_FEE_TAG). A returned line can be sent again.",
      ],
    },
    {
      name: 'Service Fee Rates',
      path: '/frbs/service-fee/setup',
      summary:
        'The rate of each service-fee segment with the market segments it covers, and the payee and cost centre of each sales unit.',
      controls: [
        'Maintained by the GL team lead (SERVICE_FEE_APPROVE); values are proposals until BDOI confirms them (AQ20).',
        'A unit without a recipient is paid under its own code and charged to its cost centre, else to the cost-centre rules of the accrual (FRBS 3.1.1).',
      ],
    },
  ],
};

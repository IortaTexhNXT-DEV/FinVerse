import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the New Business dashboard (BRNB.012). */
export const NB_DASHBOARD_HELP: HelpSection = {
  id: 'nb-dashboard',
  module: 'New Business Dashboard',
  intro:
    'Real-time tracking of New Business from request to booking; the landing page of the broking roles.',
  screens: [
    {
      name: 'New Business Dashboard',
      path: '/nb/dashboard',
      summary:
        'Headline tiles (new requests, quotations sent this month, SLA breaches, bookings of the month with premium and commission), requests and accounts by status, the quotation-to-booking funnel of the year, the ageing of open accounts by stage and production against target per team.',
      workflow: [
        'Click any tile, bar or chart segment to open the list behind it, filtered on that status or stage.',
        'Change the As of date to see the month and year-to-date figures of an earlier day; ageing and SLA breaches always show the situation now.',
        'Open New Business Reports for the detailed, exportable figures.',
      ],
      controls: [
        'Figures cover the company chosen in the header and are read directly from the workflow, account, placement and booking records.',
        'SLA breaches use the service level of each workflow stage; production uses the sales unit stamped on each account.',
      ],
    },
  ],
};

/** In-app help of the New Business reports and production targets. */
export const NB_REPORTS_HELP: HelpSection = {
  id: 'nb-reports',
  module: 'New Business Reports',
  intro: 'The operational New Business reports and the production targets they compare against.',
  screens: [
    {
      name: 'New Business Reports',
      path: '/nb/reports',
      summary:
        'Placement update (individual or collective), account status with stage age, SLA breach and stalled flag, successful and fall-out accounts per stage, placement summary, CLPC billing, matched and unmatched payments, production statistics, booked accounts and service invoice registers, and the e-policy and Insurance Advice dispatch report.',
      workflow: [
        'Open a report, set its filters and click Run Report to see it on screen.',
        'Download it as PDF, Excel, ODS, CSV or XML, or print it; the print shows the report, user, time and filters used.',
        'Save the filters as a variant to reuse them; share a variant with everyone who may run the report.',
      ],
      controls: [
        'Each report is offered only to the roles allowed to see its data (for example billing reports to Processing, production statistics to team leaders).',
        'Every run and download is recorded in the audit trail.',
        'Custom report building is not available yet (BDOI question Q40); saved variants cover the recurring views.',
      ],
    },
    {
      name: 'Production Targets',
      path: '/nb/targets',
      summary:
        'Monthly booking, premium and commission targets per region, department, team and account officer, in PHP.',
      workflow: [
        'Choose the level and the month; Business Administrators add a target or click a row to change it.',
        'The dashboard and the Production Statistics report compare bookings with the targets, pro rata to the period reported.',
      ],
      controls: [
        'A target is identified by its unit and start date; saving the same unit and start again changes it.',
        'Every change is audited. The SIT/UAT targets are placeholders until BDOI gives its targets (Q41).',
      ],
    },
  ],
};

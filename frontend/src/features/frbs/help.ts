import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the accounting report pack and service fee screens (FRBS 2.10, 3.2). */
export const FRBS_HELP: HelpSection = {
  id: 'frbs',
  module: 'Accounting Reports',
  intro:
    'The BDOI report pack groups the Comptrollership reports that need broking data (Mancom, branch production, GAP, cash flow, expense grouping, government returns) and the service-fee runs for units and referrers.',
  screens: [
    {
      name: 'Report Pack',
      path: '/frbs',
      summary:
        'The report groups of Appendix A with their reports, and the service-fee runs from computation to liquidation.',
      controls: [
        'Viewing needs FRBS_REPORT_VIEW and downloading FRBS_REPORT_EXPORT; every run is kept in the report archive.',
      ],
    },
  ],
};

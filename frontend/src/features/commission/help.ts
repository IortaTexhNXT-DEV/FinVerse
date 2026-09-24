import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Commission Receivables screens (CMRID.001-015, MKTID.012). */
export const COMMISSION_HELP: HelpSection = {
  id: 'commission',
  module: 'Commission Receivables',
  intro:
    'Commission Receivables bills insurers for the commission of direct payment accounts, follows the insurer feedback, collects the commission and reverses the premium receivable, and runs the incentive programmes and BIR certificate tracking.',
  screens: [
    {
      name: 'Commission Workbench',
      path: '/commission',
      summary:
        'Work queues of Commission Receivables (direct payment commission outstanding) and the way into the commission screens.',
      controls: [
        'Insurer feedback is due within CMR_FEEDBACK_WORKING_DAYS (10) working days (CMRID.011).',
      ],
    },
  ],
};

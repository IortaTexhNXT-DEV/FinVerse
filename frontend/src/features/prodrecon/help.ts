import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Product Reconciliation screens (PRCID.001-039). */
export const PRODRECON_HELP: HelpSection = {
  id: 'prodrecon',
  module: 'Product Reconciliation',
  intro:
    'Production Reconciliation sends each insurer the register of booked accounts, uploads the insurer feedback, matches it within the tolerance and follows unmatched and unbooked accounts to closure.',
  screens: [
    {
      name: 'Reconciliation Workbench',
      path: '/prodrecon',
      summary:
        'Work queues of Production Reconciliation (production of the month) and the way into the reconciliation screens.',
      controls: [
        'Differences up to the RECON_TOLERANCE parameter (1.00) count as matched (PRCID.026).',
      ],
    },
  ],
};

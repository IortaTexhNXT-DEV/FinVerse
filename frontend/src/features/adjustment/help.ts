import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Adjustment screens (ADJID.001-028, MKTID.008). */
export const ADJUSTMENT_HELP: HelpSection = {
  id: 'adjustment',
  module: 'Adjustment',
  intro:
    'Adjustment processes financial, non-financial and internal endorsements and cancellations of booked accounts, singly or in batches, with the recomputation per insurer, the postings, the service invoice and the endorsement slip.',
  screens: [
    {
      name: 'Adjustment Workbench',
      path: '/adjustment',
      summary:
        'Work queues of Adjustment (pending negative adjustments) and the way into the adjustment screens.',
      controls: [
        'An invoice locked by Remittance cannot be adjusted until it is released (ADJID.001, RMTID.040).',
      ],
    },
  ],
};

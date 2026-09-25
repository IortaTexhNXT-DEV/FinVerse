import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the refund and cash-advance request screens (MKT 1.2-2.26). */
export const PAY_REQUESTS_HELP: HelpSection = {
  id: 'payrequest',
  module: 'Refund & Cash Advance Requests',
  intro:
    'Marketing raises client refund requests (RRF) and employee cash-advance requests (RFP) here, has them reviewed and approved, and sends them to Disbursement. Refunds of cancelled policies are validated by ACSL and Cashiering first.',
  screens: [
    {
      name: 'Requests Home',
      path: '/payment-requests',
      summary:
        'Your refund, cash-advance and check-cancellation requests by stage, with the requests waiting for your review or approval.',
      controls: [
        'One live refund per AR number (MKT 2.23.0).',
        'Cash advances need HR approval after the Marketing approval.',
      ],
    },
  ],
};

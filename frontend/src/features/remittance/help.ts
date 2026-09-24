import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Remittance screens (RMTID.001-040, MKTID.001-009). */
export const REMITTANCE_HELP: HelpSection = {
  id: 'remittance',
  module: 'Remittance',
  intro:
    'Remittance extracts the collected and cleared premium per insurer and remittance type, builds batches, pushes payment requests to Disbursement and records the insurer ORs; Marketing holds and special remittances are handled here too.',
  screens: [
    {
      name: 'Remittance Workbench',
      path: '/remittance',
      summary:
        'Work queues of Remittance (paid invoices not yet extracted, invoices on hold, locked invoices) and the way into the Remittance screens.',
      controls: [
        'Batch amounts cannot be edited; invoices can only be excluded with a reason (RMTID.002 addendum).',
      ],
    },
  ],
};

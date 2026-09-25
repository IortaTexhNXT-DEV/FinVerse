import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the ACSL screens (ACSL 2.2-2.16). */
export const ACSL_HELP: HelpSection = {
  id: 'acsl',
  module: 'ACSL',
  intro:
    'ACSL (Accounting Control and Sub-Ledger) reconciles the insurer statements of account and the sub-ledgers with the general ledger, investigates accounts on request, and corrects wrong postings with linked correction entries that are reviewed and approved before they post.',
  screens: [
    {
      name: 'ACSL Cases',
      path: '/acsl',
      summary:
        'Analysis requests, investigations and correction entries by stage, and the way into the SOA and GL-SL reconciliations.',
      controls: [
        'Posted journals are never changed: a correction reverses the original line and posts the right one, linked to the invoice family (ACSL 2.9.1).',
        'The approver of a correction is never its preparer.',
      ],
    },
  ],
};

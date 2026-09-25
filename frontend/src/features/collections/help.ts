import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Collections screens (BRCLXN.001-060). */
export const COLLECTIONS_HELP: HelpSection = {
  id: 'collections',
  module: 'Collections',
  intro:
    'Collections follows up the premium receivables of the booked invoices: the worklist per invoice, assignment, collection efforts, dispositions, promises to pay, installments, escalations, billing statements and the collector side of unapplied payments. It reads the Operations invoice ledger and posts no accounting entry: payments, 2307 and direct payment reversals are executed by Cashiering and Commission.',
  screens: [
    {
      name: 'Collections Home',
      path: '/collections',
      summary:
        'Your collection work: open accounts, installments due, promises due today and broken, escalations with you, unapplied payments waiting for your disposition, direct payments returned by insurers and files ready to download.',
      controls: [
        'An invoice enters the worklist when its net outstanding premium is above CLX_MIN_BALANCE_THRESHOLD (BRCLXN.005).',
        'Exports run in the background, are capped at CLX_EXPORT_MAX_ROWS and need CLX_EXPORT.',
      ],
    },
  ],
};

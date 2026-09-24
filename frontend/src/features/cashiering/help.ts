import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Cashiering screens (CSHID.001-027, MKTID.010/013, DBMID.001). */
export const CASHIERING_HELP: HelpSection = {
  id: 'cashiering',
  module: 'Cashiering',
  intro:
    'Cashiering receives premium and non-premium payments, issues acknowledgement receipts (AR) and Head Office official receipts (OR), applies payments to booked invoices component by component and manages unapplied payments and BIR 2307 reversals.',
  screens: [
    {
      name: 'Cashiering Workbench',
      path: '/cashiering',
      summary:
        'Work queues of Cashiering (invoices with outstanding premium, partially paid invoices, hand-offs to complete) and the way into the Cashiering screens.',
      controls: [
        'Receipts, cancellations, reinstatements and dispositions follow maker-checker approval (CASH_APPROVE).',
      ],
    },
  ],
};

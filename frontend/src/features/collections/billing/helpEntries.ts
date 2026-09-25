import type { HelpScreen } from '@/features/help/helpContent';

/**
 * Help entries of the Collections billing screens (BRCLXN.058/060); added to
 * `COLLECTIONS_HELP.screens` by the Collections owner.
 */
export const BILLING_HELP: HelpScreen[] = [
  {
    name: 'Billing Statements',
    path: '/collections/billing',
    summary:
      'Statements of account (SOA) per billing cycle of multi-year and installment accounts: the installment of the cycle with its coverage period, any earlier installment still unpaid, the payments allocated from the invoice ledger and the amount due.',
    workflow: [
      'Billing Run: generates the statements of every billing cycle of the live plans falling due in a period and not billed yet; a single cycle is billed from its installment plan.',
      'Open a statement to download the PDF, send it by e-mail (the PDF is password-protected, the password goes in a separate e-mail) or cancel it so the cycle can be billed again.',
    ],
    controls: [
      'One live statement per plan and billing cycle; the PDF comes from the document template CLX_SOA and records the template version.',
      'Billing is monitoring only: a statement creates no receivable and no commission billing (BRCLXN.060).',
      'Generating, sending and cancelling need CLX_BILLING. Layout, recipient and numbering are to be confirmed by BDOI (CQ18).',
    ],
  },
];

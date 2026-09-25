import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the refund and cash-advance request screens (MKT 1.2-2.26). */
export const PAY_REQUESTS_HELP: HelpSection = {
  id: 'payrequest',
  module: 'Refund & Cash Advance Requests',
  intro:
    'Marketing raises client refund requests (RRF) and employee cash-advance requests (RFP) here, has them reviewed and approved, and sends them to Disbursement. Refunds of cancelled policies are validated by ACSL and Cashiering first; cash advances also need HR approval and are liquidated once paid.',
  screens: [
    {
      name: 'Requests Home',
      path: '/payment-requests',
      summary:
        'Every request by stage with its payment in Disbursement (DV, instrument status). Search by request, payee, reference or DV number; filter by kind and request date. Reviewers endorse and approvers approve the selected requests in bulk.',
      workflow: [
        'Refund: Draft → (Preparing) → For Validation (cancelled policies) → For Review → For Approval → With Disbursement → Disbursed.',
        'Cash advance: Draft → For Review → For Approval → For HR Approval → With Disbursement → Disbursed → liquidation.',
        'Check cancellation: Requested → For Review → For Approval → Sent to Disbursement.',
      ],
      controls: [
        'One live refund per AR number (MKT 2.23.0).',
        'Four eyes: nobody endorses or approves a request they raised or moved before.',
        'A request returned by Disbursement goes back to its preparer and is sent again under a new reference.',
      ],
    },
    {
      name: 'New Refund Request',
      path: '/payment-requests/new-refund',
      summary:
        'The Refund Request Form: segment and reference, the mode of payment with the CA / SA information (prefilled from the client’s known accounts) and one line per AR with the invoice, amount, reason, branch / unit and categories.',
      controls: [
        'All accounts of one request belong to one client.',
        'A line with the reason Cancelled Policy, or on a cancelled invoice, sends the request to ACSL and Cashiering for validation.',
        'On approval the CA / SA information is added to the client record without duplicates (MKT 2.25.0-2.25.1).',
      ],
    },
    {
      name: 'New Cash Advance',
      path: '/payment-requests/new-cash-advance',
      summary:
        'The Request for Payment of an employee cash advance: employee, type, purpose, amount and mode of payment. Marketing approves it, then HR, then Disbursement pays it.',
      controls: [
        'HR approves after Marketing (MKT 1.16.2).',
        'Once paid, the employee liquidates it on the request page; a reviewer checks and posts the liquidation.',
      ],
    },
    {
      name: 'Cancel a Check',
      path: '/payment-requests/check-cancellation',
      summary:
        'Asks Disbursement to cancel the check of a paid refund or cash advance, with the check number and reason, after review and approval (MKT 1.19.0).',
      controls: ['Only one cancellation of a paid request may be in progress at a time.'],
    },
    {
      name: 'Liquidation Accounts',
      path: '/payment-requests/liquidation-accounts',
      summary:
        'The GL accounts a cash-advance liquidation posts to: one per expense category and the cash account of returned excess, maintained by Comptrollership.',
      controls: ['Only users who maintain the accounting rules change them.'],
    },
  ],
};

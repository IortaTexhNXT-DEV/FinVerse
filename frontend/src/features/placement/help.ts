import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Placement & Booking screens. */
export const PLACEMENT_HELP: HelpSection = {
  id: 'placement',
  module: 'Placement & Booking',
  intro:
    'Processing places the validated accounts with the insurers: payment or client confirmation, placement slip, sending, hold cover and insurer returns. Issued policies are handed to booking.',
  screens: [
    {
      name: 'Placement Workbench',
      path: '/placement',
      summary:
        'Accounts from payment to placement: tiles for awaiting payment, ready for placement, placed, returned by insurer and hold cover expiring; tabs For Placement & Booking, Booked Account, Awaiting Payment, Returned by Insurer, Hold Cover Expiring and Cancelled Placement.',
      workflow: [
        'Search by proposal number (ARN), client code or name; open an account from its name.',
        'Select accounts, then For Placement: every prerequisite is shown and one slip is generated per insurer branch.',
        'Send Slips e-mails the generated slips to the insurer placement mailbox; the accounts become Placed.',
        'For Booking hands issued policies to the booking workbench; Cancel Placement and Reactivate work on several accounts at once.',
      ],
      controls: [
        'A slip needs the payment confirmed (or direct payment), the mandatory documents, the TSU clearance when required and a usable insurer branch reachable by e-mail.',
        'Each account of a bulk action is processed on its own; refused accounts are listed with the reason.',
        'Cancelling a placement needs a cancellation reason; every change is kept in the status history.',
      ],
    },
    {
      name: 'Placement Slips',
      path: '/placement/slips',
      summary:
        'Every placement slip PL-yyyy by insurer branch with its accounts, as PDF and Excel, with the send history.',
      workflow: [
        'Download the PDF or Excel file of a slip.',
        'Send or resend a slip to the insurer; the e-mail may be password protected (password in a separate e-mail).',
        'After an insurer return and resubmission, regenerate the slip: a new version is created and the old one is kept.',
      ],
      controls: [
        'The first send records the placement of the accounts; later sends are resends.',
        'Insurer SFTP or API channels are not available yet (Q06): an insurer set up for them is refused.',
      ],
    },
    {
      name: 'CLPC Billing',
      path: '/placement/billing',
      summary:
        'Billing file of the CBG Fire accounts awaiting payment and payment report matching: CLPC reports by PN or loan application number, other segments by ARN.',
      workflow: [
        'Bill the accounts listed (all or the selected ones) and download the billing file in Excel or OpenDocument.',
        'Upload the payment report (.xlsx, .csv or .ods); it opens in the match review with matched, unpaid, unmatched and ambiguous lines.',
        'Match an unmatched or ambiguous line to its account by hand, then confirm: the payment gate opens for every matched, paid account.',
      ],
      controls: [
        'The file transfer to and from CLPC is not built yet (Q28): files are downloaded and uploaded.',
        'Placement records only the gate decision and its evidence; receipts belong to Cashiering.',
        'An account reported unpaid can be billed again on a later batch.',
      ],
    },
  ],
};

import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Production Reconciliation screens (PRCID.001-039). */
export const PRODRECON_HELP: HelpSection = {
  id: 'prodrecon',
  module: 'Production Reconciliation',
  intro:
    'Production Reconciliation sends each insurer the register of booked accounts, uploads the insurer feedback, matches it within the tolerance and follows unmatched and unbooked accounts to closure.',
  screens: [
    {
      name: 'Reconciliation Workbench',
      path: '/prodrecon',
      summary:
        'Work queues of Production Reconciliation (registers to send, cycles waiting for the insurer feedback, cycles being reconciled, unbooked accounts) and the way into the reconciliation screens.',
      controls: [
        'Differences up to the RECON_TOLERANCE parameter (1.00) count as matched (PRCID.026).',
      ],
    },
    {
      name: 'Reconciliation Cycles',
      path: '/prodrecon/cycles',
      summary:
        'One cycle per insurer and production month, from the register sent to the insurer to its closure, with the share of items matched and the BDOI-only and insurer-only counts. Open a cycle to review its items.',
      workflow: [
        'A cycle opens with the first extract of the month (Extracted), moves to Sent to Insurer when the register is e-mailed and to Reconciling when the insurer feedback is uploaded.',
        'In a cycle, the Items tab groups the items by bucket: Matched, With Discrepancy, BDOI Only and Insurer Only. Click an item to compare BDOI and insurer values side by side and record the company concerned, instruction, insurer and marketing feedback and disposition.',
        'Pair a BDOI-only item with an insurer line the matcher missed; split a wrong pairing back into its two sides.',
        'Select several items and use Set Disposition to give them the same disposition.',
        'Run Matching matches again after bookings or corrections; the RECON_AUTOMATCH job does the same every night.',
        'The cycle closes by itself when every item is matched or ready for closure; Close Cycle closes it earlier with a comment.',
      ],
      controls: [
        'Items are matched on the keys of the RECON_MATCH_KEYS parameter (invoice number, then policy number).',
        'Amounts are compared within RECON_TOLERANCE; other fields must agree exactly (names ignore case and spacing).',
        'The Early Incentive tab checks the insurer early remittance incentive against the remittance date; the rate and window come from the remittance rules and show No Rule until they are maintained.',
        'Only a user with RECON_PROCESS can change items; a closed cycle is read only.',
      ],
    },
    {
      name: 'Production Extracts',
      path: '/prodrecon/extracts',
      summary:
        'Registers of booked production extracted for each insurer, by the schedule or with New Extract. Download a register or send it to the insurer.',
      workflow: [
        'New Extract: enter the insurer code and the production month; the register lists the accounts booked for the insurer in the month and opens the cycle of the month if none is open.',
        'Send (RECON_SEND) e-mails the register as a protected workbook; leave To blank to use the insurer reconciliation contacts.',
      ],
      controls: [
        'The file is named <INSURER>_PRODREG_<yyyyMM>_<seq> and only the Remarks and Incentive columns can be edited by the insurer.',
        'Each account is flagged New when it was not on an earlier register of the cycle.',
      ],
    },
    {
      name: 'Insurer Feedback',
      path: '/prodrecon/uploads',
      summary:
        'Registers returned by the insurers, uploaded and matched on arrival, with every attempt and its row counts.',
      workflow: [
        'Upload Feedback: choose the file returned by the insurer; the insurer and month are read from the file name and the rows are matched at once.',
        'Open the cycle to work on the items that did not match.',
      ],
      controls: [
        'A file already taken in (same content) is refused as a duplicate.',
        'Rows that cannot be read are counted as failed with their reason; the rest are still taken in.',
      ],
    },
    {
      name: 'Unbooked Accounts',
      path: '/prodrecon/unbooked',
      summary:
        'Accounts the insurers reported that BDOI has not booked, followed until they are booked or closed with a disposition.',
      workflow: [
        'Not Booked lists the accounts to chase; Pre-booked shows those found as an unbooked account (ARN) in Booking.',
        'When the account is booked, it is matched automatically and moves to Booked Since.',
        'Click an account to record the feedback and disposition, or mark it ready for closure.',
      ],
    },
    {
      name: 'Extract Schedules',
      path: '/prodrecon/schedules',
      summary:
        'When the PRODUCTION_EXTRACT job extracts each insurer register (monthly on a day of the month or weekly on a weekday) and whether it is sent automatically.',
      controls: [
        'A run day on a holiday or weekend moves to the next working day.',
        'Recipients left blank use the insurer reconciliation contacts.',
      ],
    },
  ],
};

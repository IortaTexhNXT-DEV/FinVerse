import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Disbursement screens (DIS 2.2-3.28). */
export const DISBURSEMENT_HELP: HelpSection = {
  id: 'disbursement',
  module: 'Disbursement',
  intro:
    "Disbursement pays what the other units request: remittances to insurers, client refunds, suppliers, government agencies, employees and service fees. Each payment is a disbursement voucher (DV) with a proforma entry, reviewed and approved before it posts, and then followed by its instrument (check, ATD, credit to account, manager's check, credit ticket, telegraphic transfer, online banking).",
  screens: [
    {
      name: 'Disbursement Workbench',
      path: '/disbursement',
      summary:
        'Payment requests and DVs by stage: system requests, waiting for payee, in process, for review, for approval, approved and cancelled. Open a DV to edit its details and entry, follow its instrument and tag the OR / AR and CWT.',
      controls: [
        'Requests from Operations with a maintained payee become DVs at once; refunds and remittances go straight to the approver (DIS 3.25.0, DISB_AUTO_APPROVER_ROUTING).',
        'Requests whose payee is not maintained wait under No Payee with a payee request; they resume when the payee is authorised (parameter DISB_NO_PAYEE_ACTION).',
        'The approver of a DV is never its processor. Approval posts the entry (edited lines are marked Edited) and issues the instrument; a posting failure keeps the DV for approval with the error.',
        'Cancelling an approved DV reverses its entry and sends the request back to its source (DIS 2.20.0). Instrument status edits need the team leader.',
      ],
    },
    {
      name: 'Payees',
      path: '/disbursement/payees',
      summary:
        'The payee master with bank accounts and allowed modes, and the payee requests raised by other units or by unmatched payment requests.',
      controls: [
        'New payees, changes, deactivation and reactivation are authorised by a user other than the maker (DISB_PAYEE).',
        'Bank account numbers are masked unless the user may view them in full (DISB_PAYEE_VIEW_FULL). A payee used by a DV cannot be deleted.',
      ],
    },
    {
      name: 'Disbursement Uploads',
      path: '/disbursement/uploads',
      summary:
        'Uploads of payment requests, credited accounts, negotiated checks, BOB approvals and the payee migration, each validated row by row before committing.',
      controls: [
        'Negotiated checks post the clearing entry of each check; credited accounts and BOB approvals complete the instrument.',
      ],
    },
    {
      name: 'Disbursement End of Day',
      path: '/disbursement/eod',
      summary:
        'Freezes the approved DVs of a business date and produces the checks, the DCTF credit file, the bank forms and the end-of-day reports; confirmations are then sent to the payees.',
      controls: [
        'The scheduler runs the end of day and the confirmations on the configured times; a manual run of a date already run is refused.',
      ],
    },
    {
      name: 'Account Funding',
      path: '/disbursement/funding',
      summary:
        'Transfers between BDOIR bank accounts: requested by the maker, verified, then approved twice before the transfer entry posts.',
      controls: [
        'Verifier and approvers must all be different from the maker and from each other.',
      ],
    },
    {
      name: 'Bank Accounts and Checks',
      path: '/disbursement/banks',
      summary:
        'The paying bank accounts with their GL account and check series; activation and deactivation are authorised by the approver.',
      controls: [
        'A warning is raised when the remaining check leaves fall below DISB_CHECK_SERIES_WARNING. A series can be corrected only before its first check is printed.',
      ],
    },
    {
      name: 'Disbursement Reports',
      path: '/disbursement/reports',
      summary:
        'Payment, control and end-of-day reports (masterlist, unreleased and stale checks, ATD, cash flow, CWT, payees, fall-out, unregularised) with PDF, Excel and CSV exports.',
    },
  ],
};

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
    {
      name: 'Receive Payment',
      path: '/cashiering/receive',
      summary:
        'Over-the-counter payment: enter the ARN, invoice, policy or PN numbers, the payor and the amount. The preview shows the invoices matched and how the payment is applied (DST, VAT, LGT, other charges, then basic premium), the BOOK rate and any excess.',
      workflow: [
        'Several references are applied oldest invoice first.',
        'A 2% CWT client is applied up to 98% of the premium; the 2% waits for the BIR 2307.',
        'An account not booked yet goes to the pre-booked queue; an unknown reference goes to unapplied payments.',
        'Issue AR and Apply saves the payment, issues the AR and posts the application (OPS_AR_RECEIPT, OPS_PAYMENT_APPLY).',
      ],
      controls: ['The AR number comes from the active AR series of the branch.'],
    },
    {
      name: 'Receipts',
      path: '/cashiering/receipts',
      summary:
        'Search ARs and ORs by receipt number, client, invoice, policy, payor, assured, insurer, amount, date and status. Open a receipt to print it, cancel it or reinstate it, and see its applications, lines, journals and history. Issue Official Receipt creates a Head Office OR for service fees, profit share, commissions, incentives or other income.',
      workflow: [
        'Cancel Receipt asks for a reason; the cancellation is posted when the checker approves it and reverses the applications.',
        'Reinstate (full or partial) needs the reason and the encoded fields of the reason group.',
        'The Cancellations and Reinstatements tab lists the requests waiting for approval.',
      ],
      controls: ['The requester cannot approve their own request (CASH_APPROVE).'],
    },
    {
      name: 'Unapplied Payments',
      path: '/cashiering/unapplied',
      summary:
        'Payments not applied to an invoice, in the tabs Unapplied, Monitoring, For Approval, For Reversal and Done. Open an item to assign its disposition: apply to an invoice or its DST, refund, reclass to a client or transfer to a unit.',
      workflow: [
        'Assign a disposition (Monitoring), submit it, and the approver processes it.',
        'A refund goes to the Disbursement queue; a reclass or transfer moves the whole balance.',
        'Submit Selected and Approve Selected act on the checked items.',
        'A completed disposition can be marked for reversal and reversed on approval.',
      ],
      controls: [
        'Dispositions that need approval are processed by a second user (CASH_DISPOSITION_APPROVE).',
      ],
    },
    {
      name: 'Incoming Requests',
      path: '/cashiering/requests',
      summary:
        'What other modules ask of Cashiering: collector requests on unapplied payments from Collections (apply to an invoice, refund, reclass, transfer), refund validations from Payment Requests (is the premium of a cancelled account back in the unapplied list with a new AR?) and payment reversals from ACSL.',
      workflow: [
        'Collector Requests: select one and Accept Request to assign its disposition (the invoice, amount and remarks come from the collector; add the client for a reclass or the unit for a transfer), or Reject Request with a reason.',
        'Tick "Submit the disposition now" to process an application at once; a refund, reclass or transfer goes for approval as usual.',
        'Refund Validations: Confirm Validation with the unapplied item holding the returned premium (its AR is the new AR unless another is entered), or reject it.',
        "Payment Reversals: an approver reverses the receipt's application on the invoice; the money goes back to the unapplied list.",
      ],
      controls: [
        'Collections, Payment Requests and ACSL receive each decision at once.',
        'Accepting and validating need CASH_DISPOSITION; a reversal is approved by a second user with CASH_APPROVE.',
      ],
    },
    {
      name: 'Pre-booked Payments',
      path: '/cashiering/prebooked',
      summary:
        'Payments received for accounts that are not booked yet, with their age. They are applied automatically once the account is booked (PREBOOKED_REMATCH); Re-match Now tries at once and Release moves an item to unapplied payments.',
      controls: ['Items waiting too long raise the PREBOOKED_AGEING alert.'],
    },
    {
      name: 'Payment Uploads',
      path: '/cashiering/uploads',
      summary:
        'Upload a Bills Payment, Trade, CLPC, Direct Credit or PDC file. Each accepted row becomes a payment with its AR and is matched at once; the run summary counts the applied, unapplied, pre-booked, excess and failed rows.',
      controls: [
        'The same file cannot be uploaded twice.',
        'The bank file layouts are configured in Cashiering Setup until BDOI confirms them.',
      ],
    },
    {
      name: 'PDC Warehouse',
      path: '/cashiering/pdc',
      summary:
        'Post-dated checks by maturity month with their PDCW- number. On maturity the check becomes a payment with an AR (PDC_MATURITY); before that it can be returned, replaced or pulled out.',
    },
    {
      name: 'Check Pick-up',
      path: '/cashiering/pickups',
      summary:
        'Checks to collect from clients, filtered by pick-up date. Select the checks picked up and Print ARs to issue and print their receipts in one batch.',
    },
    {
      name: 'Batch Print',
      path: '/cashiering/print',
      summary:
        'Choose receipts by kind, insurer and date, preview the list, print the selected receipts into one PDF and retry the ones that failed.',
    },
    {
      name: 'BIR 2307',
      path: '/cashiering/cwt',
      summary:
        'Marketing tags the BIR 2307 certificates (or cash) of 2% CWT clients; Cashiering receives them, ticks the CWT-copy checklist and validates them into a report per insurer that is routed to Disbursement and released to the insurer.',
      workflow: [
        'Validation reclassifies the 2% to PR2307 (OPS_CWT_RECLASS).',
        'Release to the insurer offsets the PR2307 against the premium due to the insurer (OPS_CWT_DTIP_OFFSET).',
        'A cash 2307 is settled with an AR instead.',
      ],
      controls: [
        'Marketing tags (CWT_TAG), Cashiering validates (CWT_PROCESS), Disbursement releases (DISB_PROCESS).',
      ],
    },
    {
      name: 'Commission ORs',
      path: '/cashiering/commission-ors',
      summary:
        'Upload the commission payments of the insurers and issue one Head Office official receipt per payment with its VAT and withholding tax.',
    },
    {
      name: 'Receipt Series',
      path: '/cashiering/series',
      summary:
        'AR and OR number ranges per branch with their BIR ATP number and the numbers left. A new series is usable once authorized; the RECEIPT_SERIES_LOW alert warns before a series runs out.',
      controls: ['A series is created by one user and authorized by another (maker-checker).'],
    },
    {
      name: 'Cashiering Setup',
      path: '/cashiering/setup',
      summary:
        'The payment file layouts (automatic, delimited or fixed width) and the minimal balance rules, with Run Sweep Now to clear small premium balances and small excess payments at once.',
    },
  ],
};

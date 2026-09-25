import type { HelpScreen } from '@/features/help/helpContent';

/**
 * Help entries of the unapplied-payment screens (BRCLXN.030-048), in sidebar order; added to
 * `COLLECTIONS_HELP.screens` by the Collections owner.
 */
export const UNAPPLIED_HELP: HelpScreen[] = [
  {
    name: 'Unapplied Payments',
    path: '/collections/unapplied',
    summary:
      'Payments Cashiering could not apply to an invoice, as of today, read from Cashiering (never copied): payment date and age, payment file, transaction, amount and balance, payment type, bank and check, the matched client or invoice with its assured, PR balance, segment, unit head, AO and insurer, the latest collector disposition and where the item stands in Cashiering.',
    workflow: [
      'Choose a Cashiering tab: Awaiting Disposition, In Cashiering, For Approval, For Reversal or All Open.',
      'Filter by market segment, collector disposition (or none yet) and age; search by reference, payor, transaction, check or invoice.',
      'Select one payment and Record Disposition, or Request Application to ask Cashiering to apply it to an invoice.',
      'Open a payment to see its account, dispositions, requests and history.',
    ],
    controls: [
      'Only active values of the Unapplied Payment Disposition list can be chosen (BRCLXN.037-039).',
      '"For application to invoice" needs an invoice number in the format of CLX_INVOICE_NO_PATTERN that exists in the invoice ledger (BRCLXN.047/048).',
      'A value with a Cashiering action sends a request to Cashiering, which accepts or rejects it and applies, refunds, reclassifies or transfers the payment with its own approvals.',
      'Dispositions are never deleted; the history stays after the payment is applied or refunded (BRCLXN.040).',
      'Recording dispositions needs CLX_UNAPPLIED_WORK (Collection Handler, Unapplied Payment Handler, AO).',
    ],
  },
  {
    name: 'Requests to Cashiering',
    path: '/collections/unapplied/requests',
    summary:
      'The status of every request sent to Cashiering on a collector disposition: sent, handed over, accepted, executed or rejected, with the Cashiering reference and message, and the "For Application To Invoice" file that listed an application.',
    workflow: [
      'Open requests are on the first tab; executed and rejected ones on theirs.',
      'Select open requests and Check Status to ask Cashiering again (for example after a hand-off done by hand).',
    ],
    controls: [
      'The requester is notified when Cashiering accepts, rejects or executes a request.',
      'The job CLX_APPLICATION_FILE writes the daily "For Application To Invoice" text file of the previous day before 06:00 (BRCLXN.041/042); the report CLX-APPLICATION-TO-INVOICE lists the same requests for any period.',
    ],
  },
];

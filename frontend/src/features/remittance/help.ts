import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Remittance screens (RMTID.001-040, MKTID.001-009). */
export const REMITTANCE_HELP: HelpSection = {
  id: 'remittance',
  module: 'Remittance',
  intro:
    'Remittance extracts the collected and cleared premium per insurer and remittance type, builds batches, pushes payment requests to Disbursement and records the insurer ORs; Marketing holds and special remittances are handled here too.',
  screens: [
    {
      name: 'Remittance Workbench',
      path: '/remittance',
      summary:
        'Work queues of Remittance (batches in review and for approval, batches awaiting the insurer OR, holds and special remittances for approval, paid invoices not yet extracted, invoices on hold or locked) and the way into the Remittance screens.',
      controls: [
        'Batch amounts cannot be edited; invoices can only be excluded with a reason (RMTID.002 addendum).',
      ],
    },
    {
      name: 'Extraction',
      path: '/remittance/extraction',
      summary:
        'Runs the extraction now for an insurer and remittance type, or for one invoice, and lists every scheduled and manual run with the tag each invoice received.',
      workflow: [
        'The REMITTANCE_EXTRACTION job runs off-peak every day; Run Extraction starts one now.',
        'Paid invoices meeting every criterion are Extracted into a new batch per insurer, type and currency; the others are Due - Not Extracted with their reasons (on hold, pending negative adjustment, written off, check within the holding period, paid AR above DTIP, locked by another team) or Not Yet Due.',
        'Open a run to see its tags; each batch is also stored as an Excel extract in the folder of its remittance type.',
      ],
      controls: [
        'Only applied payments count; a payment is held for REMIT_CHECK_HOLD_DAYS banking days of the branch before it is remitted.',
        'Paid AR above the DTIP balance is excluded or capped as set in REMIT_PAIDAR_OVER_DTIP_MODE (decision pending, OQ19).',
        'A failed run is logged, raises REMIT_EXTRACTION_FAILED and notifies the processors.',
      ],
    },
    {
      name: 'Remittance Batches',
      path: '/remittance/batches',
      summary:
        'Process Remittance: the batches by stage, from review to the insurer OR, with the batch page for exclusions, preview, submission, approval, documents and the schedule e-mail.',
      workflow: [
        'Review the accounts of a batch; exclude an account with a reason or restore it from the Exclusions panel while the batch is in review.',
        'Preview and Submit shows the accounts kept, the totals and any problem; the schedule and payment request are stored at submission.',
        'The team leader approves: the remittance is posted, the payment request goes to Disbursement and the commission and incentive ORs are requested from Cashiering.',
        'When Disbursement assigns the DV number the invoices become fully or partially remitted; Send Schedule to Insurer e-mails the protected Excel schedule once.',
        'Return Batch with a reason gives the invoices back, tagged Returned, for the next extraction.',
      ],
      controls: [
        'The approver cannot be the processor or the submitter (four eyes).',
        'Invoices in a batch are locked for remittance: adjustments wait until the batch is remitted, returned or the invoice excluded.',
        'Every exclusion, restore, submission, approval and return is audited and kept in the status history.',
      ],
    },
    {
      name: 'Insurer OR Upload',
      path: '/remittance/insurer-or',
      summary:
        'Uploads the remittance schedules returned by the insurers with their OR number, date and amount per account, and shows the exception report.',
      workflow: [
        'Download the template, fill one line per account (batchNo, invoiceNo, orNo, orDate, orAmount) and upload it.',
        'The exception report compares each OR amount with the paid PR and lists the refused lines with their reason; open an earlier upload from the history.',
      ],
      controls: [
        'Only accounts of approved batches are updated; an OR number is never accepted twice for one client.',
        'A batch moves to OR Received when every account has its OR.',
      ],
    },
    {
      name: 'Remittance Holds',
      path: '/remittance/holds',
      summary:
        'Marketing hold requests keeping invoices out of remittance until a date, with approval, assignment, extension, cancellation and release.',
      workflow: [
        'New Hold Request (or a Collection hold file) creates the request; submitted requests wait for the approver.',
        'An approved hold flags the invoice; the approver assigns it to a remittance processor.',
        'Extensions and cancellations are requested by Marketing and approved; Release removes the hold at once.',
        'The HOLD_EXPIRY job releases expired holds and warns the requestor and the processor the day before.',
      ],
      controls: [
        'One live hold per invoice; the requestor cannot approve their own request.',
        'Held invoices are not extracted until the hold is released or expires.',
      ],
    },
    {
      name: 'Special Remittance',
      path: '/remittance/special',
      summary:
        'Requests to remit an invoice outside the schedule (claims, renewal, installment due, immediate OR), validated at once and remitted through their own special batch.',
      workflow: [
        'New Special Remittance (or a Collection file) checks the invoice: unprocessed or partially remitted, paid AR applied and cleared, not on hold.',
        'The Remittance team leader approves it into a special batch, which follows Process Remittance to Disbursement, or rejects it with a reason.',
      ],
      controls: [
        'The requestor cannot approve their own request; every change is notified to the requestor.',
        'The claims condition is confirmed by the Claims system once connected (OQ46).',
      ],
    },
    {
      name: 'DTIP Status',
      path: '/remittance/dtip',
      summary:
        'What is due to the insurers per invoice with its payment and remittance status, flags and extraction tag, and the search of accounts across batches.',
      workflow: [
        'Search by invoice, ARN, policy or assured; filter by insurer and remittance status.',
        'Queue for End of Day asks the scheduled extraction to examine the invoice tonight.',
        'Accounts in Batches finds an account by invoice, batch, endorsement, policy or assured.',
      ],
    },
    {
      name: 'Incentive Rules',
      path: '/remittance/incentive-rules',
      summary:
        'Early remittance incentive rules per insurer, product line and segment: rate on the basic premium and window after inception or booking.',
      controls: [
        'Invoices matching an active rule within its window go to With Incentives batches; the incentive is deducted from the payment and an incentive OR is requested.',
        'Rates are to be confirmed by BDOI (OQ23); only the Remittance team leader maintains the rules.',
      ],
    },
  ],
};

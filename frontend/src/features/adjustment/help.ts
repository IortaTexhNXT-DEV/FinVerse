import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Adjustment screens (ADJID.001-028, MKTID.008). */
export const ADJUSTMENT_HELP: HelpSection = {
  id: 'adjustment',
  module: 'Adjustment',
  intro:
    'Adjustment processes financial, non-financial and internal endorsements and cancellations of booked invoices, singly or in batches, with the recomputation per insurer, the postings through booking, the service invoice and the endorsement slip.',
  screens: [
    {
      name: 'Adjustment Workbench',
      path: '/adjustment',
      summary:
        'Endorsement and cancellation requests by stage - draft, for validation, for approval, for posting, payments to re-apply, returned and posted - with their aging and flags.',
      workflow: [
        'Search by request number, invoice, ARN, policy, assured or insurer endorsement reference; open a request for its page.',
        'The Operations home tiles open the workbench on the matching tab.',
      ],
      controls: [
        'An invoice locked by Remittance cannot be adjusted until it is released (ADJID.001, RMTID.040).',
        'Aging runs from submission to completion (ADJID.021).',
      ],
    },
    {
      name: 'New Request',
      path: '/adjustment/new',
      summary:
        'Wizard to raise an endorsement or cancellation on one or more booked invoices: invoices, request, recompute and submit.',
      workflow: [
        'Choose the invoices (one request is raised per invoice); invoices locked by another module cannot be chosen.',
        'Pick the endorsement type (FIN_ financial, NF_ non-financial, INT_ internal) and, for a financial endorsement, the request type: the form asks for the cancellation reason and basis, the sum insured change or the amounts.',
        'Recompute shows the before and after of every component, the change per insurer, the service invoice to issue or credit, and whether payments are re-applied or an AR Insurer is set up.',
        'Save as draft or submit for validation; supporting documents are attached on the request page.',
      ],
      controls: [
        'A possible duplicate (same invoice, request type, reason and endorsement reference) needs a justification (ADJID.023).',
        'Cumulative adjustments above the baseline of the original premium need a justification and raise ADJ_OVER_BASELINE (ADJID.028).',
        'A cancellation cannot be combined with another open financial request on the same invoice.',
        'A TSI increase above the package limit needs the quotation prepared by Marketing (ADJID.008).',
      ],
    },
    {
      name: 'Posting Batches',
      path: '/adjustment/batches',
      summary:
        'Requests ready for posting and the validation batches posted, with the outcome of each request.',
      workflow: [
        'Select the requests that do not qualify and Return Selected with a reason: they go back to the requester (ADJID.005/007).',
        'Select the others and Post Selected: they are posted together as one validation batch (VB number).',
        'Open a batch to see which requests were posted, which wait for the payments to be re-applied, and why others failed.',
      ],
      controls: [
        'Premium entries are posted by booking (endorsement or return invoice, service invoice or credit); Adjustment adds the ledger adjustment, the AR Insurer of a remitted decrease and the re-application of payments.',
        'Each request posts in its own transaction: a failure does not stop the others and the request stays for posting.',
        'While Cashiering is not available a paid invoice waits in Payments to Re-apply; the invoice keeps its pending negative adjustment for Remittance.',
      ],
    },
    {
      name: 'Batch Request Upload',
      path: '/adjustment/upload',
      summary:
        'Upload of cancellation and adjustment requests for many invoices, one request per row (ADJID.006).',
      workflow: [
        'Download the template, fill one row per request with the codes of the lists, upload and review each row, then commit the valid rows.',
        'Each committed row is raised and submitted for validation.',
      ],
      controls: [
        'Rows are validated like requests raised on screen; a repeated row or a duplicate without justification is refused.',
      ],
    },
    {
      name: 'Minimal Balance File',
      path: '/adjustment/minimal-balance',
      summary:
        'Write-off (debit) or credit (overpayment) of premium receivable balances from 10.00 to 100.00 listed in a file (ADJID.026).',
      workflow: [
        'Upload the file of invoices with their balances; rows whose invoice and balance match the ledger are processed.',
        'Processed balances are listed with their journal; the Minimal Balance Write-off Summary report gives the totals per file.',
      ],
      controls: [
        'Only balances within MIN_BALANCE_FILE_RANGE are processed; the same file cannot be uploaded twice and an invoice is written off once.',
        'The GL accounts of the write-off are configured by Comptrollership (OQ07, OQ11).',
      ],
    },
  ],
};

import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the ACSL screens (ACSL 2.2-2.16). */
export const ACSL_HELP: HelpSection = {
  id: 'acsl',
  module: 'ACSL',
  intro:
    'ACSL (Accounting Control and Sub-Ledger) reconciles the insurer statements of account and the sub-ledgers with the general ledger, investigates accounts on request, and corrects wrong postings with linked correction entries that are reviewed and approved before they post.',
  screens: [
    {
      name: 'ACSL Cases',
      path: '/acsl',
      summary:
        'Investigations, analysis requests (the refund validations of Marketing), AR refund applications and payment reversals by stage. Open a case on an invoice; the team leader assigns it, the processor records findings and provides the result, raises a correction entry, requests a payment reversal from Cashiering or messages the Account Officer.',
      workflow: ['Received → Assigned → Investigating → Result Provided, or Sent for Correction.'],
      controls: [
        'The result of an analysis request goes back to the requesting module automatically (a refund request is validated or returned).',
        'Every case shows the other cases of the same invoice family.',
      ],
    },
    {
      name: 'Correction Entries',
      path: '/acsl/corrections',
      summary:
        'Linked correction entries by stage. The team leader creates or receives a correction and assigns a preparer; the preparer picks the wrong posted line of the invoice family and names the right account, then submits the balanced entry for review and approval.',
      workflow: ['To Assign → Draft → For Review → For Approval → Posted.'],
      controls: [
        'Posted journals are never changed: a correction reverses the original line and posts the right one, linked to the invoice family (ACSL 2.9.1).',
        'Only a balanced entry can be submitted; the reviewer and approver are never its preparer.',
        'On approval the journal posts, the open items are recorded and matched, and invoice ledger components move as a correction.',
      ],
    },
    {
      name: 'Insurer SOA Reconciliation',
      path: '/acsl/soa',
      summary:
        'Upload an insurer statement of account (CSV, Excel, ODS or text in the insurer layout) for a period; every line is reconciled with the books as outstanding, for remittance, remitted, cancelled, direct billed or not found, with premium and balance variances highlighted.',
      controls: [
        'The same file cannot be uploaded twice for an insurer; rejected rows are listed in the upload log.',
        'Download the reconciliation report, or reconcile again after the books change.',
      ],
    },
    {
      name: 'GL-SL Reconciliation',
      path: '/acsl/gl-sl',
      summary:
        'Compares each control account of the general ledger with its sub-ledger (party ledger, open items or the operations ledger). It runs every night and on demand as of a date; differences are alerted to ACSL.',
      controls: [
        'The reviewer maintains the control accounts and the sub-ledger each one is compared with.',
      ],
    },
  ],
};

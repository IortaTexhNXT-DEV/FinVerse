import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Commission Receivables screens (CMRID.001-015, MKTID.012). */
export const COMMISSION_HELP: HelpSection = {
  id: 'commission',
  module: 'Commission Receivables',
  intro:
    'Commission Receivables bills insurers for the commission of direct payment accounts, follows the insurer feedback, collects the commission and reverses the premium receivable, and runs the incentive programmes and BIR certificate tracking.',
  screens: [
    {
      name: 'Commission Workbench',
      path: '/commission',
      summary:
        'Work queues of Commission Receivables (direct payment accounts to confirm and bill, billings waiting for the insurer or overdue, approved billings to collect, certificates to acknowledge) and the way into the commission screens.',
      controls: [
        'Insurer feedback is due within CMR_FEEDBACK_WORKING_DAYS (10) working days (CMRID.011).',
      ],
    },
    {
      name: 'DP Lists',
      path: '/commission/dp/lists',
      summary:
        'Direct payment lists received from the branches and Head Office, or pulled from the collection feed, with the number of valid and excluded accounts; Branch Submissions shows which branches have not sent a list in a period.',
      workflow: [
        'Upload DP List: choose the branch list; every account is validated at once and appears under DP Accounts, For Confirmation.',
        'Pull from Collection takes in the lists waiting in the collection system.',
        'Open a list to see its accounts.',
      ],
      controls: [
        'The file must be named <Branch>_DP_<yyyyMMdd> (e.g. HO_DP_20260930.xlsx) with the columns Invoice No., Policy No., Insurer, Premium and Remarks.',
        'A file already taken in (same content) is refused.',
      ],
    },
    {
      name: 'DP Accounts',
      path: '/commission/dp/items',
      summary:
        'Each direct payment account from confirmation to billing, insurer answer, collection and premium receivable reversal, with the validation result of every rule.',
      workflow: [
        'For Confirmation: select the accounts the client paid in full to the insurer and use Confirm Paid to Insurer; exclude those that do not belong.',
        'For Billing: select the accounts and use Prepare Billing; one billing is prepared per insurer.',
        'Open an account to see its rules, validate it again, reinstate a reversed premium receivable with a direct payment reason or reverse it again.',
      ],
      controls: [
        'An account is valid when its invoice is booked, tagged direct payment, not cancelled, of the listed insurer and premium, has no pending negative adjustment and commission still open; the same invoice on another list is a duplicate.',
        'Reinstating Due to Cancellation also returns the collected commission to the collection team as an unapplied amount.',
      ],
    },
    {
      name: 'DP Billings',
      path: '/commission/dp/billings',
      summary:
        'Commission billed to each insurer on direct payment accounts, from sending to collection. Open a billing to send it, record the insurer answers, record the collection or cancel it.',
      workflow: [
        'To Send: Send to Insurer e-mails the protected billing workbook and starts the answer due date.',
        'Awaiting Insurer: Record Answers approves or rejects each account; a rejection needs the insurer reason. Rejected accounts return to the collection team.',
        'To Collect: Record Collection posts the commission to the bank, reverses the premium receivable of each approved account and requests the official receipt.',
      ],
      controls: [
        'The DP_FEEDBACK_SLA job alerts the handler when the insurer has not answered by the due date.',
        'A billing can only be cancelled before it is sent.',
        'The proposed bank account comes from the CMR_DP_COLLECTION_BANK parameter.',
        'The premium receivable reversal is posted to the ledger only when DP_PR_REVERSAL_POSTING is on; the invoice ledger always shows it.',
      ],
    },
    {
      name: 'Insurer Responses',
      path: '/commission/dp/responses',
      summary:
        'Billings waiting for the insurer answer with their due date, and the upload of the answers the insurers send back in a file.',
      workflow: [
        'Upload Answers: choose the file with the columns Billing No., Invoice No., Decision (Approved or Rejected), Reason and Comment.',
      ],
      controls: [
        'Rows that do not match an account of a billing waiting for the insurer are reported and skipped.',
      ],
    },
    {
      name: 'Incentive Schemes',
      path: '/commission/incentives/schemes',
      summary:
        'The insurer incentive programmes (No Touch, Top Up, Motor Mania and others) with their period, insurer, segments and tiers.',
      workflow: [
        'New Scheme or click a scheme (INCENTIVE_MANAGE) to maintain it and its tiers.',
        'Production Target Tiers: the highest target reached gives the rate (and multiplier) applied to every eligible invoice.',
        'Fixed Amount per Policy: each policy earns the amount of the highest minimum basic premium it meets.',
      ],
      controls: [
        'The demo schemes have no tiers until the targets and amounts are confirmed.',
        'A Branch beneficiary passes the incentive on to the branches through a disbursement request.',
      ],
    },
    {
      name: 'Incentive Runs',
      path: '/commission/incentives/runs',
      summary:
        'An incentive scheme computed on the production booked in a period, with the invoices excluded, then posted.',
      workflow: [
        'Compute Incentive: choose an active scheme and the booking period.',
        'Open a run to review its invoices; the team leader (COMMREC_APPROVE) posts it, or it is cancelled and computed again.',
      ],
      controls: [
        'Negative amounts and erroneous bookings (cancelled or written off) are excluded by the rules of INCENTIVE_EXCLUSION_RULE and raise an alert.',
        'Posting books the incentive receivable per insurer and, for branch schemes, the pass-on per sales unit.',
      ],
    },
    {
      name: 'BIR Certificates',
      path: '/commission/certificates',
      summary:
        'Certificates of the tax the insurers withheld on commission, tagged to the official receipts they cover and submitted to Comptrollership. Open a submission to attach the scan, acknowledge or reject it.',
      workflow: [
        'Submit Certificate (BIR_CERT_SUBMIT): enter the form, number, period and tax withheld and add the ORs covered.',
        'Comptrollership (BIR_CERT_ACK) acknowledges or rejects the submission with a reason.',
        'A rejected submission is corrected and resubmitted.',
      ],
      controls: ['A certificate must cover at least one official receipt.'],
    },
    {
      name: 'Estimated Items',
      path: '/commission/estimated',
      summary:
        'Invoices booked on an estimated premium, shown as estimated on the production register and commission reports until the final premium is known.',
      workflow: ['Flag Invoice marks an invoice estimated; Clear Estimate removes the flag.'],
    },
  ],
};

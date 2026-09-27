import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Submitted Policies screens (BRD-12). */
export const SUBMITTED_HELP: HelpSection = {
  id: 'submitted',
  module: 'Submitted Policies',
  intro:
    'Submitted Policies keeps the masterlist of the policies the bank submits: the uploads of each source and the documents read by extraction, the processing run (sanitation, matching with the LAMD loan report, classification, disposition and insurer limits), the policy reviews with their IAAF, the Terms of Reference of the policies above the limits, the hand-off to Renewal, the letters, the handling fees and the No Touch billing. The reports are in the Report Centre under Submitted Policies.',
  screens: [
    {
      name: 'Submitted Policies Home',
      path: '/submitted',
      summary:
        'The masterlist by tab, the IAAF and TOR waiting for approval, the hand-offs Renewal has not taken yet, the letters refused and the handling fees to tag or apply.',
      workflow: ['Select a tile to open its list.'],
      controls: ['The counts cover the records of your scope: your segments, or your own records.'],
    },
    {
      name: 'Masterlist',
      path: '/submitted/masterlist',
      summary:
        'Every submitted policy by tab (For Validation, Classified, For Renewal, Manual Disposition, Non-Renewal, Fallout) with search and filters.',
      workflow: [
        'Select records and Assign Handler, or Run Processing on them.',
        'Open a record to see its details, rule results, reviews, TOR, renewal, letters and history.',
      ],
      controls: [
        'A record already handed to Renewal keeps the details it was handed over with.',
        'A Team Leader assigns the handlers; handlers and Account Officers see the records of their scope.',
      ],
    },
    {
      name: 'Submitted Policy',
      summary:
        'The record with its stage, flags and key facts, and the tabs Details, Rule Results, Review & IAAF, TOR, Renewal, Letters and History.',
      workflow: [
        'Validate a record received by hand, dispose a fallout for renewal, exclude it with a non-renewal reason or reinstate it.',
        'Tag it Renewable or Non-Renewable: a manual tag overrides the rules of the next runs.',
        'Renew with BDOI hands a For Renewal record to Renewal at once.',
      ],
      controls: ['Every change is kept in the History tab with the old and the new value.'],
    },
    {
      name: 'Upload & Intake',
      path: '/submitted/intake',
      summary:
        'The uploads of the sources (LFS, HLS, CIU, SPI, Loan Booking, IA masterlist), the LAMD loan snapshot and the migration of the Excel masterlists, with the intake runs.',
      workflow: [
        'Choose the source, download its template, upload the file, check the validation and commit.',
        'A committed intake starts a processing run of the new records.',
      ],
      controls: [
        'A policy already in the masterlist is updated, never duplicated (PN or policy number).',
      ],
    },
    {
      name: 'Extraction Review',
      path: '/submitted/extractions',
      summary:
        'Policy documents read by extraction: each field proposed with its confidence, next to the current record.',
      workflow: [
        'Upload a policy document with its segment and business type.',
        'Check the proposed fields, correct them and Confirm, or Reject with a reason.',
      ],
      controls: ['A document without readable text is proposed empty for entry by hand.'],
    },
    {
      name: 'Processing Runs',
      path: '/submitted/runs',
      summary:
        'The processing runs with their counts and the result of every step for each record.',
      workflow: ['Open a run to see its results; the Fallout filter shows the records to fix.'],
      controls: ['The rules applied are the active rule sets approved in Setup.'],
    },
    {
      name: 'Reviews & IAAF',
      path: '/submitted/reviews',
      summary:
        'The IAAF of the reviewed policies by status: drafts, waiting for approval, approved, sent and returned.',
      workflow: [
        'Record the review of a policy on its record; findings are e-mailed to the bank counterpart.',
        'Generate the IAAF once the last review is adequate, Submit it, and the approvers of each level Approve or Return it.',
        'Send the approved IAAF to the bank counterpart.',
      ],
      controls: [
        'The approval levels come from the approval matrix by segment and sum insured.',
        'The preparer cannot approve the IAAF.',
      ],
    },
    {
      name: 'Terms of Reference',
      path: '/submitted/tors',
      summary:
        'TOR of the policies above the insurer limits, from draft to release to the Account Officer.',
      workflow: [
        'Generate a TOR from a record flagged Insurer Approval, with the proposed terms and the Account Officer.',
        'Submit it; the approvers Approve or Return it; the Account Officer downloads the signed TOR.',
      ],
      controls: ['The breached limits of the last run are copied into the TOR.'],
    },
    {
      name: 'Renewal Work List',
      path: '/submitted/renewals',
      summary:
        'The records handed to Renewal with the insurer assigned, the hold cover and the outcome.',
      workflow: [
        'Scan Now runs the expiry scan: the records For Renewal that near their expiry are handed over.',
        'Re-assign the insurer while its hold cover is not accepted.',
      ],
      controls: ['A hand-off Renewal did not take is offered again by the next scan.'],
    },
    {
      name: 'Letters & Print Batches',
      path: '/submitted/letters',
      summary: 'Letters of the letter rules by status and the print batches for the mail house.',
      workflow: [
        'Send Again a refused letter once the address is corrected.',
        'Download the merged PDF and the control list of a print batch.',
      ],
      controls: ['Every letter keeps the template version it was written with.'],
    },
    {
      name: 'Handling Fees',
      path: '/submitted/fees',
      summary:
        'Handling fees billed, their tagging to the unapplied payments (by PN or location) and their application with an official receipt.',
      workflow: [
        'Upload the billing file, then Tag Now or wait for the tagger.',
        'A payment matching several fees is tagged by hand.',
      ],
      controls: [
        'A tagged payment is recognised as income by Cashiering, which issues the official receipt.',
      ],
    },
    {
      name: 'No Touch Billing',
      path: '/submitted/no-touch',
      summary: 'The monthly No Touch accounts of an insurer: export, insurer return and billing.',
      workflow: [
        'Export the month of an insurer and send the file.',
        'Upload the insurer return, then Bill: a service invoice is issued and the fee is posted.',
      ],
      controls: ['A billed batch cannot be returned again.'],
    },
    {
      name: 'Submitted Policies Setup',
      path: '/submitted/setup',
      summary:
        'Rule sets of the processing steps, insurer limits, insurer assignment, letter rules, the approval matrix, the source register, the legacy status map and the user scopes.',
      workflow: [
        'Changes wait for a checker; a rule set is submitted and approved before it applies.',
      ],
      controls: ['The maker cannot approve a change.'],
    },
  ],
};

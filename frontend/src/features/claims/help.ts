import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Claims screens (listed in the help centre). */
export const CLAIMS_HELP: HelpSection = {
  id: 'claims',
  module: 'Claims',
  intro:
    'Claim notification, reserves, settlements, salvage / subrogation recoveries and motor LPOs, all posted to the ledger at the company share.',
  screens: [
    {
      name: 'Claims',
      path: '/claims',
      summary:
        'Register of claims with status, estimate, paid and outstanding reserve (company share). Open a claim to handle it.',
      workflow: [
        'Open a claim: Overview, Reserves, Settlements, Recoveries, LPOs, Movement history, Attachments.',
        'Checkers close, reopen, repudiate or record the withdrawal of a claim from its header.',
      ],
      controls: [
        'The user who registered a claim cannot close, reopen or decline it.',
        'Closing, repudiation and withdrawal release the outstanding reserve automatically.',
      ],
    },
    {
      name: 'Notify Claim',
      path: '/claims/new',
      summary:
        'First notification of loss: look the policy up, check it is in force at the date of loss, record the loss and the parties, and submit initial reserves.',
      controls: [
        'The claim currency is the policy currency.',
        'A report more than the threshold days after the loss raises LATE_CLAIM_NOTIFICATION.',
      ],
    },
    {
      name: 'Reserves, settlements and recoveries',
      summary:
        'Every financial change is a document entered by a maker and approved by a checker; approval posts the journal and notifies reinsurance.',
      workflow: [
        'Change reserve: new estimate per side (payment / recovery) and cost type (loss / expense).',
        'Settlement: assessed − deductible − excess, partial or final; a final settlement closes the claim.',
        'Recovery: salvage or subrogation money received into a bank account, within the recovery estimate.',
      ],
      controls: [
        'Maker and checker must differ; reserve increases and settlements are limited by the checker authorization limit.',
        'A settlement becomes a claim payable (CLAIM_SETTLEMENT) paid by a payment voucher in Payables.',
        'Reserves at or above the threshold raise LARGE_CLAIM_RESERVE.',
      ],
    },
    {
      name: 'LPO Register',
      path: '/claims/lpos',
      summary:
        'Local purchase orders issued to garages for motor repairs (own damage or third party): gross, discount and net.',
    },
  ],
};

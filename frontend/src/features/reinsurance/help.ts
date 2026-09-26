import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Reinsurance screens (listed in the help centre). */
export const REINSURANCE_HELP: HelpSection = {
  id: 'reinsurance',
  module: 'Reinsurance',
  intro:
    "Treaty programme, cession of premium, facultative placements, reinsurers' share of claims and quarterly statements of account.",
  screens: [
    {
      name: 'Treaties',
      path: '/reinsurance/treaties',
      summary:
        'Quota share, surplus and excess of loss treaties per class and underwriting year, with participants (share, commission, profit commission, premium reserve) and layers.',
      workflow: [
        'The reinsurance officer creates or changes a treaty; it is pending authorization.',
        'A checker with REINSURANCE_AUTHORIZE authorizes it from the list or My Approvals.',
      ],
      controls: [
        'Participants must total 100 %; only one authorized treaty of each type per class and year.',
        'Statements are kept in the company base currency.',
      ],
    },
    {
      name: 'RI Allocation',
      path: '/reinsurance/allocation',
      summary:
        'Cedes approved policies and endorsements: retention first, then quota share, surplus lines and the facultative remainder. Endorsements, refunds and cancellations follow the proportions in force.',
      workflow: [
        'Preview the period, then post; each transaction is ceded once.',
        'Look up a policy to see its cessions or cede it on demand.',
      ],
      controls: [
        'Posting publishes RI_PREMIUM_CEDED and records the amounts due to each reinsurer.',
        'A risk above treaty capacity raises the RI_TREATY_CAPACITY alert.',
      ],
    },
    {
      name: 'FAC Placements',
      path: '/reinsurance/fac',
      summary: 'Facultative slips for the sum insured beyond treaty capacity.',
      workflow: [
        'Record the reinsurers and their shares, then submit the slip.',
        'A different user approves it (premium ceded to the participants); the slip is then closed.',
      ],
      controls: ['Slips left unplaced beyond the threshold raise the RI_FAC_UNPLACED alert.'],
    },
    {
      name: 'Claims Recoveries',
      path: '/reinsurance/claims',
      summary:
        "Reinsurers' share of every claim movement: reserve shares, recoveries due on payments, salvage shared back and excess of loss recoveries.",
      controls: ['Each claim movement is processed once, on its reference.'],
    },
    {
      name: 'Statements of Account',
      path: '/reinsurance/soa',
      summary:
        'Quarterly income / outgo statement per treaty participant with the balance on the smaller side and in words; print or export to PDF.',
      workflow: [
        'Generate the quarter (regenerate while pending); a checker approves it.',
        'Settle the approved statement: the balance is paid or received and the open items are matched.',
      ],
      controls: [
        'Approval posts levy, reserves retained / released and interest (RI_SOA_ADJUSTMENT).',
      ],
    },
  ],
};

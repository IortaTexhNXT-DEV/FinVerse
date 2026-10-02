import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Non-Package Management screens. */
export const PROPOSALS_HELP: HelpSection = {
  id: 'proposals',
  module: 'Non-Package Management',
  intro:
    'Proposal Request Forms (PRF) for risks that need the Technical Services Unit: non-package products and package risks caught by a TSU rule. Marketing raises the PRF, TSU asks the insurers for terms and prepares the proposal slip, Marketing sends it to the client.',
  screens: [
    {
      name: 'Proposal Requests',
      path: '/proposals',
      summary:
        'Work list of PRFs by stage (Drafts, For Approval, With TSU, With Client, Accepted, Not Proceeded). Search by PRF No., ARN, QS/PS No. or client.',
      workflow: [
        'Open a PRF to act on it from the workflow panel.',
        'The tabs show the quotation slip, the insurer responses, the comparative table and the proposal slip as they are produced.',
      ],
      controls: [
        'A PRF is approved by someone other than its maker (four eyes); so are the quotation slip and the proposal slip.',
        'Every change, action and e-mail is audited and visible in the History and E-mails tabs.',
      ],
    },
    {
      name: 'New Proposal Request',
      path: '/proposals/new',
      summary:
        'Client or prospect, product line and risk code, the risk details in free-form sections, risk items with their risk groups, the insurers requested and the mandatory documents.',
      workflow: [
        'Save Draft keeps the PRF open; the mandatory-document checklist appears once saved.',
        'Submit for Approval checks the mandatory documents and sends the PRF to the Marketing approver; after approval it goes to TSU.',
      ],
      controls: [
        'A package risk not caught by a TSU rule cannot be submitted as a PRF: quote it instead.',
        'Documents are stored on the PRF and keep their document type.',
      ],
    },
    {
      name: 'TSU Workbench',
      path: '/proposals/tsu',
      summary:
        'The TSU queues of proposal requests, one tile per stage, oldest due first. Claim a PRF to work it; team leaders assign PRFs.',
      workflow: [
        'Quotation slip: choose the insurers, submit the slip with the reply date, a TSU approver releases it and it is e-mailed to each insurer.',
        'Insurer responses: key in the terms (premium, rate, deductibles, conditions, validity) and attach the insurer document; every change is versioned. Recommend one insurer and mark the terms complete.',
        'Proposal slip: submit it for the chosen insurer, a TSU approver releases it; Marketing sends it with the comparative table to the client.',
        'When the client accepts, record the acceptance (risk groups) and create the accounts: one account per risk group, with the chosen insurer and its rate.',
      ],
      controls: [
        'Documents sent to insurers and clients are password protected; the password follows in a separate e-mail.',
        'Every version of the proposal slip is archived on the PRF.',
      ],
    },
  ],
};

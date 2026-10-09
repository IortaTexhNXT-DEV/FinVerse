import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Policy Issuance screens. */
export const ISSUANCE_HELP: HelpSection = {
  id: 'issuance',
  module: 'Policy Issuance',
  intro:
    'From the placed account to the issued policy: e-policies received from the insurers, policy number update after review, Insurance Advice for mortgaged accounts and encrypted e-policy dispatch to the clients.',
  screens: [
    {
      name: 'Issuance Workbench',
      path: '/issuance',
      summary:
        'Tiles and tabs for placed accounts awaiting the e-policy, e-policies to review, confirmed e-policies ready to send and mortgaged accounts without an Insurance Advice.',
      workflow: [
        'Search by proposal number (ARN), client code or name.',
        'Open an e-policy to review its extraction; upload the e-policy of a placed account from its row.',
        'Select accounts to generate their Insurance Advice, or confirmed e-policies to send them to the clients.',
      ],
      controls: [
        'Every e-policy is reviewed by a user before the policy number reaches the account.',
        'Bulk actions process each record on its own and list the refused ones with the reason.',
      ],
    },
    {
      name: 'E-policy Upload',
      path: '/issuance/upload',
      summary:
        'Upload one e-policy (matched by the ARN chosen, the policy number or the ARN in the file) or many at once with a match review.',
      workflow: [
        'Name each file after its ARN (e.g. ARN-2026-000123_policy.pdf) so it is matched automatically; the ARN printed in the document is also used.',
        'Correct the account of a file or leave it out in the match review, then confirm.',
        'Each file is stored as the EPOLICY document of its account and the policy data is extracted for review.',
      ],
      controls: [
        'Only placed or issued accounts receive e-policies.',
        'E-policies received from insurers are uploaded here.',
        'Extraction reads the PDF text with the insurer patterns; scanned policies are completed by hand.',
      ],
    },
    {
      name: 'Insurance Advice',
      path: '/issuance/insurance-advice',
      summary:
        'Register of Insurance Advices of mortgaged accounts: search, view and download the PDF, generate, and send one or several to the mortgagee bank.',
      workflow: [
        'Advices are generated automatically on policy issue (parameter IA_TRIGGER: or on placement, or manual only).',
        'Generate an advice for mortgaged accounts by ARN when needed.',
        'Select advices and send them: the PDF is encrypted and the password is sent in a separate e-mail.',
        'An advice of a mortgagee bank enrolled for automatic sending is sent when it is generated; the Sending column shows Automatic, Manual, or why it could not be sent automatically.',
      ],
      controls: [
        'Only accounts with a mortgagee bank get an Insurance Advice.',
        'Every generation, download and send is audited.',
        'Processing is notified when an automatic sending cannot be made; the advice is then sent from the register.',
      ],
    },
    {
      name: 'Insurance Advice Recipients',
      path: '/issuance/advice-recipients',
      summary:
        'Per mortgagee bank, and where needed per market segment, the recipients of the Insurance Advices and whether each new advice is sent to them automatically.',
      workflow: [
        'Add the recipient set-up of a bank, with the e-mail addresses and the automatic sending choice.',
        'A second user authorizes the set-up; it applies from its effective date.',
        'A set-up for a market segment takes precedence over the set-up of the bank as a whole.',
      ],
      controls: [
        'Every change waits for a checker other than its maker.',
        'A bank without an authorized set-up has its advices sent manually from the register.',
      ],
    },
    {
      name: 'E-policy Dispatch',
      path: '/issuance/dispatch',
      summary:
        'Send the confirmed e-policies to the clients one by one or in batch, and follow each delivery in the dispatch report.',
      workflow: [
        'Send one e-policy after checking the proposed e-mail (account contact and template), or several at once.',
        'The e-policy is encrypted; the password and its hint are sent in a separate e-mail.',
        'The dispatch report lists every e-policy e-mail with its outcome and the reason of a failure.',
      ],
      controls: [
        'Only confirmed e-policies can be sent (E-policy Sender role).',
        'Passwords are generated.',
      ],
    },
  ],
};

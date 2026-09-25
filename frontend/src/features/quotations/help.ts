import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Quotation / Proposal screens. */
export const QUOTATIONS_HELP: HelpSection = {
  id: 'quotations',
  module: 'Quotation / Proposal',
  intro:
    'Package quotations from the client request to the accounts. Each quotation has a Proposal No. and an Account Reference Number (ARN) that every account created from it carries.',
  screens: [
    {
      name: 'Quotations',
      path: '/quotations',
      summary:
        'Work list of quotations by status (Drafts, For Review, Sent to Client, Accepted, Not Proceeded) with quick filters for your drafts and the quotations expiring soon. Search by Proposal No., ARN or client.',
      workflow: [
        'Open a quotation to act on it from the workflow panel.',
        'Select approved quotations (For Review tab) and use Send via Email to send them in one go: each client receives one e-mail with its quotations, the password follows separately.',
        'Bulk Upload creates draft quotations of one product from a template; Bulk acceptance records accepted quotations by ARN.',
      ],
      controls: [
        'A quotation is approved by someone other than its maker (four eyes) and can be sent only when approved.',
        'Every document sent to a client is password protected; the password is sent in a separate e-mail.',
        'Every change, action and e-mail is audited and visible in the History and E-mails tabs.',
        'Packages are priced on their current version; an item rate other than the scheme rate needs a rate exception (Request Rate Exception) approved in My Approvals before submission.',
      ],
    },
    {
      name: 'New Quotation',
      path: '/quotations/new',
      summary:
        'Five steps: client or prospect, product and terms (insurer, period, validity, direct payment), risk items and their risk groups, the live premium, and review.',
      workflow: [
        'A prospect is enough to quote; the client must be confirmed before accounts are created.',
        'Items sharing a risk group become one account when the client accepts them.',
        'Save Draft keeps the quotation open; Submit for Review sends it to the approver.',
      ],
      controls: [
        'The premium is computed by the server with the rates in force (Appendix A) and, for a package, the rate scheme of its current version.',
        'When a TSU routing rule applies (fleet, total sum insured, non-package risk) the wizard says so: consider a Proposal Request.',
        'The intake template version in force is stamped on the quotation.',
      ],
    },
    {
      name: 'Quotation Requests',
      path: '/quotations/requests',
      summary:
        'Inbox of the quotation requests received by e-mail, upload or source system (HLS) that are waiting to be quoted.',
      workflow: [
        'Capture Request records a request received by e-mail, with the e-mail attached.',
        'Create Prospect adds a prospect for a request without a client; Create Quotation opens the wizard with the client, product and request preselected.',
        'Close a request that will not be quoted, with the reason.',
      ],
      controls: [
        'A request from a source system is recorded once per source reference.',
        'Reading the shared mailbox and the HLS interface wait for BDOI (Q11, Q12); requests are captured or uploaded meanwhile.',
      ],
    },
  ],
};

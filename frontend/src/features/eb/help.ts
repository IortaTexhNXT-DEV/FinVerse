import type { HelpSection } from '@/features/help/helpContent';

/**
 * In-app help of the Employee Benefits screens (BRD-8), in sidebar order. Registered by the
 * foundation (E0); waves E1-B and E1-C complete the entries of their screens.
 */
export const EB_HELP: HelpSection = {
  id: 'eb',
  module: 'Employee Benefits',
  intro:
    "Employee Benefits runs BDOI's group health (HMO), group life (GLI) and group personal accident (GPA) programmes: the yearly renewal advice or new-business cycle, the franchise and proposals from insurers, the comparative with its sign-off and value-threshold approval, and the client's confirmation. Confirmation creates one account per benefit line, which then follows the usual placement, issuance and booking. Member changes, pending items and insurer SOAs are serviced here. Insurers and client HR send their files by e-mail; you record them on the programme.",
  screens: [
    {
      name: 'EB Home',
      path: '/eb',
      summary:
        'Tiles with the counts of renewal advices due, feedback awaited, franchise pending, proposals outstanding, comparatives to sign off, threshold approvals, programmes with the client and pending items overdue; each opens its list.',
      workflow: [
        'A cycle moves Open, Renewal Advice Sent, Requirements, Franchise or Incumbent Terms, Proposals, Comparative, For Sign-off, Threshold Approval, Ready to Present, With Client, Confirmed, In Placement and Placed.',
        'New Programme starts a programme; Open Programmes lists every programme you may see.',
      ],
      controls: [
        'Employee Benefits users see the screens; account officers act on programmes and cycles, the team lead signs off comparatives, BDOI Management approves above the value threshold, Processing validates and Collection follows billing.',
      ],
    },
    {
      name: 'Programmes',
      path: '/eb/programmes',
      summary:
        'Every programme you may see in the tabs Renewal Due, In Progress, With Client, In Placement, Placed, Lost and All, searchable by programme number, client or name and filtered by cycle stage and team. Open a programme for its cycles, lines, contacts, documents, Broker on Record, accounts, pending items and history.',
      workflow: [
        'Renewal Due lists the programmes flagged for renewal whose line expires within the renewal advice lead time (135 days by default) and that have no advice yet.',
        'Select programmes and click Send RA: the renewal advice goes password-protected to the HR contacts that receive it, the renewal cycle opens in Renewal Advice Sent and the advice is kept with the programme and the client. A result list shows the programmes not sent and why.',
        'On the programme, Record Feedback moves the cycle to Client Requirements and stops the reminders; Start Requirements (new business), Stay with Incumbent (renewal) and Go to Market follow. Going to market needs a validated Broker on Record.',
        'Close as Lost or Not Renewed from the workflow panel with a reason.',
      ],
      controls: [
        'The renewal advice job runs every morning; reminders follow at 120, 105 and 90 days before expiry until feedback is recorded. A programme inside the lead time that is not flagged for renewal or has no HR contact for the advice raises an alert to the AO.',
        'Every document is uploaded on its cycle with its type, process and source; a new upload of a type supersedes the earlier version. Each department sees only the document types it may see.',
        'When the client confirms, one account per benefit line is created with the business type of the cycle; a renewal account names the account it renews. The cycle is placed when every account is booked.',
      ],
    },
    {
      name: 'New Programme',
      path: '/eb/programmes/new',
      summary:
        'Pick the client (prospect or confirmed), then enter the programme name, team, funding, account officer and renewal flag, the benefit lines with the incumbent insurer, current policy, ARN, period and headcount, and the HR contacts who receive the renewal advice and SOAs.',
      controls: [
        'At least one benefit line and one HR contact are required. A programme with a current policy is existing business; otherwise it stays a prospect until its first placement.',
        'Every cycle carries its business type, New Business or Renewal, which the accounts created at placement keep.',
      ],
    },
    {
      name: 'Member Changes',
      path: '/eb/member-changes',
      summary:
        'Additions, deletions and plan or data changes of members: captured, relayed to the insurer, billed, validated by Processing and closed.',
      controls: [
        'Documents carry their process (endorsement, adjustment) and are visible by department: billing documents to Marketing, Processing and Collection.',
      ],
    },
    {
      name: 'Pending Items',
      path: '/eb/pending-items',
      summary:
        'Contracts, HMO cards, card replacements and billings pending per programme or member, with the responsible party, due date, days past due and the follow-ups sent; filtered by member, type, party and status.',
      workflow: [
        'Add Pending Item names what is expected, from whom and by when; the contract of each account created at placement is added automatically.',
        'Mark Received (with the date received), Mark Released and Close Item move the item on; closing a pending item needs its date received.',
      ],
      controls: [
        'Every morning a follow-up e-mail goes to the responsible party for items 5 working days past due, then every 5 working days; after 3 follow-ups the item is escalated to the AO with an alert.',
      ],
    },
    {
      name: 'SOA Register',
      path: '/eb/soa',
      summary:
        'Insurer statements of account: received, validated by Processing, released to the client and Collection, linked to the booked invoices with their payment status.',
      controls: ['Open to Processing and Collection.'],
    },
    {
      name: 'EB Setup',
      path: '/eb/setup',
      summary:
        'Value threshold rules that send a comparative to BDOI Management, the required documents per process and benefit line, and the EB parameters (renewal advice lead time, reminders, turn-around times).',
      controls: ['Each change waits for another user to authorize it.'],
    },
  ],
};

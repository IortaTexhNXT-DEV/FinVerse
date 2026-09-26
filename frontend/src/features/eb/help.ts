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
        'Tiles for renewal advices due, feedback awaited, franchise pending, proposals outstanding, comparatives to sign off, threshold approvals, programmes with the client, member changes open and pending items overdue; each opens its list.',
      workflow: [
        'A cycle moves Open, Renewal Advice Sent, Requirements, Franchise or Incumbent Terms, Proposals, Comparative, For Sign-off, Threshold Approval, Ready to Present, With Client, Confirmed, In Placement and Placed (workflow EB_CYCLE).',
        'New Programme starts a programme; Open Programmes lists every programme you may see.',
      ],
      controls: [
        'You see Employee Benefits with EB_VIEW; AO actions need EB_MARKET, sign-off EB_COMPARATIVE_APPROVE, threshold approval EB_THRESHOLD_APPROVE, Processing EB_PROCESS and Collection EB_COLLECT.',
      ],
    },
    {
      name: 'Programmes',
      path: '/eb/programmes',
      summary:
        'Every programme you may see in the tabs Renewal Due, In Progress, With Client, In Placement, Placed and Lost, searchable by programme number or client.',
    },
    {
      name: 'New Programme',
      path: '/eb/programmes/new',
      summary:
        'Pick the client, then enter the programme name, team, funding, the benefit lines with the incumbent insurer, current policy and period, and the HR contacts who receive the renewal advice and SOAs.',
      controls: [
        'Every cycle carries its business type, New Business or Renewal, which the accounts created at placement keep (BRID-022.01).',
      ],
    },
    {
      name: 'Member Changes',
      path: '/eb/member-changes',
      summary:
        'Additions, deletions and plan or data changes of members: captured, relayed to the insurer, billed, validated by Processing and closed (workflow EB_MEMBER_CHANGE).',
      controls: [
        'Documents carry their process (endorsement, adjustment) and are visible by department: billing documents to Marketing, Processing and Collection.',
      ],
    },
    {
      name: 'Pending Items',
      path: '/eb/pending-items',
      summary:
        'Contracts, HMO cards, card replacements and billings pending per programme or member, with due dates and the follow-ups sent.',
    },
    {
      name: 'SOA Register',
      path: '/eb/soa',
      summary:
        'Insurer statements of account: received, validated by Processing, released to the client and Collection, linked to the booked invoices with their payment status (workflow EB_SOA).',
      controls: ['Opens with EB_PROCESS or EB_COLLECT.'],
    },
    {
      name: 'EB Setup',
      path: '/eb/setup',
      summary:
        'Value threshold rules that send a comparative to BDOI Management, the required documents per process and benefit line, and the EB parameters (renewal advice lead time, reminders, turn-around times).',
      controls: ['Needs EB_SETUP; each change waits for another user to authorize it.'],
    },
  ],
};

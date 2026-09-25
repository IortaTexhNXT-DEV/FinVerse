import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Operations foundation screens (OPERATIONS_DESIGN section 12). */
export const OPERATIONS_HELP: HelpSection = {
  id: 'operations',
  module: 'Operations',
  intro:
    'Operations works from one invoice ledger: every booked invoice, endorsement and cancellation is copied from Booking with its premium receivable by component, due to insurer, commission, insurer shares, flags and locks. Cashiering, Remittance, Production Reconciliation, Adjustment and Commission Receivables all read and move this ledger.',
  screens: [
    {
      name: 'Operations Home',
      path: '/operations',
      summary:
        'One card per Operations team you belong to, with live counts from the invoice ledger and the team modules (outstanding premium, paid but not extracted, holds, pending negative adjustments, production of the month, payment requests, failed interface runs), an invoice finder and the links to the integrated applications.',
      workflow: [
        'Open a tile to see the invoices or items behind it.',
        'Use Open on a card to go to the team workbench.',
      ],
      controls: [
        'You see only the sections of your role (BRQID.003).',
        'Application links are maintained as the list of values OPS_EXTERNAL_LINK ("Name|https://address").',
      ],
    },
    {
      name: 'Invoice Search',
      path: '/operations/invoices',
      summary:
        'Every invoice of the ledger with its outstanding premium, payment status, remittance status and flags; open one for its invoice 360: components and balances, insurer shares, movements, the invoice family, one tab per Operations module (receipts, remittances, adjustments, reconciliation, commission) with its record count, history and documents.',
      workflow: [
        'Search by invoice number, ARN, policy number, client code or assured name, or pick a tab (Outstanding, Paid Not Extracted, On Hold, Locked, Direct Payment).',
        'Filters narrow the list by insurer, name of assured, account officer, booking date and inception date.',
        'The Invoice Family tab lists the original booking with its endorsements and cancellation under one root invoice number, with the family totals (DIS 3.27.2).',
      ],
      controls: [
        'Balances are booked + adjusted - applied + reversed - remitted - written off, per component.',
        'A locked invoice can only receive payments until the module holding the lock releases it (RMTID.040).',
        'Direct payment invoices have no client receivable: payment and remittance status are Not Applicable.',
      ],
    },
    {
      name: 'Disbursement Queue',
      path: '/operations/disbursements',
      summary:
        'Payment requests sent by Remittance, Cashiering and Commission (remittances, refunds, BIR 2307 reports, incentive pass-on), worked here until the Disbursement system is connected (OQ02).',
      workflow: [
        'Acknowledge a request, assign its DV number, then mark it paid; or return it with a reason.',
      ],
      controls: [
        'Every step is audited and reported back to the sending module and its sender.',
        'A request is sent once per source transaction.',
      ],
    },
    {
      name: 'Hand-offs and Extracts',
      path: '/operations/handoffs',
      summary:
        'Work handed over while a module is not active (an OR to issue, an unapplied item to set up), and the extract repository that stands in for the shared-drive folders.',
      workflow: ['Do the work by hand, then close the hand-off with what was done.'],
      controls: ['Cashiers and interface administrators close hand-offs; closing is audited.'],
    },
    {
      name: 'Interfaces',
      path: '/operations/interfaces',
      summary:
        'Feeds with Collection, Disbursement, insurers and Booking: transport, schedule, activation, file uploads and the log of every run with the outcome of each record (BRQID.004/005/006).',
      workflow: [
        'Configure a feed schedule or deactivate it; upload a file for a feed whose module processes uploads.',
        'Open a run to see accepted, duplicate and failed records. Replay Booked Invoices copies invoices missing from the ledger.',
      ],
      controls: [
        'Each record is processed once per key; a failed record does not stop the run and can be sent again.',
        'Failed or partial runs raise the OPS_FLOW_IN_FAILED alert and notify the interface administrators.',
      ],
    },
    {
      name: 'Report Archive',
      path: '/operations/report-archive',
      summary:
        'Operations reports run on screen or exported, with parameters, time and user (CSHID.017/018).',
      controls: ['Viewing needs OPS_REPORT_VIEW; downloading or printing needs OPS_REPORT_EXPORT.'],
    },
    {
      name: 'Notification Settings',
      path: '/operations/notifications',
      summary:
        'Choose, per Operations event, whether you receive the in-app notification and the e-mail (RMTID.034).',
    },
  ],
};

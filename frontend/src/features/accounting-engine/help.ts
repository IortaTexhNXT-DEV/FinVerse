import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Accounting Engine screens (listed in the help centre). */
export const ACCOUNTING_ENGINE_HELP: HelpSection = {
  id: 'accounting-engine',
  module: 'Accounting Engine',
  intro: 'How business events (policies, claims, receipts…) become journals automatically.',
  screens: [
    {
      name: 'Event Types',
      path: '/accounting/event-types',
      summary:
        'Business events published by the operational modules (policy issue, claim settlement, premium receipt…), with their category, journal type and amount components. Each needs at least one authorized rule for the company before it can post.',
    },
    {
      name: 'Accounting Rules',
      path: '/accounting/rules',
      summary:
        'Each business event type is mapped to debit/credit accounts by an accounting rule (per business line and currency, by priority and effective dates). The engine picks the matching authorized rule with the lowest priority number.',
      controls: [
        'New and changed rules must be authorized by another user; they appear in the approval inbox.',
      ],
    },
    {
      name: 'Rule Simulator',
      path: '/accounting/simulator',
      summary:
        'Preview the journal a sample event would produce with the rules in force. Nothing is posted.',
    },
    {
      name: 'Event Register',
      path: '/accounting/events',
      summary:
        'Every business event received by the engine with its status, the journal it posted (open it by batch number) or the error that stopped it.',
      workflow: [
        'Filter by value date, event type or status; failed events show why no journal was posted.',
        'Fix the cause (usually a missing or unauthorized rule) and repeat the business action, e.g. approve the document again.',
      ],
      controls: [
        'A failed event rolls back the business transaction that raised it, so no document is approved without its journal.',
      ],
    },
  ],
};

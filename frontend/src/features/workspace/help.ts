import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the My Work screens. */
export const WORKSPACE_HELP: HelpSection = {
  id: 'workspace',
  module: 'My Work',
  intro:
    'The broking work queues: quotations, proposal requests and accounts waiting for your team, with their service level.',
  screens: [
    {
      name: 'My Work',
      path: '/my-work',
      summary:
        'Every item in the stages your team works (Marketing, TSU, Processing, E-policy, Adjustment), oldest due first, with open, overdue and assigned-to-me counts per stage.',
      workflow: [
        'Click a stage tile to see only that stage; use the tabs for your own items, the unassigned team queue or everything.',
        'Claim an unassigned item to take it over, then open it to act on it.',
        'Team leaders assign or re-assign items to a user of the team, or return them to the team queue.',
      ],
      controls: [
        'You only see the stages your role works; the stage defines the team (workflow configuration).',
        'Every stage change is recorded with user, time, reason and comment in the record’s status history.',
        'Items past their stage service level show as overdue and raise the WORK_SLA_BREACH alert in the daily checks.',
        'A record returned to Marketing goes back to the account officer who created it.',
      ],
    },
  ],
};

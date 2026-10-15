import type { HelpScreen } from '@/features/help/helpContent';

/** Help of the Broking Administration screens (part of the Broking Setup section). */
export const NBADMIN_HELP_SCREENS: HelpScreen[] = [
  {
    name: 'Lists of Values',
    path: '/broking-setup/lists',
    summary:
      'Coded values used across the broking screens: market segments, reasons, document types, client tags, instruction types, KYC checklists and more. Each value has an order and an effective period.',
    workflow: [
      'Choose a list on the left; its values show on the right with their effectivity and status.',
      'New value or Edit: enter the code, label, order and effective dates. Deactivate a value that is no longer used; existing records keep it.',
      'A checker opens the list from My Approvals and authorizes the pending values.',
    ],
    controls: [
      'Maker-checker: new and changed values are pending until another user with MASTER_AUTHORIZE authorizes them; pending values show a badge.',
      'Lists marked System list are maintained by the application only.',
      'Every change and authorization is recorded in the audit trail.',
    ],
  },
  {
    name: 'Data Retention',
    path: '/broking-setup/retention',
    summary:
      'Retention rules by record type and status (5 years online, 15 years archive) and the records eligible at the latest review, with a drill-down list.',
    workflow: [
      'Run review now counts the eligible records of every active rule; a scheduled job does it every month.',
      'Records opens the list of eligible records; Edit changes the years, statuses, action or activation of a rule.',
    ],
    controls: [
      'Nothing is archived or deleted: archiving waits for the archive storage and backup decision.',
      'Record types whose module does not report candidates yet show as such and are counted once it does.',
      'Rule changes and review runs are audited.',
    ],
  },
];

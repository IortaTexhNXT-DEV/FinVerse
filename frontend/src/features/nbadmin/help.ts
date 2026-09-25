import type { HelpScreen, HelpSection } from '@/features/help/helpContent';

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
    name: 'Access Requests',
    path: '/broking-setup/access-requests',
    summary:
      'Requests of the Business Administrator to create a user, change the roles of a user, disable or enable a user, or change the permissions of a role, each with a justification.',
    workflow: [
      'New request: choose the request type and the user, the roles and home branch for a new user, and give the justification.',
      'The Approver opens the request from My Approvals and approves it (the change is applied at once) or rejects it with a comment.',
      'For a new user, the approval shows a temporary password once; hand it over securely.',
      'Change role permissions: pick the role, tick the permissions it should hold (grouped by area) and check the added / removed summary.',
    ],
    controls: [
      'Four eyes: the requester cannot decide their own request.',
      'Only one open request per user or role at a time; the requester is notified of the decision.',
      'Requests, decisions and the applied change are recorded in the audit trail.',
    ],
  },
  {
    name: 'User Access Matrix',
    path: '/broking-setup/access-matrix',
    summary:
      'The agreed user access matrix: every role against every permission as granted today, with the number of enabled users per role, and the role-to-action view by area and action class (view only, create, amend, approve). Export both views to Excel for sign-off.',
    workflow: [
      'By Permission: one row per permission with its area and action class.',
      'By Action: one row per area and action class; each cell lists the permissions the role holds.',
    ],
    controls: [
      'Read-only; role permissions change through access requests (Change role permissions), decided by another user.',
      'The export is recorded in the audit trail.',
    ],
  },
  {
    name: 'Data Retention',
    path: '/broking-setup/retention',
    summary:
      'Retention rules by record type and status (5 years online, 15 years archive) and the records eligible at the latest review, with a drill-down list.',
    workflow: [
      'Run review now counts the eligible records of every active rule; the RETENTION_REVIEW job does it every month.',
      'Records opens the list of eligible records; Edit changes the years, statuses, action or activation of a rule.',
    ],
    controls: [
      'Nothing is archived or deleted: archiving waits for the archive storage and backup decision.',
      'Record types whose module does not report candidates yet show as such and are counted once it does.',
      'Rule changes and review runs are audited.',
    ],
  },
];

/** In-app help of the User Access section (BRD-11 User Access Maintenance). */
export const USER_ACCESS_HELP: HelpSection = {
  id: 'user-access',
  module: 'User Access',
  intro:
    'User Access Maintenance: every change to a user or a group profile (role) is made through an approved request with a chosen approver and four eyes; each applied change is kept attribute by attribute in the access change log with its request number, who did it and who approved it.',
  screens: [
    {
      name: 'Access Request Queues',
      path: '/user-access/requests',
      summary:
        'Your access request queues: drafts, returned requests, requests assigned to you, second approvals and approved group-profile requests to implement. Open Access Requests and the User Access Matrix from the page header.',
      workflow: [
        'A requester enrolls, modifies, deactivates or reactivates a user, or asks for a group-profile change, and chooses the approver.',
        'The approver approves, returns with remarks or rejects; a privileged or out-of-hours change also needs a second approval (UAM_WORKING_HOURS).',
      ],
      controls: [
        'The requester and the user a request is about never decide it.',
        'A deactivated group profile grants nothing to its members until it is reactivated.',
        'Roles are edited directly on the Roles screen only through the emergency path UAM_DIRECT_ROLE_EDIT; every such edit is audited and raises the UAM_DIRECT_ROLE_EDIT alert.',
      ],
    },
  ],
};

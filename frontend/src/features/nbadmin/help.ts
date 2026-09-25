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
      name: 'Access Requests',
      path: '/user-access/requests',
      summary:
        'Requests to enrol, modify, deactivate and reactivate users (internal BIBS users, or portal users of insurers and clients), by tab: My Requests, Assigned to Me, Second Approval, For Implementation and All. A row opens the request with its current and requested values, approvers and history.',
      workflow: [
        'New Request: choose the type, the user and the new data or group profiles, an optional effective date, the approver and the remarks. Save Draft keeps it for later (only you see it); Submit runs the full checks and notifies the approver.',
        "The approver opens the request from My Approvals or Assigned to Me and clicks Approve and Apply, Return (remarks mandatory) or Reject (reason mandatory). A new user's temporary password is shown once.",
        'A returned request is corrected with Edit Request and resubmitted with a correction remark; the history keeps every round.',
        'A request with an effective date is SCHEDULED after approval and applied by the daily job UAM_EFFECTIVE_CHANGES on that date.',
        'Cancel Request (reason mandatory) withdraws a draft, pending, returned or scheduled request; the approver is told.',
      ],
      controls: [
        'The requester and the user a request is about never decide it; only the chosen approver decides unless UAM_ANY_APPROVER is true.',
        'A change that raises a user to a High or Admin group profile, or that is submitted or approved outside UAM_WORKING_HOURS, needs a second approval by another approver (UAM_SECOND_APPROVE).',
        'User IDs of new users follow USER_ID_PATTERN; one open request per user; every step is in the history and the audit trail.',
      ],
    },
    {
      name: 'Group Profile Requests',
      path: '/user-access/group-profiles',
      summary:
        'Requests to create, change, deactivate and reactivate group profiles (roles), with the permissions picked by area and action class, decided by one or more approvers in order and implemented by the System Administrator.',
      workflow: [
        'New Group Profile Request: choose the type and the profile, pick the permissions, the name and the privilege level, add the approvers in order and submit.',
        'Each approver decides in turn; a return restarts the chain with the first approver after the correction.',
        'After the last approval the request is FOR_IMPLEMENTATION: the System Administrator opens it (tab For Implementation, or Implement Request on Roles & Permissions) and implements it; the request becomes IMPLEMENTED.',
      ],
      controls: [
        'The implementer is never the requester. A deactivated profile grants nothing to its members until it is reactivated; SYSADMIN cannot be deactivated.',
        'Roles are edited directly on the Roles screen only through the emergency path UAM_DIRECT_ROLE_EDIT (off by default); every such edit is audited and raises the UAM_DIRECT_ROLE_EDIT alert.',
        'With UAM_ROLE_APPLY_ON_APPROVAL true the change applies at the last approval (UQ03).',
      ],
    },
    {
      name: 'Bulk Request',
      path: '/user-access/bulk',
      summary:
        'Many user requests in one file (template UAM_ACCESS_REQUEST): one row per user with the action ENROL, MODIFY, DEACTIVATE or REACTIVATE. Every row is checked as a single request before the batch is created.',
      workflow: [
        'Download the template, fill one row per user and upload it; correct the rows reported and upload again.',
        'Commit the valid rows: they become the draft lines of a batch. Open the batch, choose the approver, enter the remarks and submit.',
        'The approver approves the batch: each line is applied on its own; a line that fails is listed with its reason and the others stand.',
      ],
      controls: [
        'Each line follows every rule of a single request (user ID format, one open request per user, four eyes).',
        'The batch is decided as a whole (UQ10); temporary passwords of new users are shown once to the approver.',
      ],
    },
    {
      name: 'User Access Matrix',
      path: '/user-access/matrix',
      summary:
        'The agreed user access matrix: every role against every permission as granted today, with the number of enabled users per role, and the role-to-action view by area and action class (view only, create, amend, approve). Export both views to Excel for sign-off.',
      workflow: [
        'By Permission: one row per permission with its area and action class.',
        'By Action: one row per area and action class; each cell lists the permissions the role holds.',
      ],
      controls: [
        'Read-only; group profiles change through group-profile requests decided by another user.',
        'The export is recorded in the audit trail.',
      ],
    },
  ],
};

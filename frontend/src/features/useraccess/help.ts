import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the User Access section (BRD-11 User Access Maintenance). */
export const USER_ACCESS_HELP: HelpSection = {
  id: 'user-access',
  module: 'User Access',
  intro:
    'UIDM-ISC creates, changes, deactivates and reactivates the users and assigns their group profiles over the provisioning interface; Microsoft Entra ID signs them in. This section keeps the definitions of the group profiles (every change proposed by a maker and approved by another user), the data access of each user, unlock and session end, identity synchronisation and reconciliation, separation of duties, the reports, the certification extract, the audit log and the UAM settings.',
  screens: [
    {
      name: 'Users',
      path: '/access/users',
      summary:
        'The users received from UIDM-ISC with their User ID, Windows ID, name, business unit, status, group profiles, last sign-in and lock, filtered by status, group profile and business unit and exported as CSV.',
      workflow: [
        'A row opens the user: Profile (as UIDM-ISC sent it), Group Profiles (with the source and the date each was given), Effective Permissions, Data Access, Sessions and Access History.',
        'Data Access sets the companies and branches the user may act for; the change applies at once.',
        'Unlock lets a locked local test account sign in again; End Sessions signs the user out on every server. Both ask for a confirmation.',
      ],
      controls: [
        'Users and their group profiles cannot be changed here: UIDM-ISC maintains them.',
        'Every change of a user is kept attribute by attribute in the access change log, with the UIDM-ISC request number or the user who made it.',
      ],
    },
    {
      name: 'Group Profiles',
      path: '/access/group-profiles',
      summary:
        'Every group profile with its code, name, privilege level, members, status and the change waiting for approval. A row opens the definition, the permissions by area, the members (from UIDM-ISC, read-only), the pending change and the history.',
      workflow: [
        'New Group Profile, Propose Change, Propose Deactivation and Propose Reactivation start a change: enter the definition and permissions and the reason, then Save Draft or Submit for Approval.',
        'A checker opens the Pending Change tab and approves, returns for correction or rejects; a returned change is corrected by its maker and submitted again.',
        'A High or Admin profile, or a change outside the working hours, needs a second approval; an approved change applies at once unless the setting asks for its implementation by the System Administrator.',
      ],
      controls: [
        'The maker never approves the change; any other holder of the approval right may.',
        'Changes that would break a separation-of-duties rule are refused.',
      ],
    },
    {
      name: 'Identity Sync',
      path: '/access/identity-sync',
      summary:
        'The connection with UIDM-ISC (last call, last success, errors of the last 24 hours), the events received with their outcome, the refused events to retry or dismiss, and the reconciliation mismatches to resolve.',
      workflow: [
        'Synchronise Now compares every user with the directory at once and shows the result; the reconciliation also runs on its schedule.',
        'Refused: Retry processes an event again once UIDM-ISC corrected the data; Dismiss closes it with a remark.',
        'Reconciliation: resolve a mismatch with a remark once UIDM-ISC corrected the user.',
        'The Simulator tab is shown only where the Enterprise SSO simulator is on (SIT and UAT).',
      ],
      controls: [
        'Nothing is corrected here: UIDM-ISC corrects the user with an event.',
        'Every dismissal and resolution is in the audit log with its remark.',
      ],
    },
    {
      name: 'Separation of Duties',
      path: '/access/separation-of-duties',
      summary:
        'The rules of group profiles or permissions one user may not hold together, and the users who breach an active rule.',
      workflow: [
        'New Rule proposes a rule; Information Security authorises or rejects it. Deactivate proposes its removal.',
        'Violations lists the users who breach an active rule, with the export; ask UIDM-ISC to correct their group profiles.',
      ],
      controls: [
        'A daily check records each breach and tells Information Security of the new ones.',
      ],
    },
    {
      name: 'Access Matrix',
      path: '/access/matrix',
      summary:
        'The group profiles against the permissions, and by area and action class, with the export to Excel.',
    },
    {
      name: 'Reports & Certification',
      path: '/access/reports',
      summary:
        'The user access reports and the certification extract of every account, group profile and permission (CSV or XLSX).',
      controls: [
        'Each extract is logged with who took it, when, the rows and the SHA-256 of the file, and Information Security is told.',
      ],
    },
    {
      name: 'Audit Log',
      path: '/access/audit',
      summary:
        'The audit trail of the user access events: users, sessions, group profiles and their changes, separation of duties, identity synchronisation, certification extracts and the security settings.',
    },
    {
      name: 'UAM Settings',
      path: '/access/settings',
      summary:
        'Session timeout and warnings, dormancy, the lock-out and passwords of the local test accounts, the provisioning and reconciliation, and the group-profile switches.',
      controls: [
        'Every change waits until another user with the approval right approves it; the maker may withdraw it.',
        'Local passwords serve only the test accounts outside production; the bank users sign in at Microsoft Entra ID.',
      ],
    },
  ],
};

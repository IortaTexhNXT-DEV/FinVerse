import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Administration screens (listed in the help centre). */
export const ADMIN_HELP: HelpSection = {
  id: 'admin',
  module: 'Administration',
  intro: 'Security, configuration and monitoring (administrators).',
  screens: [
    {
      name: 'Users',
      path: '/admin/users',
      summary:
        'User accounts with their Windows ID, group profiles, business unit, user level, status (Active, Disabled, Locked), last sign-in and authorization limit: the base-currency amount a user may approve on journals, payments and claims (blank = unlimited).',
      workflow: [
        'Search by user ID, name or Windows ID and filter by status.',
        'Raise Request opens a new access request; a row opens a Modify user request for that user. Users are enrolled and changed only through approved requests.',
      ],
      controls: [
        'Accounts lock after the number of failed sign-ins set in the parameter LOGIN_MAX_FAILED_ATTEMPTS (a wrong code of the second factor counts too); an administrator unlocks them or resets the password.',
        'The direct create and edit of users is kept for the System Administrator while the emergency path UAM_DIRECT_ROLE_EDIT is open.',
        'Every change to a user is recorded in the audit trail and the access change log.',
      ],
    },
    {
      name: 'Roles & Permissions',
      path: '/admin/roles',
      summary:
        'The permissions of each group profile (role) with its privilege level and active flag. Keep makers, checkers, administrators and auditors in different roles (segregation of duties).',
      workflow: [
        'Approved Requests to Implement lists the approved group-profile requests: Implement Request applies the approved change and the request becomes IMPLEMENTED.',
        'While the emergency path UAM_DIRECT_ROLE_EDIT is open, tick or clear permissions in the matrix and save the role.',
      ],
      controls: [
        'A profile changes only by implementing an approved request; the implementer is never the requester.',
        'Every direct edit is audited and raises an alert; every change is in the access change log.',
      ],
    },
    {
      name: 'Second Factor',
      path: '/admin/second-factor',
      summary:
        'The authenticator app of each user (the second factor asked after the password): whether it is set up, when it was last used and whether the user holds a privileged role. Who must use it is the parameter MFA_POLICY (all users, privileged users or off).',
      workflow: [
        'Request Reset on a user whose phone is lost or replaced, with the reason.',
        'Another administrator approves or rejects the reset in the table of resets waiting for approval (or in My Approvals).',
        'Once approved, the app and the recovery codes are removed, the user is signed out and sets the app up again at the next sign-in.',
      ],
      controls: [
        'The requester never approves the reset; nobody approves the reset of their own second factor.',
        'Requests, approvals and rejections are recorded in the audit trail.',
      ],
    },
    {
      name: 'Audit Trail',
      path: '/admin/audit',
      summary:
        'Every financial and non-financial action with the time, user, action, record and details, filtered by date range, user and entity type.',
      controls: [
        'The audit trail is insert-only: entries cannot be changed or deleted, neither from the application nor directly in the database (database triggers reject every change or deletion).',
        'An entry is written in the same transaction as the change it describes.',
      ],
    },
    {
      name: 'System Parameters',
      path: '/admin/parameters',
      summary:
        'Business parameters such as session timeout, ageing buckets, report footer and suspense accounts, plus a read-only view of the runtime configuration.',
      controls: [
        'Values are validated by type and every change is audited.',
        'A change of a security setting (sign-in, password, lock-out, session and access settings, the emergency direct edit of roles among them) waits until Information Security approves it; the user who requested it cannot approve it.',
      ],
    },
    {
      name: 'Exception Codes',
      path: '/admin/exception-codes',
      summary: 'Severity, threshold amount/days and activation of each monitored exception.',
    },
    {
      name: 'Scheduled Jobs',
      path: '/admin/jobs',
      summary: 'Background jobs with schedule, last and next run, run history and "Run now".',
      workflow: [
        'Daily jobs as delivered: RECURRING_JOURNALS (recurring and accrual journals), ALERT_DAILY_CHECKS (exception codes), PDC_ISSUED_DUE (post-dated cheques issued whose date is reached become due) and QUOTATION_EXPIRY (lapsed quotations become expired).',
        'Manual by default: RESERVE_VALUATION and RI_ALLOCATION; the schedules are configured by the administrator of the installation.',
      ],
      controls: ['A failed run raises a JOB_FAILURE alert.'],
    },
    {
      name: 'Integration Events',
      path: '/admin/integration-events',
      summary:
        'Business events sent to Kafka through the transactional outbox (invoices booked, payments applied, receipts issued, remittance and disbursement status, collection feeds, product versions, clients, e-mail requests), with the events a consumer could not process.',
      workflow: [
        'Dead Letters: read the error, fix the cause, then Retry (the event is published again to its topic) or Discard it.',
        'Outbox: a FAILED event (Kafka did not acknowledge it after all attempts) can be sent again with Send Again.',
        'Topics: the catalogue of topics, their event types and dead-letter topics.',
      ],
      controls: [
        'System Administrator only; retries and discards are logged with the user.',
        'Consumers ignore an event they already processed (event id), so a retry never processes it twice.',
      ],
    },
    {
      name: 'Application Info',
      path: '/admin/info',
      summary: 'Version, build, database migration level and health of the installation.',
    },
  ],
};

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
        'User accounts with their roles, status, last sign-in and authorization limit: the base-currency amount a user may approve on journals, payments and claims (blank = unlimited).',
      controls: [
        'Accounts lock after five failed sign-ins; an administrator unlocks them or resets the password.',
        'Every change to a user is recorded in the audit trail.',
      ],
    },
    {
      name: 'Roles & Permissions',
      path: '/admin/roles',
      summary:
        'The permissions of each role. Keep makers, checkers, administrators and auditors in different roles (segregation of duties).',
      workflow: ['Tick or clear permissions in the matrix, then save the role.'],
      controls: ['Every permission change is recorded in the audit trail.'],
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
      controls: ['Values are validated by type and every change is audited.'],
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

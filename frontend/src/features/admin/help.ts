import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Administration screens (listed in the help centre). */
export const ADMIN_HELP: HelpSection = {
  id: 'admin',
  module: 'Administration',
  intro: 'Security, configuration and monitoring (administrators).',
  screens: [
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
        'A change of a security setting waits until Information Security approves it; the user who requested it cannot approve it.',
        'The user access settings (sessions, dormancy, local test accounts, synchronisation, group-profile switches) are maintained on User Access › UAM Settings.',
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
      name: 'Caches',
      path: '/admin/caches',
      summary:
        'The reference-data caches of the platform (parameters, lists of values, organisation, roles) with how long each entry is kept, where it is kept and which changes clear it.',
      workflow: [
        'After a change made outside the application (for example a correction in the database by the support team), choose Clear Cache in the row menu of the cache, or Clear All Caches.',
      ],
      controls: ['Clearing a cache drops its data on every instance; nothing else changes.'],
    },
    {
      name: 'Record Classes',
      path: '/admin/record-classes',
      summary:
        'Retention, legal hold and archiving of the stored documents by record class (policy documents, claims files, reports and the others).',
      workflow: [
        'The records hold approver chooses Change Settings in the row menu to change the retention, the legal hold of new files, the archiving of final files or whether the class accepts new files.',
      ],
      controls: ['Every change is kept in the audit trail.'],
    },
    {
      name: 'Legal Holds',
      path: '/admin/legal-holds',
      summary:
        'Requests to place or release the legal hold of a stored document, and their approval. A document under legal hold is neither deleted nor purged at the end of its retention.',
      workflow: [
        'The records hold officer looks up the document by its file number and requests the hold or its release with a reason.',
        'The records hold approver approves or rejects each waiting request from the row menu, with a note.',
      ],
      controls: ['The requester never approves their own request.'],
    },
    {
      name: 'Content Migration',
      path: '/admin/content-migration',
      summary:
        'Progress of the copy of the documents kept in the database into the file store, by area: files, copied, remaining and the status of each area.',
      workflow: ['The copy runs as a scheduled job; Refresh shows the latest counts.'],
      controls: ['An area is complete when no file remains to copy.'],
    },
    {
      name: 'Product Modules',
      path: '/admin/modules',
      summary:
        'The modules of the product in use in this deployment (New Business, Operations, Collections, Renewal and the others) and the module profiles, such as the insurance broker profile.',
      workflow: [
        'The System Administrator chooses Switch Off or Switch On in the row menu of a module, or Apply Profile in the row menu of a profile, and gives the reason.',
        'Another user with the approval right approves or rejects the change from the row menu; the requester may withdraw it.',
      ],
      controls: [
        'A switched-off module has no menu, its screens and reports are not available, its permissions grant nothing and its scheduled jobs do not run; its records are kept.',
        'A module another module needs cannot be switched off while that module is on.',
      ],
    },
    {
      name: 'Role Home Set-up',
      path: '/admin/role-homes',
      summary:
        'The widgets of the home page (Home > Dashboard) of each role, in display order: headline figures, my workload, collections, payables, cash position, budget and the income and expense charts.',
      workflow: [
        'The System Administrator chooses Change Widgets in the row menu of a role, ticks the widgets, orders them with the arrows and gives the reason.',
        'Another user with the approval right approves or rejects the change from the row menu; the requester may withdraw it.',
      ],
      controls: [
        'A role not set up shows the delivered widgets. A user with several roles sees the widgets of the roles that are set up, each once, in the order of the first role that shows it.',
        'A widget of a switched-off product module is not shown, and a widget never counts a record its list would not show the user.',
        'Every request, approval and rejection is recorded in the audit trail.',
      ],
    },
    {
      name: 'Integration Events',
      path: '/admin/integration-events',
      summary:
        'Business events sent to Kafka through the transactional outbox (invoices booked, payments applied, receipts issued, remittance and disbursement status, collection feeds, product versions, clients, e-mail requests), with the events a consumer could not process.',
      workflow: [
        'Dead Letters: read the error, fix the cause, then Retry (the event is published again to its topic) or Discard it.',
        'Outbox: a FAILED event (Kafka did not acknowledge it after all attempts) can be sent again with Send Again.',
        'Event Archive: every event published, searched by topic, event key or correlation ID; View Event shows the full event.',
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

import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Legacy Inquiry (BRD-13), in sidebar order. */
export const LEGACY_INQUIRY_HELP: HelpSection = {
  id: 'legacy-inquiry',
  module: 'Legacy Inquiry',
  intro:
    'The legacy archive keeps the closed transactions and history of the legacy systems, with their documents, after the systems are decommissioned. It is read-only; every access is logged.',
  screens: [
    {
      name: 'Legacy Inquiry',
      path: '/legacy-inquiry',
      summary:
        'Search the archive by client, policy or cover, invoice, receipt or claim number, record type, legacy system and date range; open a record to read all its legacy columns and download its documents; export the result to Excel.',
      workflow: [
        'Give the reason of the inquiry once per session, then search.',
        'Open a record for its legacy details and documents; download a document or export the list.',
      ],
      controls: [
        'A reason is required when the setting asks for it; it is written to the access log with each search, view, download and export.',
        'The export is limited to the configured number of records and needs the export permission.',
        'Exporting more records in a day than the alert limit raises an unusual-access alert to Compliance.',
      ],
    },
    {
      name: 'Access Log',
      path: '/legacy-inquiry/access-log',
      summary:
        'Every access to the legacy archive with the user, time, source address, criteria, records and reason, filtered by user, action and period. A monthly digest is sent to the reviewers.',
      controls: ['The log cannot be changed or deleted, even by an administrator.'],
    },
  ],
};

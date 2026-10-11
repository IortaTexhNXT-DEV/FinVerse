import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Bulk Processing screens. */
export const BULK_HELP: HelpSection = {
  id: 'bulk',
  module: 'Bulk Processing',
  intro: 'Create or update many quotations, clients, accounts or bookings from one file.',
  screens: [
    {
      name: 'Bulk Uploads',
      path: '/bulk',
      summary:
        'The upload types available to your role and the history of uploads with their counts and result reports.',
      workflow: [
        'Choose an upload type and download its template: one Excel sheet that says what the file is for, who fills it in and its rules, with a guide above every column (mandatory, format, allowed values, what to enter), drop-downs and an example row.',
        'Fill in your rows below the example row and upload the file as it is: the guide and the example row are skipped. OpenDocument (.ods) and CSV (.csv) files with the headers in the first row are also accepted.',
        'Review the validation of every row; correct the rows of the error file (same layout, with an Error column) and upload it again, or process the valid rows now.',
        'Processing creates or updates one record per valid row; rows that fail at that moment are reported with the reason.',
        'Download the result report (summary and every row with status, messages and the reference created).',
      ],
      controls: [
        'Only files that follow the current template are accepted (headers are checked).',
        'Values are cleaned (spaces, identifiers, Y/N) before validation; mandatory values, types and duplicates inside the file are checked for every row.',
        'Each row is processed in its own transaction: one failing row never blocks the others.',
        'Uploads, their rows and outcomes are kept for audit; the maximum number of rows per file is the BULK_MAX_ROWS parameter.',
      ],
    },
  ],
};

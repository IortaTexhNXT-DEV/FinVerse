import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Broking Setup screens. */
export const BROKING_SETUP_HELP: HelpSection = {
  id: 'broking-setup',
  module: 'Broking Setup',
  intro:
    'Masters and controls of the broking operation: templates, lists of values and the e-mail log.',
  screens: [
    {
      name: 'Document Templates',
      path: '/broking-setup/templates',
      summary:
        'Texts merged into generated quotations, quotation and proposal slips, placement slips, hold cover requests, insurance advice, e-policy e-mails and service invoices.',
      workflow: [
        'Choose a template to see its text in force and its version history.',
        'New version: edit the title and text, keep the {{placeholders}}, and choose the date from which it applies.',
      ],
      controls: [
        'Versions are never changed or deleted; each generated document records the template version it used.',
        'Every new version is recorded in the audit trail.',
      ],
    },
    {
      name: 'Outbound Messages',
      path: '/broking-setup/messages',
      summary:
        'Log of every e-mail sent or attempted: recipients, subject, attachments with checksum and protection, attempts, outcome and the reason of failures.',
      workflow: [
        'Filter by status or search by recipient, subject or reference.',
        'Open a failed e-mail to see the reason and send it again once the cause is fixed.',
      ],
      controls: [
        'Protected documents are encrypted (PDF AES-256, Excel agile encryption); the password travels in a separate e-mail whose content is never displayed.',
        'Deliveries are retried automatically (MAIL_MAX_ATTEMPTS) by the MAIL_DISPATCH job.',
        'Without a configured mail server, deliveries are simulated and shown as such.',
      ],
    },
  ],
};

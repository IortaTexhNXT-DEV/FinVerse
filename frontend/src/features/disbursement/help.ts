import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Disbursement screens (DIS 2.2-3.28). */
export const DISBURSEMENT_HELP: HelpSection = {
  id: 'disbursement',
  module: 'Disbursement',
  intro:
    "Disbursement pays what the other units request: remittances to insurers, client refunds, suppliers, government agencies, employees and service fees. Each payment is a disbursement voucher (DV) with a proforma entry, reviewed and approved before it posts, and then followed by its instrument (check, ATD, credit to account, manager's check, credit ticket, telegraphic transfer, online banking).",
  screens: [
    {
      name: 'Disbursement Workbench',
      path: '/disbursement',
      summary:
        'Payment requests and DVs by stage: system requests, in process, for review, for approval, approved and cancelled.',
      controls: [
        'Refunds to clients and remittances to insurers with a maintained payee go straight to the approver (DIS 3.25.0, DISB_AUTO_APPROVER_ROUTING).',
        'The approver of a DV is never its processor; cancelling an approved DV reverses its entry and returns the request to its source (DIS 2.20.0).',
      ],
    },
  ],
};

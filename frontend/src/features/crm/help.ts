import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Clients screens. */
export const CRM_HELP: HelpSection = {
  id: 'crm',
  module: 'Clients',
  intro:
    'Prospects and confirmed clients: onboarding with KYC verification, duplicate prevention, tags and special instructions, the client 360 view and periodic KYC reviews.',
  screens: [
    {
      name: 'Clients',
      path: '/crm/clients',
      summary:
        'Search clients by prospect or client code, name, TIN, ID number, e-mail, mobile, status, KYC status, segment or bank relationship, and open the complete client record. Quick filters show prospects, confirmed clients and KYC reviews due.',
      workflow: [
        'Open a client to see its details, KYC documents, tags and special instructions, linked records and history.',
        'The client page shows the onboarding stage: Prospect, KYC for verification, KYC verified, Confirmed or Inactive.',
        'Account Officer: upload the mandatory KYC documents, then Submit KYC. Checker (Marketing TL): Verify KYC. Then Confirm client.',
        'Bulk upload creates or updates many clients at once (handler CLIENT_CREATE): a row with a code updates that client, otherwise the client matching its TIN, ID or name and birth date, else a prospect is created.',
      ],
      controls: [
        'Four eyes: the KYC is verified by a user other than the one who created the client or submitted the KYC.',
        'Submission and confirmation are blocked while mandatory KYC documents (lists KYC_DOCS_INDIVIDUAL / KYC_DOCS_CORPORATE) or the minimum client information (parameters CLIENT_MIN_FIELDS_*) are missing.',
        'Confirmation issues the client code CL-yyyy-nnnnnn (the prospect code is kept) and opens the client sub-ledger party, authorized by the system, so the client can carry receivables.',
        'Linked Records flags missing linkages, such as a confirmed client without a party.',
        'Tags (VIP, Do not call...) and special instructions show as a banner on the client page and on the client quotations and accounts; every change is kept in the change history.',
      ],
    },
    {
      name: 'New Client',
      path: '/crm/clients/new',
      summary:
        'Create a client in three sections: Identity, Contact & Address, Segment & Bank relationship. Only the type and name are needed to save a prospect; the success screen shows the prospect code.',
      workflow: [
        'Choose Individual or Corporate and enter the name.',
        'Enter the TIN (000-000-000-000), ID, e-mail and mobile (09xxxxxxxxx or +639xxxxxxxxx); possible duplicates appear as you type.',
        'Save as prospect, then upload the KYC documents on the client page.',
      ],
      controls: [
        'Saving is blocked when the TIN, the ID or the name with birth date belongs to an existing client; the blocked attempt is recorded in the audit trail.',
        'Individual policyholders must be at least the minimum age (parameter CLIENT_MIN_AGE, 18) and the birth date must be in the past.',
        'Coded fields use the lists of values (market segment, ID type, nationality, source of funds, risk rating).',
      ],
    },
    {
      name: 'KYC Reviews Due',
      path: '/crm/kyc-reviews',
      summary:
        'Clients whose periodic KYC review is overdue (expired) or due within the review window, by default the non-bank clients. Download as Excel or PDF (report NB-KYC-DUE) or print.',
      workflow: [
        'Filter by due date, bank relationship, risk rating or segment.',
        'Open a client, upload the refreshed KYC documents and ask a checker to Verify KYC: the next review date is set again.',
      ],
      controls: [
        'The monthly KYC_REVIEW_DUE job sets overdue KYC to Expired and notifies the Account Officers of the number of reviews due.',
        'Review cycle by risk rating: KYC_REVIEW_MONTHS (standard, 36) and KYC_REVIEW_MONTHS_HIGH_RISK (12); window KYC_DUE_WINDOW_DAYS (30).',
      ],
    },
  ],
};

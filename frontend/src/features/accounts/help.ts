import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Accounts & Placement screens. */
export const ACCOUNTS_HELP: HelpSection = {
  id: 'accounts',
  module: 'Accounts & Placement',
  intro:
    'New Business accounts: entry by Marketing, validation by Processing, then payment, placement, policy and booking. Each account has an Account Reference Number (ARN).',
  screens: [
    {
      name: 'Accounts',
      path: '/accounts',
      summary:
        'Search accounts by ARN, client, PN number, plate / conduction / engine / chassis number, location, product, insurer, status or period, with quick filters for your drafts, accounts returned to you, awaiting payment, FFY and direct payment.',
      workflow: [
        'Open an account to see its status, work case and history, and to act on it.',
        'Submit (Marketing) sends a complete account to Processing; Resubmit sends back a returned one.',
        'Validate (Processing) moves it to payment; direct payment accounts go straight to placement.',
        'Direct booking skips placement when the insurer already issued the policy (attach the policy copy or e-policy first).',
        'Bulk upload creates many draft accounts of one product from a template.',
      ],
      controls: [
        'An account cannot be submitted until every mandatory field and document of its product is present and the premium is rated.',
        'Risks already on a live account (same vehicle identifiers or location) are refused with the existing ARN; CTPL may share a vehicle with a comprehensive account.',
        'Accounts that meet a TSU rule, or packages above their TSI limit, need TSU clearance before validation.',
        'Every change, action and document is audited; voided accounts are hidden unless asked for.',
      ],
    },
    {
      name: 'New Account',
      path: '/accounts/new',
      summary:
        'Six steps: client, product, period and payment, risk items, contact and premium, then review with the completeness check, documents and submission.',
      workflow: [
        'Choose the client (a prospect is allowed for a draft) and the product; the draft is then saved every 30 seconds and gets its ARN.',
        'Enter the vehicles, locations with their insured items, or persons of the product line.',
        'In the review step, attach the required documents (choose the document type; several files at once), check the premium and submit.',
      ],
      controls: [
        'The premium is re-rated on the server at every save with the rates in force.',
        'A prospect must be confirmed as a client before Processing can validate the account.',
      ],
    },
    {
      name: 'FFY Register',
      path: '/accounts/ffy',
      summary: 'Accounts tagged Free First Year, with the FFY start and end dates.',
      workflow: [
        'Tag or cancel FFY from the account (Details tab); cancelling needs a reason.',
        'FFY can also be tagged in bulk (FFY tagging upload) by vehicle identifier.',
      ],
      controls: [
        'Only products marked FFY-eligible can be tagged; each tag and cancellation is audited.',
      ],
    },
    {
      name: 'Direct Payment',
      path: '/accounts/direct-payment',
      summary: 'Accounts whose premium the client pays directly to the insurer.',
      workflow: ['Tag or untag direct payment from the account (Details tab) while it is open.'],
      controls: [
        'Only products that allow direct payment can be tagged.',
        'On validation a direct payment account is released for placement without BDOI collection.',
      ],
    },
  ],
};

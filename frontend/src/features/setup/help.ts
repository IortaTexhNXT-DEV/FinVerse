import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Setup screens (listed in the help centre). */
export const SETUP_HELP: HelpSection = {
  id: 'setup',
  module: 'Setup',
  intro: 'Organisation and reference data.',
  screens: [
    {
      name: 'Companies',
      path: '/setup/companies',
      summary:
        'Legal entities, each with its own books: legal name, TIN, base currency, fiscal year start, back / forward value-date window and retained earnings account. Companies can be consolidated at group level.',
      controls: [
        'Maker-checker: a new or changed company is authorized by another user (MASTER_AUTHORIZE).',
        'The value-date window limits how far back or forward journals may be dated; the retained earnings account receives the year-end close.',
      ],
    },
    {
      name: 'Branches',
      path: '/setup/branches',
      summary: 'Branches, offices and customer centres with their weekly holidays.',
      controls: ['Maker-checker: changes appear in the authorizers’ approval inbox.'],
    },
    {
      name: 'Currencies & Rates',
      path: '/setup/currencies',
      summary:
        'Currencies and exchange rates by type (SPOT, CLOSING, AVERAGE, BUDGET) and date, quoted as base-currency units per one unit of foreign currency.',
      controls: [
        'Documents use the SPOT rate of their date; FX revaluation needs a CLOSING rate at the period end.',
      ],
    },
    {
      name: 'Dimensions',
      path: '/setup/dimensions',
      summary:
        'Financial dimensions used on journal lines and in reports: cost centres, lines of business, departments and profit centres.',
      controls: [
        'Deactivate a value instead of deleting it; inactive values can no longer be posted to.',
      ],
    },
    {
      name: 'Business Partners',
      path: '/setup/parties',
      summary:
        'Policyholders, agents, brokers, reinsurers, suppliers and banks used by the sub-ledgers.',
      controls: ['Maker-checker: new and changed partners must be authorized before use.'],
    },
    {
      name: 'Holiday Calendar',
      path: '/setup/holidays',
      summary:
        'Company-wide or branch holidays. Used for working-day checks such as the WEEKEND_POSTING exception.',
    },
  ],
};

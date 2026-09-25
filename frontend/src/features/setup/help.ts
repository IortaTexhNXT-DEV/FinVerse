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
      workflow: [
        'Monthly revaluation rates (FRBS 2.2.0): the GL Team Head enters the month-end rate per currency; it is the CLOSING rate of the last day of the month.',
        'The BOOK_RATE_FROM_CLOSING job copies it as the Operations BOOK rate of the next month (parameter OPS_BOOK_RATE_SOURCE); Copy to BOOK does it on demand.',
      ],
      controls: [
        'Documents use the SPOT rate of their date; FX revaluation needs a CLOSING rate at the period end.',
        'Revaluation rates need REVALUATION_RATE_MAINTAIN; an existing BOOK rate is never overwritten by the copy.',
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
        'Policyholders, agents, brokers, reinsurers, suppliers, banks and the payees of Disbursement (employees, government agencies, other payees) used by the sub-ledgers.',
      controls: ['Maker-checker: new and changed partners must be authorized before use.'],
    },
    {
      name: 'Employees',
      path: '/setup/employees',
      summary:
        'Employees with their branch and cost centre (DIS 3.30.1), for employee payments, cash advances and the Headcount per Cost Centre report (ORG-HEADCOUNT-CC).',
      workflow: [
        'Add the employee with number, branch and cost centre; link the payee party of type Employee for payments.',
        'A separation date makes the employee inactive; the history is kept.',
      ],
      controls: [
        'Needs EMPLOYEE_MAINTAIN; the cost centre must be an active cost-centre dimension.',
      ],
    },
    {
      name: 'Cost-Centre Rules',
      path: '/setup/cost-centre-rules',
      summary:
        'Standard rules that give generated journal lines their cost centre (FRBS 3.1.1): by source module, event type, branch, party and GL account, evaluated from the lowest priority.',
      workflow: [
        'Only lines of accounts that require a cost centre and have none are filled.',
        'When no rule matches, the posting is stopped, logged as FAILED in the event register and the COST_CENTER_MISSING alert is raised.',
      ],
      controls: ['Needs ACCOUNTING_RULE_MANAGE or MASTER_MAINTAIN; changes are audited.'],
    },
    {
      name: 'Bank Statement Layouts',
      path: '/setup/statement-layouts',
      summary:
        'Column layout of each bank account’s spreadsheet statement (FRBS 3.3.1) and the cheque number and amount matching rule (FRBS 3.3.2).',
      workflow: [
        'Map the date, description, cheque / reference and amount columns (one signed amount, or withdrawals and deposits) and the date pattern.',
        'Import Statement reads the .xlsx / .ods / .csv file with that layout and runs the automatic reconciliation at once.',
      ],
      controls: [
        'With cheque matching on, a bank line and a book entry with the same cheque number and amount match first, whatever their dates.',
        'Needs RECONCILIATION_MANAGE.',
      ],
    },
    {
      name: 'Holiday Calendar',
      path: '/setup/holidays',
      summary:
        'Company-wide or branch holidays. Used for working-day checks such as the WEEKEND_POSTING exception.',
    },
  ],
};

import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Tax & Statutory screens (listed in the help centre). */
export const TAX_HELP: HelpSection = {
  id: 'tax',
  module: 'Tax & Statutory',
  intro:
    'BIR, LGU and BFP returns computed from posted documents, BIR Form 2307 certificates and the Insurance Commission schedules.',
  screens: [
    {
      name: 'Tax Calendar',
      path: '/tax/calendar',
      summary:
        'Every form and period of the year with its due date, the return prepared for it and a due / overdue badge.',
      workflow: ['Prepare a missing return from its row, then file and pay it on Tax Returns.'],
      controls: [
        'Alerts TAX_RETURN_DUE and TAX_RETURN_OVERDUE are raised daily for tracked forms.',
        'Forms marked as reminders (e.g. 1601-C from payroll) raise no alerts.',
      ],
    },
    {
      name: 'VAT, Withholding, DST and Premium Tax Worksheets',
      path: '/tax/vat',
      summary:
        'Tax of a month or quarter from approved policies, endorsements, commissions and supplier invoices, reconciled with the tax accounts of the ledger.',
      workflow: [
        'Pick the period, review the return lines and the reconciliation difference.',
        'Open a source document from the drill-down table when a figure needs checking.',
        'Export the worksheet (PDF, Excel, CSV) or the BIR lists (SLS, SLP, QAP) and create the return.',
      ],
      controls: [
        'Payees without an authorized default ATC are listed as UNMAPPED and are not certified on 2307.',
      ],
    },
    {
      name: 'Tax Returns',
      path: '/tax/returns',
      summary: 'Returns register: DRAFT, FILED and PAID returns with their remittance.',
      workflow: [
        'Refresh a draft to recompute it from the worksheet.',
        'File it with the eFPS / eBIRForms reference, then pay it from a bank account.',
      ],
      controls: [
        'A return is filed by a user other than the one who prepared it.',
        'Paying posts the TAX_REMITTANCE journal that clears the tax payable (and applies input VAT).',
      ],
    },
    {
      name: 'BIR Form 2307',
      path: '/tax/2307',
      summary:
        'Certificates of creditable tax withheld per payee and quarter, generated in batches and printed as PDF.',
      controls: ['A payee holds one issued certificate per quarter; cancel it to re-issue.'],
    },
    {
      name: 'IC Statutory Schedules',
      path: '/tax/ic-schedules',
      summary:
        'Premiums, losses and commissions by line of business, net worth, RBC, reserves and investments from the ledger.',
    },
    {
      name: 'Tax masters',
      path: '/tax/codes',
      summary:
        'Tax codes and ATCs, filing forms, party tax profiles and the IC mapping, each subject to authorization.',
      controls: ['Maker-checker: changes appear in the authorizers’ approval inbox.'],
    },
  ],
};

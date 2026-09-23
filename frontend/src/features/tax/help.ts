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
      name: 'VAT Worksheet',
      path: '/tax/vat',
      summary:
        'Quarterly VAT (2550Q): output VAT on the net premium of approved policies and endorsements, input VAT on approved supplier invoices, reconciled with the VAT accounts of the ledger.',
      workflow: [
        'Pick the period, review the return lines and the reconciliation difference.',
        'Open a source document from the drill-down table when a figure needs checking.',
        'Export the worksheet (PDF, Excel, CSV) or the SLS and SLP lists and create the return.',
      ],
      controls: [
        'VAT payable = output VAT − input VAT − excess input VAT carried over from the previous quarter’s filed return.',
      ],
    },
    {
      name: 'Withholding Tax',
      path: '/tax/ewt',
      summary:
        'Expanded withholding tax (0619-E monthly, 1601-EQ quarterly): income payments and tax withheld per ATC and payee from supplier invoices and commissions, with the QAP export per quarter.',
      controls: [
        'Payees without an authorized default ATC are listed as UNMAPPED and are not certified on 2307.',
        'For a quarter, tax still due = total withheld − the monthly 0619-E returns filed or paid in the quarter.',
      ],
    },
    {
      name: 'Documentary Stamp Tax',
      path: '/tax/dst',
      summary:
        'Monthly DST (BIR 2000) on the policies and endorsements approved in the month, by line of business; return premiums reduce the month in which they are approved.',
    },
    {
      name: 'Premium Tax, LGT & FST',
      path: '/tax/premium-tax',
      summary:
        'Premium tax (2551Q) on business not subject to VAT, local government tax (quarterly, LGU) and fire service tax on fire premiums (monthly, BFP), from the levies of approved policies and endorsements.',
      controls: [
        'LGT is computed on the quarter’s premiums; enter the LGU assessment as a manual adjustment if it differs.',
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
      name: 'Tax Codes & Forms',
      path: '/tax/codes',
      summary:
        'VAT, premium tax and withholding tax codes (ATCs) with their rates and GL accounts, and the BIR, LGU and BFP forms with their frequency, due-date rule and payable account.',
      controls: [
        'Maker-checker: changes appear in the authorizers’ approval inbox (MASTER_AUTHORIZE).',
        'Forms marked as reminders are shown on the calendar but raise no alerts.',
      ],
    },
    {
      name: 'Party Tax Profiles',
      path: '/tax/profiles',
      summary:
        'TIN, registered name, individual name parts, VAT treatment and default ATC of suppliers, agents, brokers and customers, as printed on BIR lists and 2307 certificates.',
      controls: [
        'Maker-checker: a profile is used only once authorized.',
        'Payees without an authorized default ATC are listed as UNMAPPED on the worksheets.',
      ],
    },
    {
      name: 'IC Mapping',
      path: '/tax/ic-mapping',
      summary:
        'Which ledger accounts (account range or report group) feed each Insurance Commission schedule line, with the natural side, sign, measure (balance or movement) and the RBC factor.',
      controls: ['Maker-checker: mapping changes are authorized before the schedules use them.'],
    },
  ],
};

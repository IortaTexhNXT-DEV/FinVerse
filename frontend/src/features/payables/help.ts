import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Payables & Cash screens (listed in the help centre). */
export const PAYABLES_HELP: HelpSection = {
  id: 'payables',
  module: 'Payables & Cash',
  intro:
    'Supplier invoices, payment vouchers for every payable (suppliers, commissions, claims, reinsurance, premium refunds), post-dated cheques issued, petty cash and the company bank accounts.',
  screens: [
    {
      name: 'Supplier Invoices',
      path: '/payables/invoices',
      summary:
        'Accounts payable: capture, approve and post supplier invoices (SI-…) with input VAT and expanded withholding tax.',
      workflow: [
        'Enter the invoice lines net of VAT with expense account and cost centre; input VAT (12 %) and the supplier’s EWT are computed per line.',
        'Save as draft, then submit. A checker approves and posts it, or rejects it back to the maker.',
        'Approval posts SUPPLIER_INVOICE (Dr expense and input VAT / Cr supplier payable and EWT payable) and records one credit open item for the supplier.',
      ],
      controls: [
        'The checker must differ from the creator and the submitter, and the invoice (base currency) must be within the checker’s authorization limit.',
        'Draft and pending invoices can be cancelled; approved invoices are settled by a payment voucher.',
      ],
    },
    {
      name: 'Payment Vouchers',
      path: '/payables/vouchers',
      summary:
        'Pay open payables of suppliers, intermediaries, claimants, reinsurers and policyholders by cheque, bank transfer or post-dated cheque (PV-…).',
      workflow: [
        'New payment: choose the payee and bank account, select the open items to settle and the payment mode.',
        'Submit; the checker approves: the cheque leaf is allocated, the payment event of the category is posted (Dr party payable / Cr bank) and the paid items are matched.',
        'Confirm when a cheque is presented, or void an unpresented cheque: the payment is reversed and the paid items re-open with their own numbers and due dates.',
      ],
      controls: [
        'Same maker-checker and authorization limit rules as supplier invoices; the approval inbox shows a checker only what they may approve.',
        'The accounting event follows the payee and documents: SUPPLIER_PAYMENT, COMMISSION_PAYMENT, CLAIM_PAYMENT, RI_SETTLEMENT_PAYMENT or PREMIUM_REFUND_PAYMENT.',
      ],
    },
    {
      name: 'PDC Issued',
      path: '/payables/pdc-issued',
      summary:
        'Post-dated cheques issued. On approval the liability moves from the payee to the PDC issued clearing account and stays there until the cheque is presented.',
      workflow: [
        'Refresh due: cheques whose date has been reached become Due (also done by a nightly task).',
        'Present: posts Dr PDC clearing / Cr bank on the presentation date; clear it when it appears on the bank statement.',
        'Cancel a stopped cheque (the payment is reversed and the voucher voided) or replace it with a new cheque number or date (no posting).',
      ],
      controls: ['Every status change is kept in the cheque’s status history.'],
    },
    {
      name: 'Petty Cash',
      path: '/payables/petty-cash',
      summary:
        'Imprest funds per branch. Cash in the box plus vouchers pending reimbursement always equals the imprest amount.',
      workflow: [
        'Create the fund (custodian, imprest, bank); another user authorizes it, then it is established: the imprest is drawn from the bank.',
        'Record petty cash vouchers; each is approved by a checker and posts PETTY_CASH_EXPENSE (Dr expense / Cr petty cash).',
        'Claim reimbursement of the approved vouchers; its approval posts PETTY_CASH_REPLENISHMENT (Dr petty cash / Cr bank) and refills the box.',
      ],
      controls: [
        'A voucher that would overdraw the box is refused; a replenishment never takes the fund above its imprest.',
        'Maker-checker on funds (MASTER_AUTHORIZE), vouchers and claims (RECEIPT_PAYMENT_AUTHORIZE).',
      ],
    },
    {
      name: 'Bank Accounts',
      path: '/payables/bank-accounts',
      summary:
        'House bank accounts mapped to bank GL accounts, with their cheque books and PDC clearing accounts; generate the payment notification file for the bank.',
      workflow: [
        'Register a bank account; another user authorizes it before it can be used on payments.',
        'Add cheque books by leaf range; approval of a cheque payment takes the next leaf.',
        'Payment file (FIN-BRS-PAYNOTIFY): approved transfers of a date range in the bank’s layout, optionally with cheques and PDCs for positive pay.',
      ],
      controls: ['Maker-checker: MASTER_MAINTAIN creates, MASTER_AUTHORIZE authorizes.'],
    },
  ],
};

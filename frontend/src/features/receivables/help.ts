import type { HelpSection } from '@/features/help/helpContent';

/** In-app help of the Receivables & Banking screens (listed in the help centre). */
export const RECEIVABLES_HELP: HelpSection = {
  id: 'receivables',
  module: 'Receivables & Banking',
  intro:
    'Official receipts and their application to debit notes, cheque deposits, post-dated cheques received, bank statements and the bank reconciliation.',
  screens: [
    {
      name: 'Receipts',
      path: '/receivables/receipts',
      summary:
        'Official receipts (OR-…) from policyholders, intermediaries, reinsurers and other payers, with their status and the debit notes they settled. Open a receipt to approve, apply, cancel or record a bounced cheque.',
      workflow: [
        'Money left on account after approval can be applied later to the payer’s open debit notes.',
        'Cancelling a receipt or recording a bounced cheque reverses its journal and re-opens the debit notes it settled.',
      ],
      controls: [
        'Cancellation and bounce need RECEIPT_PAYMENT_AUTHORIZE, a date and a reason; both are audited.',
        'The exchange rate is the SPOT rate of the receipt date, as used by the accounting engine.',
      ],
    },
    {
      name: 'New Receipt',
      path: '/receivables/receipts/new',
      summary:
        'Record money received and apply it to the payer’s debit notes; the rest stays on account. A post-dated cheque entered here is registered in the PDC register instead.',
      workflow: [
        'Choose the payer type and party, the mode (cash, cheque, bank transfer, card or PDC), the bank GL account, amount and currency.',
        'Allocation: Manual (pick the debit notes and amounts), FIFO (oldest due first, computed at approval) or None (all on account).',
        'Save: the receipt waits for approval in Receipt Approvals and My Approvals.',
      ],
      controls: [
        'Manual allocations are validated at entry and cannot exceed the open amount of a debit note or the receipt.',
        'The module never picks GL accounts: approval posts PREMIUM_RECEIPT, PREMIUM_DEPOSIT, RI_SETTLEMENT_RECEIPT or MISC_RECEIPT through the accounting rules.',
      ],
    },
    {
      name: 'Receipt Approvals',
      path: '/receivables/approvals',
      summary:
        'Receipts waiting for a checker, pending first. Approval posts the journal, records the credit open item and matches it against the allocated debit notes.',
      controls: [
        'The user who entered a receipt cannot approve it; no authorization limit applies (the money is already received).',
        'A rejected receipt keeps its number and is not posted.',
      ],
    },
    {
      name: 'Cheques & Deposits',
      path: '/receivables/deposits',
      summary:
        'Cash and cheques received but not yet deposited, and the deposit slips (DS-…) that group them per bank account and currency.',
      workflow: [
        'Select undeposited receipts of one bank account and prepare a deposit slip.',
        'Confirm the slip with the deposit date once the bank has stamped it, or cancel it to release the receipts.',
      ],
      controls: [
        'Until the bank credits it, an undeposited amount is a deposit in transit in the bank reconciliation.',
      ],
    },
    {
      name: 'PDC Received',
      path: '/receivables/pdcs',
      summary:
        'Post-dated cheques on hand. They are memorandum items, not posted to the ledger, until they are banked.',
      workflow: [
        'Mark due: cheques whose date has been reached move from On hand to Due.',
        'Bank a cheque: an official receipt (mode PDC) is raised for approval and applied to the linked debit note, else FIFO.',
        'Clear it once the receipt is approved, or record a bounce (the receipt is reversed) or a return.',
      ],
      controls: [
        'Every status change is kept in the confirmation audit trail of the cheque; reports as of a date use this history.',
      ],
    },
    {
      name: 'Bank Statements',
      path: '/receivables/bank-statements',
      summary:
        'Import the bank’s statement (CSV) for a GL bank account before reconciling it, and review its lines.',
      controls: [
        'The file layout is described in BANK_STATEMENT_FORMAT; the statement closing balance is used to finalize the reconciliation.',
      ],
    },
    {
      name: 'Bank Reconciliation',
      path: '/receivables/bank-reconciliation',
      summary:
        'Match the book entries of a bank account with the statement lines and prepare the Bank Reconciliation Statement (BRS) as of a date.',
      workflow: [
        'Auto-match: equal amounts within a date window (7 days by default), preferring matching references or cheque numbers, then deposit slips and receipts named in the bank reference.',
        'Match the remaining items manually (any balanced selection) or undo a wrong match.',
        'Save the BRS; finalize it when the difference to the statement balance is zero.',
      ],
      controls: [
        'Matches covered by a finalized reconciliation cannot be undone, so a finalized BRS never changes.',
        'Reconciliation needs RECONCILIATION_MANAGE; every match, unmatch and finalization is audited.',
        'The period-end and year-end checklists count the unreconciled book entries and statement lines of every bank account up to the period end and show them as a warning.',
      ],
    },
    {
      name: 'Receivables Reports',
      path: '/receivables/reports',
      summary:
        'Debtors ageing (detail, summary, by division), statement of outstanding, cheque registers, matched / unmatched statement of account, bank reconciliation and PDC reports, with PDF, Excel and CSV export.',
      controls: [
        'Ageing slots can be entered per run (up to five); left blank, the AGEING_BUCKETS system parameter applies.',
      ],
    },
  ],
};

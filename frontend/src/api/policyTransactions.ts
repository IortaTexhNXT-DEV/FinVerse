import { api } from './client';

/** Kind of a policy transaction (history of a policy, ADJID.022/024). */
export type PolicyTransactionKind =
  'BOOKING' | 'ENDORSEMENT' | 'CANCELLATION' | 'ADJUSTMENT' | 'REFUND';

/** Premium, taxes and charges, gross (premium plus taxes) and commission. */
export interface PolicyAmounts {
  premium: number;
  taxes: number;
  gross: number;
  commission: number;
}

/** A line of a GL journal. */
export interface JournalEntryLine {
  lineNo: number;
  accountCode: string;
  accountName: string;
  debit?: number;
  credit?: number;
  partyCode?: string;
  narration?: string;
}

/** A GL journal of a policy transaction. */
export interface TransactionJournal {
  id: number;
  batchNo: string;
  valueDate: string;
  status: string;
  narration?: string;
  totalDebit: number;
  totalCredit: number;
  lines: JournalEntryLine[];
}

/** One transaction of a policy: booking, endorsement, cancellation, adjustment or refund. */
export interface PolicyTransaction {
  seq: number;
  kind: PolicyTransactionKind;
  date?: string;
  effectiveDate?: string;
  typeLabel: string;
  detail?: string;
  refs: { invoiceNo?: string; endorsementNo?: string; requestNo?: string; requestId?: number };
  change: PolicyAmounts;
  after?: PolicyAmounts;
  balanceChange?: number;
  status: string;
  statusLabel: string;
  posted: boolean;
  journals: TransactionJournal[];
}

/** The transaction history of a policy, the original booking first. */
export interface PolicyTransactions {
  rootInvoiceNo?: string;
  arn: string;
  policyNo?: string;
  currency?: string;
  rows: PolicyTransaction[];
}

const enc = encodeURIComponent;

/** Policy transaction history (Operations ledger, booking and the accounting engine journals). */
export const policyTransactionsApi = {
  /** Transaction history of the policy of an invoice, with the GL journals. */
  forInvoice: (invoiceNo: string) =>
    api.get<PolicyTransactions>(`/ops/invoices/${enc(invoiceNo)}/transactions`),
  /** Transaction history of an account (every policy year). */
  forAccount: (arn: string) =>
    api.get<PolicyTransactions>(`/ops/accounts/${enc(arn)}/transactions`),
};

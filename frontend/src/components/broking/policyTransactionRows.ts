import type { PolicyTransaction } from '@/api/policyTransactions';

/** Key of a transaction row (its position in the history). */
export function transactionRowKey(row: PolicyTransaction): number {
  return row.seq;
}

/** "1 Journal", "2 Journals": the label of the expand button of a transaction. */
export function journalCountLabel(count: number): string {
  return `${String(count)} ${count === 1 ? 'Journal' : 'Journals'}`;
}

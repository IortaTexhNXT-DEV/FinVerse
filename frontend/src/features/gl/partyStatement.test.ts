import type { OpenItem } from '@/api/subledger';
import { daysOverdue, signedOutstanding, statementLines, summarize } from './partyStatement';

function item(patch: Partial<OpenItem>): OpenItem {
  return {
    id: 1,
    partyCode: 'C-1',
    direction: 'DEBIT',
    documentType: 'DEBIT_NOTE',
    documentNo: 'DN-1',
    documentDate: '2026-06-01',
    dueDate: '2026-07-01',
    currency: 'PHP',
    amount: 1000,
    settledAmount: 0,
    outstanding: 1000,
    status: 'OPEN',
    sourceModule: 'UNDERWRITING',
    ...patch,
  };
}

describe('party statement', () => {
  const items = [
    item({ id: 1, documentNo: 'DN-2', documentDate: '2026-06-05', outstanding: 800.1 }),
    item({ id: 2, documentNo: 'DN-1', outstanding: 0, status: 'SETTLED' }),
    item({
      id: 3,
      direction: 'CREDIT',
      documentNo: 'OR-1',
      documentDate: '2026-06-10',
      dueDate: '2026-12-31',
      outstanding: 300,
    }),
  ];

  it('computes days overdue from the due date', () => {
    expect(daysOverdue(item({}), '2026-07-31')).toBe(30);
    expect(daysOverdue(item({}), '2026-06-15')).toBe(0);
  });

  it('signs outstanding amounts by direction', () => {
    expect(signedOutstanding(items[0]!)).toBe(800.1);
    expect(signedOutstanding(items[2]!)).toBe(-300);
  });

  it('summarises receivables, payables, net and overdue', () => {
    expect(summarize(items, '2026-09-01')).toEqual({
      receivable: 800.1,
      payable: 300,
      net: 500.1,
      overdue: 800.1,
      openItems: 2,
    });
  });

  it('orders statement lines and filters settled items', () => {
    expect(statementLines(items, false).map((i) => i.documentNo)).toEqual(['DN-1', 'DN-2', 'OR-1']);
    expect(statementLines(items, true).map((i) => i.id)).toEqual([1, 3]);
  });
});

import { emptyLine, totals } from './journalMath';

describe('journal totals', () => {
  it('detects a balanced journal', () => {
    const t = totals([
      { accountCode: '5603', side: 'DEBIT', amount: 100.1 },
      { accountCode: '5604', side: 'DEBIT', amount: 0.2 },
      { accountCode: '1111', side: 'CREDIT', amount: 100.3 },
    ]);
    expect(t).toEqual({ debit: 100.3, credit: 100.3, difference: 0, balanced: true });
  });

  it('reports the difference of an unbalanced journal', () => {
    const t = totals([
      { accountCode: 'A', side: 'DEBIT', amount: 50 },
      { accountCode: 'B', side: 'CREDIT', amount: 20 },
    ]);
    expect(t.difference).toBe(30);
    expect(t.balanced).toBe(false);
  });

  it('treats an empty journal as unbalanced', () => {
    expect(totals([emptyLine('DEBIT'), emptyLine('CREDIT')]).balanced).toBe(false);
  });
});

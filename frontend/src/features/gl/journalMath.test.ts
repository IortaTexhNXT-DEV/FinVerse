import { balanceStatus, emptyLine, isBlankLine, lineProblems, totals } from './journalMath';

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

  it('reports incomplete and non-positive lines instead of dropping them', () => {
    const lines = [
      { accountCode: '2502', side: 'DEBIT' as const, amount: -500 },
      { accountCode: '5613', side: 'CREDIT' as const, amount: 0 },
      { accountCode: '', side: 'DEBIT' as const, amount: 25 },
      emptyLine('CREDIT'),
      { accountCode: '1111', side: 'CREDIT' as const, amount: 10 },
    ];
    expect(lineProblems(lines)).toEqual({
      0: { field: 'amount', message: 'Amount must be greater than zero' },
      1: { field: 'amount', message: 'Amount must be greater than zero' },
      2: { field: 'account', message: 'Choose an account' },
    });
    expect(isBlankLine(lines[3]!)).toBe(true);
    expect(isBlankLine({ accountCode: '', side: 'DEBIT', amount: Number.NaN })).toBe(true);
    expect(isBlankLine(lines[1]!)).toBe(false);
  });

  it('shows a neutral badge until amounts are entered', () => {
    expect(balanceStatus(totals([emptyLine('DEBIT'), emptyLine('CREDIT')]))).toEqual({
      tone: 'neutral',
      label: 'Enter amounts',
    });
    expect(
      balanceStatus(
        totals([
          { accountCode: 'A', side: 'DEBIT', amount: -500 },
          { accountCode: 'B', side: 'CREDIT', amount: -500 },
        ]),
      ),
    ).toEqual({ tone: 'danger', label: 'Amounts must be greater than zero' });
    expect(
      balanceStatus(
        totals([
          { accountCode: 'A', side: 'DEBIT', amount: 50 },
          { accountCode: 'B', side: 'CREDIT', amount: 20 },
        ]),
      ),
    ).toEqual({ tone: 'danger', label: 'Difference 30.00' });
    expect(
      balanceStatus(
        totals([
          { accountCode: 'A', side: 'DEBIT', amount: 5 },
          { accountCode: 'B', side: 'CREDIT', amount: 5 },
        ]),
      ).tone,
    ).toBe('success');
  });
});

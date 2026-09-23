import { newJournalValues, toJournalInput } from './journalForm';

describe('journal request body', () => {
  it('leaves out only blank placeholder rows, never lines the user filled in', () => {
    const values = newJournalValues(3, 'PHP');
    values.lines = [
      { accountCode: '5613', side: 'DEBIT', amount: -500 },
      { accountCode: '2502', side: 'CREDIT', amount: 0 },
      { accountCode: '', side: 'DEBIT', amount: 0 },
    ];
    const input = toJournalInput(1, values);
    expect(input.lines.map((l) => [l.accountCode, l.amount])).toEqual([
      ['5613', -500],
      ['2502', 0],
    ]);
    expect(input.companyId).toBe(1);
    expect(input.reference).toBeUndefined();
  });
});

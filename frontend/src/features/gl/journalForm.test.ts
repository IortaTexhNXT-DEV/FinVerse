import {
  assigneeOptions,
  journalStatusOption,
  newJournalValues,
  notPostedText,
  toJournalInput,
} from './journalForm';

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

describe('journal texts', () => {
  it('offers the statuses as their pills read them', () => {
    expect(journalStatusOption('')).toBe('All');
    expect(journalStatusOption('PENDING_APPROVAL')).not.toMatch(/_/);
    expect(journalStatusOption('PENDING_APPROVAL')).toMatch(/^[A-Z][a-z]/);
  });

  it('names a journal that was not posted by its batch number', () => {
    expect(
      notPostedText({ id: 7, batchNo: 'JV-2026-000007', posted: false, message: 'Period closed' }),
    ).toBe('JV-2026-000007: Period closed');
  });

  it('offers the assignees by name, sorted', () => {
    const options = assigneeOptions(['gltl', 'glofficer']);
    expect(options).toHaveLength(2);
    const labels = options.map((o) => o.label);
    expect([...labels].sort((a, b) => a.localeCompare(b))).toEqual(labels);
  });
});

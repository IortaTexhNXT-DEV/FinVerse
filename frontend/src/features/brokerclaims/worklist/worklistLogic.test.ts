import { mayComplete, validateDiary } from '../diary/api';
import type { DiaryEntry } from '../diary/api';
import { bucketShares } from '../home/homeLogic';
import { filterText, queryFromSearch } from './worklistLogic';

describe('worklist, diary and home logic', () => {
  it('reads the worklist query from the URL', () => {
    expect(queryFromSearch(new URLSearchParams('tab=ALL&flag=UNPAID_PREMIUM'))).toEqual({
      tab: 'ALL',
      flag: 'UNPAID_PREMIUM',
      status: undefined,
    });
    expect(
      queryFromSearch(new URLSearchParams('tab=NOPE&flag=NOPE&status=INSURER_REVIEW')),
    ).toEqual({
      tab: 'MINE',
      flag: undefined,
      status: 'INSURER_REVIEW',
    });
    expect(filterText({ tab: 'ALL', flag: 'OVERDUE', status: 'X' })).toBe(
      'Follow-up overdue · Status X',
    );
    expect(filterText({ tab: 'MINE' })).toBeUndefined();
  });

  it('validates a diary entry and who may complete it', () => {
    expect(
      validateDiary({
        entryType: '',
        entryDate: '2026-09-26',
        dueDate: '2026-09-25',
        assignee: '',
        text: ' ',
      }),
    ).toEqual({
      entryType: 'Select the type of entry',
      text: 'Enter the diary text',
      dueDate: 'The due date must be on or after the entry date',
    });
    expect(
      validateDiary({
        entryType: 'NOTE',
        entryDate: '2026-09-26',
        dueDate: '',
        assignee: '',
        text: 'x'.repeat(2001),
      }),
    ).toEqual({ text: 'The diary text can have up to 2000 characters' });
    const entry = { assignee: 'clmTL', createdBy: 'clmofficer' } as DiaryEntry;
    expect(mayComplete(entry, 'clmtl')).toBe(true);
    expect(mayComplete(entry, 'clmofficer')).toBe(true);
    expect(mayComplete(entry, 'clmth')).toBe(false);
    expect(mayComplete({ ...entry, doneAt: '2026-09-26T00:00:00Z' }, 'clmtl')).toBe(false);
    expect(mayComplete(entry, undefined)).toBe(false);
  });

  it('sizes the ageing bars against the fullest bucket', () => {
    expect(
      bucketShares([
        { bucket: '0-30', claims: 4 },
        { bucket: '31-60', claims: 2 },
      ]).map((b) => b.share),
    ).toEqual([1, 0.5]);
    expect(bucketShares([{ bucket: '0-30', claims: 0 }])[0]?.share).toBe(0);
  });
});

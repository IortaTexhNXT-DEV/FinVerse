import { effectOf } from './rowEffect';

describe('waive and exclude dialog wording', () => {
  it('speaks of one row in the singular', () => {
    expect(effectOf('waive', 1)).toBe('1 row loads despite its errors and counts as waived.');
    expect(effectOf('exclude', 1)).toBe('1 row is left out of the load; state how it is handled.');
  });

  it('speaks of several rows in the plural', () => {
    expect(effectOf('waive', 3)).toBe('3 rows load despite their errors and count as waived.');
    expect(effectOf('exclude', 2)).toBe(
      '2 rows are left out of the load; state how they are handled.',
    );
  });
});

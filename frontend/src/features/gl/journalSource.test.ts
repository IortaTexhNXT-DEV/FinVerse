import { journalSource } from './journalSource';

describe('journal source', () => {
  it('identifies uploaded vouchers with their upload reference', () => {
    expect(
      journalSource({ sourceModule: 'JOURNAL_UPLOAD', sourceReference: 'UPL-2026-000003' }),
    ).toEqual({ label: 'Journal upload', reference: 'UPL-2026-000003' });
  });

  it('shows keyed vouchers as manual entry and other modules by name', () => {
    expect(journalSource({})).toEqual({ label: 'Manual entry' });
    expect(
      journalSource({ sourceModule: 'RECURRING', sourceReference: 'REC:4:2026-09-15' }),
    ).toEqual({
      label: 'Recurring journal',
      reference: 'REC:4:2026-09-15',
    });
    expect(journalSource({ sourceModule: 'UNDERWRITING' }).label).toBe('Underwriting');
  });
});

import { describe, expect, it, vi } from 'vitest';
import { retentionActions } from './retentionActions';

describe('retentionActions', () => {
  it('offers Records to every reader and Edit to the maintainer', () => {
    const records = vi.fn();
    const edit = vi.fn();
    const all = retentionActions(true, records, edit);
    expect(all.filter((a) => !a.hidden).map((a) => a.label)).toEqual(['Records', 'Edit']);
    all.forEach((a) => void a.onSelect(''));
    expect(records).toHaveBeenCalledOnce();
    expect(edit).toHaveBeenCalledOnce();
    const read = retentionActions(false, records, edit).filter((a) => !a.hidden);
    expect(read.map((a) => a.label)).toEqual(['Records']);
  });
});

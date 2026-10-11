import { describe, expect, it, vi } from 'vitest';
import { queuedActions } from './bookingRowActions';

describe('queuedActions', () => {
  it('offers Edit and Remove, Remove as the reversing action', () => {
    const edit = vi.fn();
    const remove = vi.fn();
    const actions = queuedActions(edit, remove);
    expect(actions.map((a) => [a.label, Boolean(a.danger)])).toEqual([
      ['Edit', false],
      ['Remove', true],
    ]);
    actions.forEach((a) => void a.onSelect(''));
    expect(edit).toHaveBeenCalledOnce();
    expect(remove).toHaveBeenCalledOnce();
  });
});

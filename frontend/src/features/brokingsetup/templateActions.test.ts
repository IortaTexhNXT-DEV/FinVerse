import { describe, expect, it, vi } from 'vitest';
import { templateVersionActions } from './templateActions';

describe('templateVersionActions', () => {
  it('offers the Word download of a version in the row menu', () => {
    const word = vi.fn();
    const [action] = templateVersionActions(word);
    expect(action?.label).toBe('Download Word');
    void action?.onSelect('');
    expect(word).toHaveBeenCalledOnce();
  });
});

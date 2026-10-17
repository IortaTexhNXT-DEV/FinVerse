import { describe, expect, it, vi } from 'vitest';
import { instructionActions } from './instructionActions';

describe('instructionActions', () => {
  it('offers Change and End of an instruction in force, End as the reversing action', () => {
    const change = vi.fn();
    const end = vi.fn();
    const actions = instructionActions(true, change, end);
    expect(actions.filter((a) => !a.hidden).map((a) => a.label)).toEqual(['Change', 'End']);
    expect(actions.find((a) => a.label === 'End')?.danger).toBe(true);
    actions.forEach((a) => void a.onSelect(''));
    expect(change).toHaveBeenCalledOnce();
    expect(end).toHaveBeenCalledOnce();
  });

  it('offers nothing for an ended instruction or a user who may not maintain the client', () => {
    expect(instructionActions(false, vi.fn(), vi.fn()).filter((a) => !a.hidden)).toEqual([]);
  });
});

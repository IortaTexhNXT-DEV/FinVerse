import { describe, expect, it, vi } from 'vitest';
import type { RecordStatus } from '@/api/types';
import { accountRowActions, accountWord, periodRowActions } from './rowActions';

const labels = (actions: { label: string }[]) => actions.map((a) => a.label);
const on = { authorize: vi.fn(), freeze: vi.fn(), unfreeze: vi.fn() };
const account = {
  code: '1000',
  name: 'Cash in Bank',
  frozen: false,
  recordStatus: 'ACTIVE' as RecordStatus,
};

describe('chart of accounts row menu', () => {
  it('ends with Freeze in red, asking for a reason', () => {
    const actions = accountRowActions(account, { authorize: true, awaitsMe: false }, on);
    expect(labels(actions)).toEqual(['Freeze']);
    expect(actions[0]?.danger).toBe(true);
    expect(actions[0]?.confirm?.reason).toBe('required');
    actions[0]?.onSelect('Wrong postings');
    expect(on.freeze).toHaveBeenCalledWith('Wrong postings');
  });

  it('offers Authorize to the second checker and Unfreeze on a frozen account', () => {
    expect(
      labels(
        accountRowActions(
          { ...account, recordStatus: 'PENDING_AUTHORIZATION' },
          { authorize: true, awaitsMe: true },
          on,
        ),
      ),
    ).toEqual(['Authorize']);
    expect(
      labels(
        accountRowActions({ ...account, frozen: true }, { authorize: true, awaitsMe: false }, on),
      ),
    ).toEqual(['Unfreeze']);
    expect(accountRowActions(account, { authorize: false, awaitsMe: true }, on)).toEqual([]);
  });

  it('names the class and the sub-ledger in words', () => {
    expect(accountWord('ASSET')).toBe('Asset');
    expect(accountWord(undefined)).toBe('');
  });
});

describe('financial period row menu', () => {
  it('offers the status changes of the period, Close last in red', () => {
    const ask = vi.fn();
    const actions = periodRowActions({ status: 'OPEN' }, true, ask);
    expect(labels(actions)).toEqual(['Start closing', 'Close']);
    expect(actions[1]?.danger).toBe(true);
    actions[1]?.onSelect('');
    expect(ask).toHaveBeenCalledWith('close');
    expect(periodRowActions({ status: 'OPEN' }, false, ask)).toEqual([]);
  });
});

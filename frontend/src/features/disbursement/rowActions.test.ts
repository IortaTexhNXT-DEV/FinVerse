import { describe, expect, it, vi } from 'vitest';
import {
  bankActions,
  payeeAccountActions,
  payeeRequestActions,
  payeeRequestSource,
  seriesActions,
} from './rowActions';

const labels = (actions: { label: string }[]) => actions.map((a) => a.label);

describe('payee request row menu', () => {
  it('offers Create Payee, then Close last in red with a confirmation', () => {
    const create = vi.fn();
    const actions = payeeRequestActions({ status: 'OPEN' }, true, { create, close: vi.fn() });
    expect(labels(actions)).toEqual(['Create Payee', 'Close']);
    expect(actions[1]?.danger).toBe(true);
    expect(actions[1]?.confirm?.title).toBe('Close Payee Request');
    actions[0]?.onSelect('');
    expect(create).toHaveBeenCalled();
  });

  it('offers nothing to a user who may not maintain payees or on a closed request', () => {
    expect(
      payeeRequestActions({ status: 'OPEN' }, false, { create: vi.fn(), close: vi.fn() }),
    ).toEqual([]);
    expect(
      payeeRequestActions({ status: 'DONE' }, true, { create: vi.fn(), close: vi.fn() }),
    ).toEqual([]);
  });

  it('names the source of the request in words', () => {
    expect(payeeRequestSource('RRF')).toBe('Refund request');
    expect(payeeRequestSource('DISBURSEMENT')).toBe('Disbursement');
    expect(payeeRequestSource('NO_MATCH')).toBe('No match');
  });
});

describe('payee bank account row menu', () => {
  it('deactivates an active account with a confirmation', () => {
    const actions = payeeAccountActions({ active: true }, true, { deactivate: vi.fn() });
    expect(labels(actions)).toEqual(['Deactivate']);
    expect(actions[0]?.confirm?.destructive).toBe(true);
  });

  it('offers nothing on an inactive account or without the maintain right', () => {
    expect(payeeAccountActions({ active: false }, true, { deactivate: vi.fn() })).toEqual([]);
    expect(payeeAccountActions({ active: true }, false, { deactivate: vi.fn() })).toEqual([]);
  });
});

describe('BDOIR bank account row menu', () => {
  const on = { addSeries: vi.fn(), changeStatus: vi.fn(), authorize: vi.fn() };

  it('gives the team leader Add Series and Deactivate, last in red', () => {
    const actions = bankActions(
      { code: 'BDO-CA', status: 'ACTIVE', recordStatus: 'ACTIVE' },
      { review: true, approve: false },
      on,
    );
    expect(labels(actions)).toEqual(['Add Series', 'Deactivate']);
    expect(actions[1]?.danger).toBe(true);
    expect(actions[1]?.confirm?.title).toBe('Deactivate Bank BDO-CA');
  });

  it('offers Activate on an inactive account and nothing while a change waits', () => {
    expect(
      labels(
        bankActions(
          { code: 'X', status: 'INACTIVE', recordStatus: 'ACTIVE' },
          { review: true, approve: false },
          on,
        ),
      ),
    ).toEqual(['Add Series', 'Activate']);
    expect(
      labels(
        bankActions(
          {
            code: 'X',
            status: 'ACTIVE',
            requestedStatus: 'INACTIVE',
            recordStatus: 'PENDING_AUTHORIZATION',
          },
          { review: true, approve: false },
          on,
        ),
      ),
    ).toEqual(['Add Series']);
  });

  it('gives the approver Authorise while a change waits', () => {
    const actions = bankActions(
      {
        code: 'X',
        status: 'ACTIVE',
        requestedStatus: 'INACTIVE',
        recordStatus: 'PENDING_AUTHORIZATION',
      },
      { review: false, approve: true },
      on,
    );
    expect(labels(actions)).toEqual(['Authorise']);
  });
});

describe('check series row menu', () => {
  it('edits a series only before its first check is printed', () => {
    expect(
      labels(
        seriesActions({ status: 'ACTIVE', firstNo: 100, nextNo: 100 }, true, { edit: vi.fn() }),
      ),
    ).toEqual(['Edit']);
    expect(
      seriesActions({ status: 'ACTIVE', firstNo: 100, nextNo: 101 }, true, { edit: vi.fn() }),
    ).toEqual([]);
    expect(
      seriesActions({ status: 'ACTIVE', firstNo: 100, nextNo: 100 }, false, { edit: vi.fn() }),
    ).toEqual([]);
  });
});
